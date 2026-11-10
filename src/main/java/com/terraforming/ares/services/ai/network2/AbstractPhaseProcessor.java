package com.terraforming.ares.services.ai.network2;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.ArclightCorporation;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.payments.Payment;
import com.terraforming.ares.model.payments.PaymentType;
import com.terraforming.ares.services.ai.network2.buildParams.AiMarsUniversityInputHandler;
import com.terraforming.ares.services.ai.network2.dto.CardWithChanceAndInput;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.ai.turnProcessors.frontier.State;

import java.util.List;
import java.util.Map;

public abstract class AbstractPhaseProcessor {
    private final AiTurnService aiTurnService;
    private final AiMarsUniversityInputHandler aiMarsUniversityInputHandler;

    protected AbstractPhaseProcessor(AiTurnService aiTurnService, AiMarsUniversityInputHandler aiMarsUniversityInputHandler) {
        this.aiTurnService = aiTurnService;
        this.aiMarsUniversityInputHandler = aiMarsUniversityInputHandler;
    }

    protected void finalizeBuildProject(MarsGame game, Player player, CardWithChanceAndInput bestCard, boolean isSimulation) {
        Map<Integer, List<Integer>> params = bestCard.getInputParameters();

        // Логика Mars University
        if (params.containsKey(InputFlag.MARS_UNIVERSITY_DUMMY_INPUT.getId())) {
            aiMarsUniversityInputHandler.updateMarsUniversityInput(game, player, bestCard.getCard().getId(), params);
        }

        aiTurnService.buildProject(game, player, bestCard.getCard().getId(), bestCard.getPayments(), params);
    }

    protected void finalizeUnmiTurn(MarsGame game, Player player, boolean isSimulation) {
        if (isSimulation) {
            aiTurnService.unmiRtCorporationTurn(game, player);
        } else {
            finalizeUnmiTurn(game, player);
        }
    }

    protected void finalizeUnmiTurn(MarsGame game, Player player) {
        aiTurnService.unmiRtCorporationTurn(game, player);
    }

    protected void finalizeDoExtraAction(MarsGame game, Player player, boolean isSimulation) {
        if (isSimulation) {
            aiTurnService.pickExtraBonusTurnSync(player, game);
        } else {
            finalizeDoExtraAction(game, player);
        }
    }

    protected void finalizeDoExtraAction(MarsGame game, Player player) {
        aiTurnService.pickExtraBonusTurnAsync(player);
    }

    protected void finalizeBlueAction(MarsGame game, Player player, Card blueCard, Map<Integer, List<Integer>> inputParams, boolean isSimulation) {
        if (isSimulation) {
            aiTurnService.performBlueAction(game, player, blueCard.getId(), inputParams);
        } else {
            finalizeBlueAction(game, player, blueCard, inputParams);
        }
    }

    protected void finalizeBlueAction(MarsGame game, Player player, Card blueCard, Map<Integer, List<Integer>> inputParams) {
        aiTurnService.performBlueAction(game, player, blueCard.getId(), inputParams);
    }

    protected void applyDeltaState(Player copyPlayer, State state) {
        copyPlayer.setMc((int) (state.mc + state.extraMcValue));
        copyPlayer.setHeat(state.heat);
        copyPlayer.setPlants(state.plants);
        copyPlayer.setTerraformingRating(state.tr);
        copyPlayer.setForests(state.forests);

        Map<Class<?>, Integer> cardResourcesCount = copyPlayer.getCardResourcesCount();
        if (cardResourcesCount.containsKey(GhgProductionBacteria.class)) {
            cardResourcesCount.put(GhgProductionBacteria.class, (int) state.ghgBacteriaCount);
        }
        if (cardResourcesCount.containsKey(NitriteReductingBacteria.class)) {
            cardResourcesCount.put(NitriteReductingBacteria.class, (int) state.nitriteReductingBacteria);
        }
        if (cardResourcesCount.containsKey(RegolithEaters.class)) {
            cardResourcesCount.put(RegolithEaters.class, (int) state.regolithEaters);
        }
        if (cardResourcesCount.containsKey(SelfReplicatingBacteria.class)) {
            cardResourcesCount.put(SelfReplicatingBacteria.class, (int) state.selfReplicatingBacteria);
        }

        if (cardResourcesCount.containsKey(AnaerobicMicroorganisms.class)) {
            cardResourcesCount.put(AnaerobicMicroorganisms.class, (int) state.anaerobicMicroorganisms);
        }
        if (cardResourcesCount.containsKey(BacterialAggregates.class)) {
            cardResourcesCount.put(BacterialAggregates.class, (int) state.bacterialAggregates);
        }
        if (cardResourcesCount.containsKey(Decomposers.class)) {
            cardResourcesCount.put(Decomposers.class, (int) state.decomposers);
        }
        if (cardResourcesCount.containsKey(DecomposingFungus.class)) {
            cardResourcesCount.put(DecomposingFungus.class, (int) state.decomposingFungus);
        }

        if (cardResourcesCount.containsKey(Birds.class)) {
            cardResourcesCount.put(Birds.class, (int) state.birds);
        }

        if (cardResourcesCount.containsKey(FilterFeeders.class)) {
            cardResourcesCount.put(FilterFeeders.class, (int) state.filterFeeders);
        }

        if (cardResourcesCount.containsKey(Fish.class)) {
            cardResourcesCount.put(Fish.class, (int) state.fish);
        }

        if (cardResourcesCount.containsKey(Livestock.class)) {
            cardResourcesCount.put(Livestock.class, (int) state.livestock);
        }

        if (cardResourcesCount.containsKey(Zoos.class)) {
            cardResourcesCount.put(Zoos.class, (int) state.zoos);
        }

        if (cardResourcesCount.containsKey(EcologicalZone.class)) {
            cardResourcesCount.put(EcologicalZone.class, (int) state.ecologicalZone);
        }

        if (cardResourcesCount.containsKey(Herbivores.class)) {
            cardResourcesCount.put(Herbivores.class, (int) state.herbivores);
        }

        if (cardResourcesCount.containsKey(SmallAnimals.class)) {
            cardResourcesCount.put(SmallAnimals.class, (int) state.smallAnimals);
        }

        if (cardResourcesCount.containsKey(Tardigrades.class)) {
            cardResourcesCount.put(Tardigrades.class, (int) state.tardigrades);
        }

        if (cardResourcesCount.containsKey(ArclightCorporation.class)) {
            cardResourcesCount.put(ArclightCorporation.class, (int) state.arclight);
        }

        if (cardResourcesCount.containsKey(BuffedArclightCorporation.class)) {
            cardResourcesCount.put(BuffedArclightCorporation.class, (int) state.arclight);
        }

        if (cardResourcesCount.containsKey(PhysicsComplex.class)) {
            cardResourcesCount.put(PhysicsComplex.class, (int) state.physixComplex);
        }

        if (cardResourcesCount.containsKey(FibrousCompositeMaterial.class)) {
            cardResourcesCount.put(FibrousCompositeMaterial.class, (int) state.fibrousComposite);
        }
    }

}
