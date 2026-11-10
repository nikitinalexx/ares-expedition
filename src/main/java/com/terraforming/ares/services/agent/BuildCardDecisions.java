package com.terraforming.ares.services.agent;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.turnProcessors.AiBuildProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * First step of building a project: which hand card to play, or stop (skip / leave UNMI and the phase-2 bonus to the
 * fallback). Payment and on-built inputs stay with the SMART/RANDOM helpers until they become their own decisions —
 * a joint (card × payment × input) menu does not fit the 255-candidate limit once Viral Enhancers or Mars University
 * multiply the choices.
 * <p>
 * A card is offered only when {@link AiBuildProjectService#getAvailableCardsToBuild} accepts it (colour of the current
 * build slot + {@code validateCard} with a SMART payment and a validation stub for inputs).
 */
@Service
@RequiredArgsConstructor
public class BuildCardDecisions {
    public static final int STOP = CardFeatures.STOP_ID;
    /** Hand can grow past the opening size; 80 cards plus stop is still under the chosen-index byte. */
    public static final int MAX_CANDIDATES = 81;
    public static final int CANDIDATE_FEATURES = CardFeatures.LENGTH;
    public static final String OBSERVATION_LAYOUT = ObservationBuilder.LAYOUT
            + "; candidate=[classSignature(" + CardFeatures.SIGNATURE + "), color(" + CardFeatures.COLORS
            + "), tags(" + CardFeatures.TAGS + "), price/50, active, stop]; stopId=" + STOP
            + "; payment and on-built inputs are not chosen here";

    private final ObservationBuilder observationBuilder;
    private final AiBuildProjectService aiBuildProjectService;

    public static DecisionFile.Schema schema() {
        return new DecisionFile.Schema(DecisionType.BUILD_CARD, ObservationBuilder.size(),
                DecisionType.BUILD_CARD.maxCandidates(), CANDIDATE_FEATURES, OBSERVATION_LAYOUT);
    }

    public Decision build(MarsGame game, Player player) {
        List<Card> cards = new ArrayList<>(aiBuildProjectService.getAvailableCardsToBuild(game, player));
        cards.sort(Comparator.comparingInt(Card::getId));
        List<Candidate> candidates = new ArrayList<>(cards.size() + 1);
        for (Card card : cards) {
            candidates.add(new Candidate(card.getId(), CardFeatures.of(card, false)));
        }
        candidates.add(new Candidate(STOP, CardFeatures.of(null, true)));
        if (candidates.size() > MAX_CANDIDATES) {
            throw new IllegalStateException("Build card menu has " + candidates.size()
                    + " candidates; the decision is sized for " + MAX_CANDIDATES);
        }
        return new Decision(DecisionType.BUILD_CARD, player.getUuid(),
                observationBuilder.build(game, player), candidates);
    }
}
