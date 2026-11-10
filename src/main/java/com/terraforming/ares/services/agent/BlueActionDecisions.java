package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which played blue action to activate now, or stop. Inputs are a separate {@link DecisionType#BLUE_INPUT} sequence —
 * a joint (card × input) menu does not fit the 255-candidate limit for heat spends and hand discards.
 * <p>
 * A card is offered when it is still activatable this phase and {@link BlueInputDecisions#canActivate} finds a legal
 * input. Stop means leave leftover builds / endgame / skip to the fallback path.
 */
@Service
@RequiredArgsConstructor
public class BlueActionDecisions {
    public static final int STOP = CardFeatures.STOP_ID;
    public static final int MAX_CANDIDATES = 80;
    public static final int CANDIDATE_FEATURES = CardFeatures.LENGTH;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[classSignature(" + CardFeatures.SIGNATURE + "), color(" + CardFeatures.COLORS
            + "), tags(" + CardFeatures.TAGS + "), price/50, active, stop]; stopId=" + STOP
            + "; blue-action inputs are chosen by BLUE_INPUT";

    private final ObservationBuilder observationBuilder;
    private final CardService cardService;
    private final BlueInputDecisions blueInputDecisions;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.PERFORM_BLUE_ACTION, ObservationBuilder.size(),
                DecisionType.PERFORM_BLUE_ACTION.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public Decision build(MarsGame game, Player player) {
        List<Card> cards = available(game, player);
        cards.sort(Comparator.comparingInt(Card::getId));
        List<Candidate> candidates = new ArrayList<>(cards.size() + 1);
        for (Card card : cards) {
            candidates.add(new Candidate(card.getId(), CardFeatures.of(card, false)));
        }
        candidates.add(new Candidate(STOP, CardFeatures.of(null, true)));
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Blue action menu has " + candidates.size()
                    + " candidates; the decision is sized for " + MAX_CANDIDATES);
        }
        return new Decision(DecisionType.PERFORM_BLUE_ACTION, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }

    public List<Card> available(MarsGame game, Player player) {
        Deck activated = player.getActivatedBlueCards();
        Deck activatedTwice = player.getActivatedBlueCardsTwice();
        boolean extra = player.getBlueActionExtraActivationsLeft() > 0;
        List<Card> result = new ArrayList<>();
        for (Integer id : player.getPlayed().getCards()) {
            Card card = cardService.getCard(id);
            if (!card.isActiveCard()) {
                continue;
            }
            if (activated.containsCard(id)) {
                if (!extra || activatedTwice.containsCard(id) || isPowerInfrastructure(card)) {
                    continue;
                }
            }
            if (isPowerInfrastructure(card) && player.getHeat() <= 0) {
                continue;
            }
            if (!blueInputDecisions.canActivate(game, player, card)) {
                continue;
            }
            result.add(card);
        }
        return result;
    }

    private static boolean isPowerInfrastructure(Card card) {
        return card.getCardMetadata() != null
                && card.getCardMetadata().getCardAction() == CardAction.POWER_INFRASTRUCTURE;
    }
}
