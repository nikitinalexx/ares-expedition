package com.terraforming.ares.services.ai.turnProcessors.network2.buildParams;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.blue.ResearchGrant;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.AiDiscoveryDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AiCardBuildInputAnalyzer {

    private static final int PLANT_FLAG = -1;
    private final AiDiscoveryDecisionService aiDiscoveryDecisionService;
    private final CardService cardService;

    /**
     * Анализирует список карт и группирует их требования к входным данным
     */
    public SharedInputAnalysis analyzeCards(Player player, List<Card> cards) {
        SharedInputAnalysis analysis = new SharedInputAnalysis();

        analyzeActiveEffectRequirements(player, cards, analysis);

        for (Card card : cards) {
            analyzeBaseCardRequirements(card.getCardMetadata(), analysis);
        }


        return analysis;
    }

    public void analyzeBaseCardRequirements(
            CardMetadata cardMetadata,
            SharedInputAnalysis analysis
    ) {
        CardAction action = cardMetadata.getCardAction();

        if (action != null) {
            switch (action) {
                case CRYOGENIC_SHIPMENT -> {
                    analysis.requiresPhaseUpgrade = true;
                    analysis.requiresMicrobeInput = true;
                    analysis.requiresAnimalInput = true;
                    return;
                }

                case BIOMEDICAL_IMPORTS -> {
                    analysis.requiresPhaseUpgrade = true;
                    analysis.requiresOxygenCheck = true;
                    return;
                }

                case UPDATE_PHASE_CARD_TWICE -> {
                    analysis.requiresPhaseUpgrade = true;
                    return;
                }

                case TOPOGRAPHIC_MAPPING -> {
                    analysis.requiresPhaseUpgrade = true;
                    analysis.requiresTagChoice = true;
                    return;
                }

                case UPDATE_PHASE_1_CARD -> {
                    analysis.requiresPhase1Upgrade = true;
                    return;
                }

                case UPDATE_PHASE_2_CARD -> {
                    analysis.requiresPhase2Upgrade = true;
                    return;
                }

                case COMMUNICATIONS_STREAMLINING -> {
                    analysis.requiresPhase3Upgrade = true;
                    return;
                }

                case UPDATE_PHASE_4_CARD -> {
                    analysis.requiresPhase4Upgrade = true;
                    return;
                }

                case CHOOSE_TAG -> {
                    analysis.requiresTagChoice = true;
                    return;
                }

                case ASTROFARM -> {
                    analysis.requiresMicrobeInput = true;
                    return;
                }

                case EOS_CHASMA,
                     LARGE_CONVOY -> {
                    analysis.requiresAnimalInput = true;
                    return;
                }

                case IMPORTED_HYDROGEN,
                     IMPORTED_NITROGEN,
                     LOCAL_HEAT_TRAPPING -> {
                    analysis.requiresMicrobeInput = true;
                    analysis.requiresAnimalInput = true;
                    return;
                }

                case SYNTHETIC_CATASTROPHE -> {
                    analysis.requiresRedCard = true;
                    return;
                }
            }

            if (AiConstants.CARD_ACTIONS_WITH_PHASE_UPGRADE_EFFECT.contains(action)) {
                analysis.requiresPhaseUpgrade = true;
                return;
            }
        }

        if (analysis.viralEnhancersActive) {
            analysis.requiresMicrobeInput = true;
            analysis.requiresAnimalInput = true;
        }

        var resourcesOnBuild = cardMetadata.getResourcesOnBuild();

        if (!resourcesOnBuild.isEmpty()
                && resourcesOnBuild.getFirst().getType() == CardCollectableResource.ANY) {

            analysis.requiresMicrobeInput = true;
            analysis.requiresAnimalInput = true;
            analysis.requiresScienceResourceInput = true;
        }
    }



    public void analyzeActiveEffectRequirements(Player player, List<Card> newCards, SharedInputAnalysis analysis) {
        List<Card> playedCards = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .toList();

        Set<CardAction> activeTagEffects = Stream.concat(playedCards.stream(), newCards.stream()).map(card -> card.getCardMetadata().getCardAction()).collect(Collectors.toSet());

        Map<Tag, Long> tagLongMap = cardService.countTagsOnCards(newCards);
        boolean containsDynamicTag = tagLongMap.containsKey(Tag.DYNAMIC);

        boolean containsAnimalPlantMicrobe = containsDynamicTag || tagLongMap.containsKey(Tag.ANIMAL) || tagLongMap.containsKey(Tag.PLANT) || tagLongMap.containsKey(Tag.MICROBE);

        analysis.decomposersActive = activeTagEffects.contains(CardAction.DECOMPOSERS) && containsAnimalPlantMicrobe;
        analysis.marsUniversityActive = activeTagEffects.contains(CardAction.MARS_UNIVERSITY) && (containsDynamicTag || tagLongMap.containsKey(Tag.SCIENCE));
        analysis.viralEnhancersActive = activeTagEffects.contains(CardAction.VIRAL_ENHANCERS) && containsAnimalPlantMicrobe;
    }

}

