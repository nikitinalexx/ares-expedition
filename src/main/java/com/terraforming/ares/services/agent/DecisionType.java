package com.terraforming.ares.services.agent;

/**
 * Every non-crisis decision the learnable agent owns. Crisis turns and forced engine turns ({@code SKIP_TURN},
 * {@code GAME_END_CONFIRM}, free second-phase bonus) are outside this set. Until a model is loaded for a type,
 * {@link UniformRandomAgent} chooses uniformly among the legal candidates.
 */
public enum DecisionType {
    PICK_PHASE(5, PickPhaseDecisions.OBSERVATION_LAYOUT),
    /**
     * Which offered corporation to found, together with the input its on-built effect requires.
     */
    PICK_CORPORATION(CorporationDecisions.MAX_CANDIDATES, CorporationDecisions.OBSERVATION_LAYOUT),
    /**
     * One step of the opening mulligan: discard one card that is still in hand, or stop.
     */
    MULLIGAN(MulliganDecisions.MAX_CANDIDATES, MulliganDecisions.OBSERVATION_LAYOUT),
    /**
     * Discard one card from a revealed set (a draft, or a draw-and-keep). The engine wants an exact number, so the
     * steps have no stop candidate.
     */
    DRAFT_CARDS(HandCardDecisions.MAX_CANDIDATES, HandCardDecisions.DRAFT_LAYOUT),
    /**
     * Discard one card from the hand. Same shape as a draft, but the pool is the whole hand.
     */
    DISCARD_CARDS(HandCardDecisions.MAX_CANDIDATES, HandCardDecisions.DISCARD_LAYOUT),
    /**
     * Sell one card for its sell price, or stop. Stop on the first step means "take the other action". The sale does
     * not spend the build.
     */
    SELL_CARDS(HandCardDecisions.MAX_CANDIDATES, HandCardDecisions.SELL_LAYOUT),
    /**
     * One card of the end-of-round sale that brings the hand down to the limit. Stop is absent until the limit is met.
     */
    SELL_CARDS_LAST_ROUND(HandCardDecisions.MAX_CANDIDATES, HandCardDecisions.SELL_LAST_ROUND_LAYOUT),
    /**
     * Phase-3 spend: a standard project, a plant/heat/infra conversion, Helion heat→MC (one unit), or stop.
     */
    STANDARD_ACTION(StandardActionDecisions.MAX_CANDIDATES, StandardActionDecisions.OBSERVATION_LAYOUT),
    /**
     * Which hand card to build now (payment and on-built inputs come later). Stop means skip the build.
     */
    BUILD_CARD(BuildCardDecisions.MAX_CANDIDATES, BuildCardDecisions.OBSERVATION_LAYOUT),
    /**
     * How to pay for the chosen card: Restructured / Anaerobic / MC / Helion heat splits.
     */
    BUILD_PAYMENT(BuildPaymentDecisions.MAX_CANDIDATES, BuildPaymentDecisions.OBSERVATION_LAYOUT),
    /**
     * One atomic on-built answer (phase upgrade, tag, resource target, Viral Enhancers plant, Mars University discard…).
     * A full input map is a sequence of these; payment comes after the map is complete.
     */
    BUILD_INPUT(BuildInputDecisions.MAX_CANDIDATES, BuildInputDecisions.OBSERVATION_LAYOUT),
    /**
     * Which played blue action to activate now, or stop. Action inputs come later ({@link #BLUE_INPUT}).
     */
    PERFORM_BLUE_ACTION(BlueActionDecisions.MAX_CANDIDATES, BlueActionDecisions.OBSERVATION_LAYOUT),
    /**
     * One atomic blue-action input (phase upgrade, heat spend, hand discard, resource target…).
     */
    BLUE_INPUT(BlueInputDecisions.MAX_CANDIDATES, BlueInputDecisions.OBSERVATION_LAYOUT),
    /**
     * UNMI: pay 6 MC for an extra TR after raising TR this phase, or stop.
     */
    UNMI_RT(UnmiRtDecisions.MAX_CANDIDATES, UnmiRtDecisions.OBSERVATION_LAYOUT),
    /**
     * Phase-4 double-produce: which green {@code canPayAgain} card to collect again, or stop (income once only).
     */
    COLLECT_INCOME(CollectIncomeDecisions.MAX_CANDIDATES, CollectIncomeDecisions.OBSERVATION_LAYOUT);

    private final int maxCandidates;
    private final String observationLayout;

    DecisionType(int maxCandidates, String observationLayout) {
        this.maxCandidates = maxCandidates;
        this.observationLayout = observationLayout;
    }

    /**
     * Upper bound of the number of legal candidates (records are padded to it).
     */
    public int maxCandidates() {
        return maxCandidates;
    }

    /**
     * Description of the observation layout. Changing the encoder MUST change this text, it is part of the file schema hash
     * and lets a trainer refuse data produced by a different encoder.
     */
    public String observationLayout() {
        return observationLayout;
    }
}
