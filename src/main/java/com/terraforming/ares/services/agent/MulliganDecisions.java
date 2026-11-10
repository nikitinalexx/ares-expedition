package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mulligan is one engine turn (a set of cards to replace, possibly empty) but a sequence of small decisions:
 * discard one remaining card, or stop. The order does not matter; each step sees only the cards not yet chosen,
 * so the set of candidates is the information about what was already queued. Stopping with nothing discarded is legal
 * and is how a player keeps the whole hand.
 * <p>
 * Candidate features ({@link #CANDIDATE_FEATURES}): class signature, colour, tags, price/50, blue action, stop flag.
 * The stop candidate is the only one with a zero signature and the stop flag set.
 */
@Service
@RequiredArgsConstructor
public class MulliganDecisions {
    public static final int STOP = CardFeatures.STOP_ID;
    public static final int SIGNATURE = CardFeatures.SIGNATURE;
    public static final int COLORS = CardFeatures.COLORS;
    public static final int TAGS = CardFeatures.TAGS;
    public static final int MAX_CANDIDATES = Constants.DEFAULT_START_HAND_SIZE + 1;
    public static final int CANDIDATE_FEATURES = CardFeatures.LENGTH;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[classSignature(" + SIGNATURE + "), color(" + COLORS + "), tags(" + TAGS
            + "), price/50, active, stop]; stopId=" + STOP;

    private final ObservationBuilder observationBuilder;
    private final CardService cardService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.MULLIGAN, ObservationBuilder.size(),
                DecisionType.MULLIGAN.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    /**
     * @param alreadyDiscarding cards already chosen in the earlier steps of this mulligan (not yet sent to the engine)
     */
    public Decision build(MarsGame game, Player player, List<Integer> alreadyDiscarding) {
        Map<Integer, Integer> left = new HashMap<>();
        for (int id : player.getHand().getCards()) {
            left.merge(id, 1, Integer::sum);
        }
        for (int id : alreadyDiscarding) {
            Integer copies = left.get(id);
            if (copies == null) {
                throw new IllegalArgumentException("Mulligan step discards " + id + " which is not left in the hand");
            }
            if (copies == 1) {
                left.remove(id);
            } else {
                left.put(id, copies - 1);
            }
        }

        List<Integer> ids = new ArrayList<>(left.keySet());
        ids.sort(Integer::compareTo);
        List<Candidate> candidates = new ArrayList<>();
        for (int id : ids) {
            for (int copy = 0; copy < left.get(id); copy++) {
                candidates.add(new Candidate(id, features(cardService.getCard(id), false)));
            }
        }
        candidates.add(new Candidate(STOP, features(null, true)));
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Mulligan hand has " + (candidates.size() - 1)
                    + " cards; the decision is sized for " + (MAX_CANDIDATES - 1));
        }
        return new Decision(DecisionType.MULLIGAN, player.getUuid(), observationBuilder.build(game, player), candidates);
    }

    /**
     * @param stop the "discard nothing more" candidate; {@code card} is null in that case
     */
    static float[] features(Card card, boolean stop) {
        return CardFeatures.of(card, stop);
    }
}
