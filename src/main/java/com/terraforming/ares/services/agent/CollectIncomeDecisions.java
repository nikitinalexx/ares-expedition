package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardColor;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Phase-4 double-produce upgrade: which played green {@link Card#canPayAgain()} card to collect again, or stop
 * (collect income once only). Offered only when the player chose phase 4 with the double-produce upgrade and has at
 * least one eligible card; otherwise income is collected with no decision.
 */
@Service
@RequiredArgsConstructor
public class CollectIncomeDecisions {
    public static final int STOP = CardFeatures.STOP_ID;
    public static final int MAX_CANDIDATES = 80;
    public static final int CANDIDATE_FEATURES = CardFeatures.LENGTH;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[classSignature(" + CardFeatures.SIGNATURE + "), color(" + CardFeatures.COLORS
            + "), tags(" + CardFeatures.TAGS + "), price/50, active, stop]; stopId=" + STOP
            + "; phase-4 double-produce pick among green canPayAgain cards";

    private final ObservationBuilder observationBuilder;
    private final CardService cardService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.COLLECT_INCOME, ObservationBuilder.size(),
                DecisionType.COLLECT_INCOME.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public boolean needsChoice(Player player) {
        Integer phase = player.getChosenPhase();
        return phase != null
                && phase == 4
                && player.hasPhaseUpgrade(Constants.PHASE_4_UPGRADE_DOUBLE_PRODUCE)
                && !eligible(player).isEmpty();
    }

    public List<Card> eligible(Player player) {
        List<Card> result = new ArrayList<>();
        for (Integer id : player.getPlayed().getCards()) {
            Card card = cardService.getCard(id);
            if (card.getColor() == CardColor.GREEN && card.canPayAgain()) {
                result.add(card);
            }
        }
        result.sort(Comparator.comparingInt(Card::getId));
        return result;
    }

    public Decision build(MarsGame game, Player player) {
        List<Card> cards = eligible(player);
        List<Candidate> candidates = new ArrayList<>(cards.size() + 1);
        for (Card card : cards) {
            candidates.add(new Candidate(card.getId(), CardFeatures.of(card, false)));
        }
        candidates.add(new Candidate(STOP, CardFeatures.of(null, true)));
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Collect-income menu has " + candidates.size()
                    + " candidates; the decision is sized for " + MAX_CANDIDATES);
        }
        return new Decision(DecisionType.COLLECT_INCOME, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }
}
