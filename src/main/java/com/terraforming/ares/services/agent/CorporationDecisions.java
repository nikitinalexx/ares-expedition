package com.terraforming.ares.services.agent;

import com.terraforming.ares.cards.corporations.ApolloIndustriesCorporation;
import com.terraforming.ares.cards.corporations.AustellarCorporation;
import com.terraforming.ares.cards.corporations.ExocorpCorporation;
import com.terraforming.ares.cards.corporations.HyperionSystemsCorporation;
import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.cards.corporations.NebuLabsCorporation;
import com.terraforming.ares.cards.corporations.SultiraCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.milestones.MilestoneType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.CardValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The corporation decision: every pair (offered corporation, legal on-built input) is one candidate.
 * Corporations without an on-built choice have a single candidate with an empty input. The input space is small
 * (at most a tag times the three milestones, plus the other offered corporation), so the whole turn is one decision
 * instead of a sequence — the engine plays it as one turn.
 * <p>
 * A corporation whose on-built effect needs an input this class does not know about is a hard error: silently dropping
 * it would make the agent unable to play that corporation, and inventing an input would be a move the engine rejects.
 */
@Service
@RequiredArgsConstructor
public class CorporationDecisions {
    public static final int SIGNATURE = CardSignature.BITS;
    public static final int PHASE_UPGRADES = 10;
    public static final int TAGS = 10;
    public static final int MILESTONE_SLOTS = 8;
    public static final int MILESTONE_TYPES = 15;
    public static final int CANDIDATE_FEATURES = SIGNATURE + PHASE_UPGRADES + TAGS + MILESTONE_SLOTS + MILESTONE_TYPES;
    public static final int MAX_CANDIDATES = 96;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[classSignature(" + SIGNATURE + "), phaseUpgrade(" + PHASE_UPGRADES + "), tag(" + TAGS
            + "), milestoneSlot(" + MILESTONE_SLOTS + "), milestoneType(" + MILESTONE_TYPES + ")]";

    private static final List<Tag> REAL_TAGS = List.of(Tag.SPACE, Tag.EARTH, Tag.EVENT, Tag.SCIENCE, Tag.PLANT,
            Tag.ENERGY, Tag.BUILDING, Tag.ANIMAL, Tag.JUPITER, Tag.MICROBE);

    static {
        if (MilestoneType.values().length != MILESTONE_TYPES) {
            throw new ExceptionInInitializerError("MilestoneType changed: update the corporation candidate layout");
        }
        if (Tag.DYNAMIC.ordinal() != TAGS || REAL_TAGS.size() != TAGS) {
            throw new ExceptionInInitializerError("Tag changed: update the corporation candidate layout");
        }
        for (int i = 0; i < REAL_TAGS.size(); i++) {
            if (REAL_TAGS.get(i).ordinal() != i) {
                throw new ExceptionInInitializerError("Tag order changed: update the corporation candidate layout");
            }
        }
    }

    private final ObservationBuilder observationBuilder;
    private final CardService cardService;
    private final CardValidationService cardValidationService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.PICK_CORPORATION, ObservationBuilder.size(),
                DecisionType.PICK_CORPORATION.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public List<CorporationChoice> options(MarsGame game, Player player) {
        List<Integer> offered = new ArrayList<>(player.getCorporations().getCards());
        offered.sort(Comparator.naturalOrder());
        if (offered.isEmpty()) {
            throw new IllegalStateException("No corporation was offered");
        }
        List<CorporationChoice> choices = new ArrayList<>();
        for (int corporationId : offered) {
            Card card = cardService.getCard(corporationId);
            for (Map<Integer, List<Integer>> input : legalInputs(game, player, card)) {
                choices.add(new CorporationChoice(corporationId, input, new Candidate(corporationId, features(card, input, game))));
            }
        }
        if (choices.size() > MAX_CANDIDATES) {
            throw new IllegalStateException(choices.size() + " corporation options do not fit into " + MAX_CANDIDATES);
        }
        return List.copyOf(choices);
    }

    public Decision decision(MarsGame game, Player player, List<CorporationChoice> choices) {
        float[] observation = observationBuilder.build(game, player);
        List<Candidate> candidates = new ArrayList<>();
        for (CorporationChoice choice : choices) {
            candidates.add(choice.candidate());
        }
        return new Decision(DecisionType.PICK_CORPORATION, player.getUuid(), observation, candidates);
    }

    /**
     * The inputs the validator accepts for this corporation. Checked again here: a template the validator rejects is a
     * bug in the template, not a move we hand to the engine.
     */
    public List<Map<Integer, List<Integer>>> legalInputs(MarsGame game, Player player, Card card) {
        List<Map<Integer, List<Integer>>> inputs = templates(game, card);
        if (inputs.isEmpty()) {
            throw new IllegalStateException(card.getClass().getSimpleName() + " produced no legal input");
        }
        for (Map<Integer, List<Integer>> input : inputs) {
            String error = cardValidationService.validateCorporation(player, game, card.getId(), input);
            if (error != null) {
                throw new IllegalStateException(card.getClass().getSimpleName() + " template " + input + " was rejected: " + error);
            }
        }
        return inputs;
    }

    private static List<Map<Integer, List<Integer>>> templates(MarsGame game, Card card) {
        if (!card.onBuiltEffectApplicableToItself()) {
            return List.of(Map.of());
        }
        if (card instanceof AustellarCorporation) {
            int slots = milestoneSlots(game);
            List<Map<Integer, List<Integer>>> inputs = new ArrayList<>();
            for (Tag tag : REAL_TAGS) {
                for (int slot = 0; slot < slots; slot++) {
                    inputs.add(Map.of(
                            InputFlag.TAG_INPUT.getId(), List.of(tag.ordinal()),
                            InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId(), List.of(slot)));
                }
            }
            return inputs;
        }
        if (card instanceof ModproCorporation) {
            List<Map<Integer, List<Integer>>> inputs = new ArrayList<>();
            for (Tag tag : REAL_TAGS) {
                inputs.add(Map.of(InputFlag.TAG_INPUT.getId(), List.of(tag.ordinal())));
            }
            return inputs;
        }
        if (card instanceof NebuLabsCorporation) {
            return phaseUpgrades(0, 9);
        }
        if (card instanceof SultiraCorporation) {
            return phaseUpgrades(0, 1);
        }
        if (card instanceof ApolloIndustriesCorporation) {
            return phaseUpgrades(2, 3);
        }
        if (card instanceof HyperionSystemsCorporation) {
            return phaseUpgrades(4, 5);
        }
        if (card instanceof ExocorpCorporation) {
            return phaseUpgrades(8, 9);
        }
        throw new IllegalStateException("Corporation " + card.getClass().getSimpleName()
                + " applies an on-built effect to itself, but the legal inputs are not described");
    }

    private static int milestoneSlots(MarsGame game) {
        int slots = game.getMilestones().size();
        if (slots > MILESTONE_SLOTS) {
            throw new IllegalStateException("The game has " + slots + " milestones; the candidate describes " + MILESTONE_SLOTS);
        }
        return slots;
    }

    private static List<Map<Integer, List<Integer>>> phaseUpgrades(int from, int to) {
        List<Map<Integer, List<Integer>>> inputs = new ArrayList<>();
        for (int upgrade = from; upgrade <= to; upgrade++) {
            inputs.add(Map.of(InputFlag.PHASE_UPGRADE_CARD.getId(), List.of(upgrade)));
        }
        return inputs;
    }

    static float[] features(Card card, Map<Integer, List<Integer>> input, MarsGame game) {
        float[] features = new float[CANDIDATE_FEATURES];
        System.arraycopy(CardSignature.of(card.getClass()), 0, features, 0, SIGNATURE);
        int pos = SIGNATURE;
        List<Integer> upgrade = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());
        if (upgrade != null) {
            int value = upgrade.get(0);
            if (value < 0 || value >= PHASE_UPGRADES) {
                throw new IllegalArgumentException("Phase upgrade " + value + " is outside 0.." + (PHASE_UPGRADES - 1));
            }
            features[pos + value] = 1f;
        }
        pos += PHASE_UPGRADES;
        List<Integer> tag = input.get(InputFlag.TAG_INPUT.getId());
        if (tag != null) {
            int value = tag.get(0);
            if (value < 0 || value >= TAGS) {
                throw new IllegalArgumentException("Tag " + value + " is not one of the " + TAGS + " real tags");
            }
            features[pos + value] = 1f;
        }
        pos += TAGS;
        List<Integer> slot = input.get(InputFlag.AUSTELLAR_CORPORATION_MILESTONE.getId());
        if (slot != null) {
            int value = slot.get(0);
            if (value < 0 || value >= MILESTONE_SLOTS) {
                throw new IllegalArgumentException("Milestone slot " + value + " is outside 0.." + (MILESTONE_SLOTS - 1));
            }
            features[pos + value] = 1f;
            Milestone milestone = game.getMilestones().get(value);
            features[pos + MILESTONE_SLOTS + milestone.getType().ordinal()] = 1f;
        }
        return features;
    }
}
