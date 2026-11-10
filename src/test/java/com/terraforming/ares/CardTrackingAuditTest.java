package com.terraforming.ares;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Expansion;
import com.terraforming.ares.model.GameParameters;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.PlayerDifficulty;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.GameService;
import com.terraforming.ares.services.SimulationProcessorService;
import com.terraforming.ares.services.agent.PickPhaseDecisions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Which cards does the observation tell apart? Two questions, decided by the BEHAVIOUR of the card class (not by CardMetadata, which was written for the frontend):
 * <ul>
 *   <li>a card ON THE TABLE with a lasting effect (an action, a reaction to what happens later, a special rule, resources on it) must be recognisable
 *       as exactly that card - a card that only changed tags and incomes needs no tracking, those are already counted;</li>
 *   <li>a card IN THE HAND must be recognisable: its identity is what the player decides about.</li>
 * </ul>
 * "Recognisable" = the observation differs from the one with ANOTHER card of the same colour and the same tags in the same place.
 * The report is written to target/card_tracking_audit.txt.
 */
class CardTrackingAuditTest extends EngineTestBase {
    /**
     * The only lasting-effect cards the observation may leave unrecognised are the "pays again" production cards: the owner's rule is that a card
     * that only changed incomes and tags needs no tracking (their income is in the income features). Which of them are unrecognised varies from
     * run to run (the games are random); any OTHER kind of card (action, reaction, resource holder, special rule) must be recognisable.
     * Every card in the HAND must be recognisable.
     */
    static final String ONLY_ALLOWED_REASON = "[pays again]";

    @Autowired
    GameService gameService;
    @Autowired
    SimulationProcessorService simulation;
    @Autowired
    PickPhaseDecisions decisions;
    @Autowired
    CardService cardService;

    private static final List<String> REACTIONS = List.of("onOceanFlippedEffect", "onTemperatureChangedEffect", "onInfrastructureChangedEffect",
            "onOxygenChangedEffect", "onForestBuiltEffect", "onMilestoneGained", "collectExtraIncome", "postProjectBuiltEffectForAll");

    /**
     * Why a card has a lasting effect (empty = it only changes tags / incomes / points once).
     */
    private List<String> lastingEffects(Card card) {
        List<String> why = new ArrayList<>();
        if (card.isActiveCard()) why.add("action");
        if (card.canPayAgain()) why.add("pays again");
        if (card.getSpecialEffects() != null && !card.getSpecialEffects().isEmpty()) why.add("special effect " + card.getSpecialEffects());
        if (card.onBuiltEffectApplicableToOther()) why.add("reacts to other cards built by me");
        if (card.onBuiltEffectApplicableToAllPlayers()) why.add("reacts to cards built by anybody");
        if (card.getCollectableResource() != null && !card.getCollectableResource().name().equals("NONE")) why.add("keeps " + card.getCollectableResource());
        for (String name : REACTIONS) {
            for (Method m : card.getClass().getMethods()) {
                if (m.getName().equals(name) && !m.getDeclaringClass().isInterface() && m.getDeclaringClass() != Object.class) {
                    why.add(name);
                    break;
                }
            }
        }
        return why;
    }

    private MarsGame midGame(int round) {
        MarsGame game = gameService.createNewSimulation(GameParameters.builder()
                .playerNames(new ArrayList<>(List.of("A", "B")))
                .computers(List.of(PlayerDifficulty.SMART, PlayerDifficulty.SMART))
                .mulligan(true)
                .expansion(Expansion.BASE).expansion(Expansion.DISCOVERY).expansion(Expansion.BUFFED_CORPORATION)
                .dummyHand(true).build());
        MarsGame[] snapshot = new MarsGame[1];
        simulation.processSimulation(game, current -> {
            if (snapshot[0] == null && current.getTurns() >= round) {
                snapshot[0] = new MarsGame(current);
            }
        });
        return snapshot[0];
    }

    private static String key(Card card) {
        List<String> tags = card.getTags() == null ? List.of() : card.getTags().stream().map(Tag::name).sorted().collect(Collectors.toList());
        return card.getColor() + "|" + tags;
    }

    /**
     * The observation of a state that is the same in every respect except for `cardId` being in one place. Both cards of the comparison
     * are removed from every zone first, so that the two states differ in nothing but the card under test.
     */
    private float[] observe(MarsGame base, boolean table, int cardId, List<Integer> group, boolean ownerObserves) {
        MarsGame game = new MarsGame(base);
        List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        players.sort(Comparator.comparing(Player::getName));
        for (int id : group) {  // the whole group leaves every zone, so that the compared states differ in nothing but the card under test
            for (Player p : players) {
                p.getHand().getCards().remove((Integer) id);
                p.getPlayed().getCards().remove((Integer) id);
                p.getActivatedBlueCards().getCards().remove((Integer) id);
            }
            game.getProjectsDeck().getCards().remove((Integer) id);
        }
        Player owner = players.get(0);
        Player observer = ownerObserves ? owner : players.get(1);
        (table ? owner.getPlayed() : owner.getHand()).addCard(cardId);
        return decisions.build(game, observer).observation();
    }

    /**
     * Cards of a group (same colour and tags) with the observation they produce. Cards that produce the very same observation as several others
     * are NOT recognised (only their tags/colour are seen); the cards with an observation of their own are. The result does not depend on which
     * "twin" is picked. A group of two different observations is ambiguous and counted as recognised (the benefit of the doubt).
     */
    private List<Integer> unrecognised(MarsGame base, List<Integer> group, boolean table, boolean ownerObserves) {
        Map<String, List<Integer>> classes = new LinkedHashMap<>();
        for (int id : group) {
            classes.computeIfAbsent(Arrays.toString(observe(base, table, id, group, ownerObserves)), k -> new ArrayList<>()).add(id);
        }
        List<Integer> biggest = classes.values().stream().max(Comparator.comparingInt(List::size)).orElse(List.of());
        return biggest.size() >= 2 ? biggest : List.of();
    }

    @Test
    void auditCards() throws IOException {
        List<Integer> ids = new ArrayList<>(cardService.createProjectsDeck(List.of(Expansion.BASE, Expansion.DISCOVERY, Expansion.BUFFED_CORPORATION, Expansion.INFRASTRUCTURE)).getCards());
        ids.sort(Integer::compare);
        Map<String, List<Integer>> byKey = new TreeMap<>();
        for (int id : ids) {
            byKey.computeIfAbsent(key(cardService.getCard(id)), k -> new ArrayList<>()).add(id);
        }

        // a card is reported only if it is unrecognised in EVERY snapshot (a feature that is saturated in one state hides a card that another state shows)
        java.util.Set<Integer> notOnTable = null;
        java.util.Set<Integer> notInHand = null;
        java.util.Set<Integer> leaks = new java.util.TreeSet<>();
        List<String> unverifiable = new ArrayList<>();
        for (int round : new int[]{6, 14, 22}) {
            MarsGame snapshot = midGame(round);
            java.util.Set<Integer> table = new java.util.TreeSet<>();
            java.util.Set<Integer> hand = new java.util.TreeSet<>();
            unverifiable.clear();
            for (Map.Entry<String, List<Integer>> group : byKey.entrySet()) {
                List<Integer> members = group.getValue();
                if (members.size() < 2) {
                    unverifiable.add(cardService.getCard(members.get(0)).getClass().getSimpleName() + "#" + members.get(0) + " " + group.getKey());
                    continue;
                }
                table.addAll(unrecognised(snapshot, members, true, true));
                table.addAll(unrecognised(snapshot, members, true, false));
                hand.addAll(unrecognised(snapshot, members, false, true));
                Map<String, Integer> seenByOpponent = new TreeMap<>();
                for (int id : members) {
                    seenByOpponent.merge(Arrays.toString(observe(snapshot, false, id, members, false)), 1, Integer::sum);
                }
                if (seenByOpponent.size() > 1) {
                    leaks.addAll(members);  // a leak in ANY snapshot is a leak
                }
            }
            if (notOnTable == null) {
                notOnTable = table;
                notInHand = hand;
            } else {
                notOnTable.retainAll(table);
                notInHand.retainAll(hand);
            }
        }

        List<String> lastingButNotRecognisableOnTable = new ArrayList<>();
        int lasting = 0;
        for (int id : ids) {
            List<String> why = lastingEffects(cardService.getCard(id));
            if (!why.isEmpty()) {
                lasting++;
                if (notOnTable.contains(id)) {
                    lastingButNotRecognisableOnTable.add(cardService.getCard(id).getClass().getSimpleName() + "#" + id + " " + key(cardService.getCard(id)) + " -> " + why);
                }
            }
        }
        List<String> handNotRecognisable = notInHand.stream().map(id -> cardService.getCard(id).getClass().getSimpleName() + "#" + id + " " + key(cardService.getCard(id))).toList();
        List<String> handLeaks = leaks.stream().map(id -> cardService.getCard(id).getClass().getSimpleName() + "#" + id).toList();
        int handChecked = ids.size();
        List<String> lastingRecognisable = new ArrayList<>();
        for (int i = 0; i < lasting - lastingButNotRecognisableOnTable.size(); i++) {
            lastingRecognisable.add("");
        }

        StringBuilder report = new StringBuilder();
        report.append("cards: ").append(ids.size()).append(", with lasting effects: ").append(lasting).append("\n");
        report.append("lasting-effect cards recognisable on the table (by both players): ").append(lastingRecognisable.size()).append("\n");
        report.append("LASTING-EFFECT CARDS THE OBSERVATION CAN NOT TELL FROM A SIMILAR CARD: ").append(lastingButNotRecognisableOnTable.size()).append("\n");
        lastingButNotRecognisableOnTable.forEach(s -> report.append("  - ").append(s).append("\n"));
        report.append("cards in the hand whose identity is NOT visible: ").append(handNotRecognisable.size()).append(" of ").append(handChecked).append("\n");
        handNotRecognisable.forEach(s -> report.append("  - ").append(s).append("\n"));
        report.append("hand cards of one player visible to the OTHER player (leak): ").append(handLeaks.size()).append("\n");
        handLeaks.forEach(s -> report.append("  - ").append(s).append("\n"));
        report.append("unverifiable (unique colour+tags): ").append(unverifiable.size()).append("\n");
        unverifiable.forEach(s -> report.append("  - ").append(s).append("\n"));
        Files.writeString(Path.of("target", "card_tracking_audit.txt"), report.toString());

        org.junit.jupiter.api.Assertions.assertTrue(handLeaks.isEmpty(), "A hand card of one player changes what the OTHER player sees: " + handLeaks);
        List<String> realGaps = lastingButNotRecognisableOnTable.stream().filter(line -> !line.endsWith("-> " + ONLY_ALLOWED_REASON)).toList();
        org.junit.jupiter.api.Assertions.assertTrue(realGaps.isEmpty(), "Cards with a lasting effect (not just 'pays again') that the observation does not show: " + realGaps);
        org.junit.jupiter.api.Assertions.assertTrue(handNotRecognisable.isEmpty(), "Hand cards the observation does not distinguish: " + handNotRecognisable);
    }
}
