package com.terraforming.ares.services.policyai.service;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.BuildType;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.turnProcessors.AiTurnService;
import com.terraforming.ares.services.policyai.action.ActionHead;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import com.terraforming.ares.services.policyai.input.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PolicyFirstSecondPhaseActionProcessor extends AbstractPolicyProcessor {
    private final AiTurnService aiTurnService;
    private final CardService cardService;
    private final PolicyTableAndHandEncoder encoder;
    private final List<InputGeneratorEffect> inputGeneratorEffects;

    public PolicyFirstSecondPhaseActionProcessor(AiTurnService aiTurnService, CardService cardService, PolicyTableAndHandEncoder encoder, Phase1UpgradeInputEffect phase1UpgradeInputEffect, Phase2UpgradeInputEffect phase2UpgradeInputEffect, Phase3UpgradeInputEffect phase3UpgradeInputEffect, Phase4UpgradeInputEffect phase4UpgradeInputEffect, UniversalPhaseUpgradeInputEffect universalPhaseUpgradeInputEffect, DoublePhaseUpgradeInputEffect doublePhaseUpgradeInputEffect, BiomedicalImportsInputEffect biomedicalImportsInputEffect, CryogenicShipmentInputEffect cryogenicShipmentInputEffect, EosChasmaNationalParkInputEffect eosChasmaNationalParkInputEffect, ViralEnhancersInputEffect viralEnhancersInputEffect, DecomposersInputEffect decomposersInputEffect, MarsUniversityInputEffect marsUniversityInputEffect, ImportedNitrogenInputEffect importedNitrogenInputEffect, LargeConvoyInputEffect largeConvoyInputEffect, LocalHeatTrappingInputEffect localHeatTrappingInputEffect, AstrofarmInputEffect astrofarmInputEffect, CeosFavoriteProjectInputEffect ceosFavoriteProjectInputEffect, SyntheticCatastropheInputEffect syntheticCatastropheInputEffect, ImportedHydrogenInputEffect importedHydrogenInputEffect) {
        this.aiTurnService = aiTurnService;
        this.cardService = cardService;
        this.encoder = encoder;

        this.inputGeneratorEffects = new ArrayList<>();
        inputGeneratorEffects.add(phase1UpgradeInputEffect);
        inputGeneratorEffects.add(phase2UpgradeInputEffect);
        inputGeneratorEffects.add(phase3UpgradeInputEffect);
        inputGeneratorEffects.add(phase4UpgradeInputEffect);
        inputGeneratorEffects.add(universalPhaseUpgradeInputEffect);
        inputGeneratorEffects.add(doublePhaseUpgradeInputEffect);
        inputGeneratorEffects.add(biomedicalImportsInputEffect);
        inputGeneratorEffects.add(cryogenicShipmentInputEffect);
        inputGeneratorEffects.add(eosChasmaNationalParkInputEffect);
        inputGeneratorEffects.add(decomposersInputEffect);
        inputGeneratorEffects.add(viralEnhancersInputEffect);
        inputGeneratorEffects.add(marsUniversityInputEffect);
        inputGeneratorEffects.add(importedNitrogenInputEffect);
        inputGeneratorEffects.add(largeConvoyInputEffect);
        inputGeneratorEffects.add(localHeatTrappingInputEffect);
        inputGeneratorEffects.add(astrofarmInputEffect);
        inputGeneratorEffects.add(ceosFavoriteProjectInputEffect);
        inputGeneratorEffects.add(syntheticCatastropheInputEffect);
        inputGeneratorEffects.add(importedHydrogenInputEffect);
    }

    public void processTurn(List<TurnType> possibleTurns, MarsGame game, Player player) {
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        boolean isHelion = player.getSelectedCorporationCard() != null
                && cardService.getCard(player.getSelectedCorporationCard())
                .getCardMetadata().getCardAction() == CardAction.HELION_CORPORATION;

        while (true) {
            PolicyRecord record = new PolicyRecord();
            TableContext context = encoder.encode(game, player, anotherPlayer, record);

            List<Card> possibleBuilds = getAvailableProjects(game, player);

            Map<HeadAction, Card> actionToBuildCard = new HashMap<>();
            List<HeadAction> validActions = new ArrayList<>();

            for (Card possibleBuild : possibleBuilds) {
                HeadAction action = ActionInputService.buildProjectAction(possibleBuild);
                validActions.add(action);
                actionToBuildCard.put(action, possibleBuild);
            }
            validActions.add(game.getCurrentPhase() == 2 ? ActionInputService.passSecondPhase() : ActionInputService.passFirstPhase());
            if (!player.getHand().isEmpty() && ThreadLocalRandom.current().nextInt(20) == 1) {
                validActions.add(ActionInputService.enterSellingModeAction());
            }
            if (possibleTurns.contains(TurnType.UNMI_RT) && player.getMc() >= 6) {
                validActions.add(ActionInputService.unmiAction());
            }
            if (player.canBuildAny(List.of(BuildType.BLUE_RED_OR_MC, BuildType.BLUE_RED_OR_CARD))) {
                validActions.add(ActionInputService.takeBonusAction());
            }
            if (isHelion && player.getHeat() > 0) {
                validActions.add(ActionInputService.enterHelionModeAction());
            }

            HeadAction bestTurn = predict(record, validActions, game);
            if (bestTurn == ActionInputService.passFirstPhase() || bestTurn == ActionInputService.passSecondPhase()) {
                skipTurn(player);
                return;
            }

            if (bestTurn == ActionInputService.takeBonusAction()) {
                aiTurnService.pickExtraBonusTurnAsync(player);
                return;
            }

            if (bestTurn == ActionInputService.unmiAction()) {
                aiTurnService.unmiRtCorporationTurn(game, player);
                return;
            }

            if (bestTurn == ActionInputService.enterHelionModeAction()) {
                doHelionExchange(record, player);
                return;
            }

            if (bestTurn == ActionInputService.enterSellingModeAction()) {
                sellingCardsMode(record, game, player);
                return;
            }

            if (bestTurn.head() == ActionHead.BUILD) {
                Card cardToBuild = actionToBuildCard.get(bestTurn);

                doBuild(cardToBuild, record, inputGeneratorEffects, context);
                return;
            }

            throw new IllegalStateException("Invalid best turn found");
        }
    }


    private void skipTurn(Player player) {
        aiTurnService.skipTurn(player);
    }

}
