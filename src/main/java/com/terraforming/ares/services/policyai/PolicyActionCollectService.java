package com.terraforming.ares.services.policyai;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardCollectableResource;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import com.terraforming.ares.services.policyai.rollout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyActionCollectService {
    private final PolicyTableAndHandEncoder encoder;
    private final CardService cardService;
    private final EffectChainProcessor effectChainProcessor;

    public void collectAction(MarsGame game, Player player, Card card, Map<Integer, List<Integer>> inputParams) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        //the first activation is ignored, because it doesn't require price, it is obvious gain
        if (AiConstants.NO_PAYMENT_BLUE_ACTIONS.contains(card.getClass()) && !player.getActivatedBlueCards().containsCard(card.getId())) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = GameArenaContext.current().next();
        TableContext playerContext = encoder.encode(game, player, anotherPlayer, record);
        record.setAction(ActionInputService.getBlueAction(card));

        if (AiConstants.NO_PAYMENT_BLUE_ACTIONS.contains(card.getClass())) {
            return;
        }

        if (card.getClass() == ConservedBiome.class) {
            if (ResourceHelper.countAvailableAnimalsAndMicrobes(playerContext) <= 1) {
                return;//no real choice, it can only choose one card
            }
            Card targetCard = cardService.getCard(inputParams.get(InputFlag.CARD_CHOICE.getId()).getFirst());

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.conservedBiomeAction = true;
            record.setAction(targetCard.getCollectableResource() == CardCollectableResource.ANIMAL ? ActionInputService.getAnimalTargetAction(targetCard) : ActionInputService.getMicrobeTargetAction(targetCard));
            return;
        }

        if (card.getClass() == DecomposingFungus.class) {
            if (ResourceHelper.countCardsWithAtLeastOneMicrobeOrAnimal(playerContext) <= 1) {//this is wrong, it can discard animals
                return;//no real choice, it can only choose one card
            }
            Card targetCard = cardService.getCard(inputParams.get(InputFlag.CARD_CHOICE.getId()).getFirst());

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.decomposingFungus = true;
            record.setAction(targetCard.getCollectableResource() == CardCollectableResource.ANIMAL ? ActionInputService.getAnimalTargetAction(targetCard) : ActionInputService.getMicrobeTargetAction(targetCard));
            return;
        }

        if (card.getClass() == ExtremeColdFungus.class) {
            if (ResourceHelper.countPlayedMicrobeCards(playerContext) == 0) {
                return;//no real choice, it always takes a plant
            }
            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.extremeColdFungus = true;
            if (inputParams.containsKey(InputFlag.EXTEME_COLD_FUNGUS_PICK_PLANT.getId())) {
                record.setAction(ActionInputService.takePlantTargetAction());
            } else {
                Card targetCard = cardService.getCard(inputParams.get(InputFlag.EXTREME_COLD_FUNGUS_PUT_MICROBE.getId()).getFirst());
                record.setAction(ActionInputService.getMicrobeTargetAction(targetCard));
            }
            return;
        }

        if (card.getClass() == SymbioticFungus.class) {//TODO wtf, it is not extreme cold fungus
            if (ResourceHelper.countPlayedMicrobeCards(playerContext) == 0) {
                return;//no real choice, it always takes a plant
            }

            Card targetCard = cardService.getCard(inputParams.get(InputFlag.CARD_CHOICE.getId()).getFirst());

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.microbePutCount = 1;
            record.setAction(ActionInputService.getMicrobeTargetAction(targetCard));
            return;
        }

        if (card.getClass() == FarmingCoops.class) {
            if (player.getHand().size() == 1) {
                return;//no real choice, it discards the last card
            }
            Card targetCard = cardService.getCard(inputParams.get(InputFlag.CARD_CHOICE.getId()).getFirst());

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.farmingCoops = true;
            record.setAction(ActionInputService.sellDiscardCardAction(targetCard));
            return;
        }

        if (card.getClass() == GreenHouses.class) {
            if (player.getHeat() == 1) {
                return;//no real choice, it exchanges the single heat it has
            }
            int count = inputParams.get(InputFlag.DISCARD_HEAT.getId()).getFirst();

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.greenHouses = true;
            record.setAction(ActionInputService.action1To4Count(count));
            return;
        }

        if (card.getClass() == MatterGenerator.class) {
            if (player.getHand().size() == 1) {
                 return;//no real choice, it discards the last card
            }
            Card targetCard = cardService.getCard(inputParams.get(InputFlag.CARD_CHOICE.getId()).getFirst());

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.matterGenerator = true;
            record.setAction(ActionInputService.sellDiscardCardAction(targetCard));
            return;
        }

        if (card.getClass() == GhgProductionBacteria.class || card.getClass() == NitriteReductingBacteria.class || card.getClass() == RegolithEaters.class || card.getClass() == SelfReplicatingBacteria.class) {
            int microbesOnCardNow = player.getCardResourcesCount().get(card.getClass());
            if (microbesOnCardNow < card.getCardMetadata().getActionsInputData().getFirst().getMax()) {
                return;//it may only take microbe, it is not enough to use
            }
            List<Integer> addDiscardInput = inputParams.get(InputFlag.ADD_DISCARD_MICROBE.getId());

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            if (card.getClass() == GhgProductionBacteria.class) {
                record.ghgProduction = true;
            }
            if (card.getClass() == NitriteReductingBacteria.class) {
                record.nitriteReductingBacteria = true;
            }
            if (card.getClass() == RegolithEaters.class) {
                record.regolithEaters = true;
            }
            if (card.getClass() == SelfReplicatingBacteria.class) {
                record.selfReplicatingBacteria = true;
            }
            record.setAction(addDiscardInput.getFirst() == 1 ? ActionInputService.actionTakeMicrobe() : ActionInputService.actionUseMicrobe());
            return;
        }

        if (card.getClass() == PowerInfrastructure.class) {
            if (player.getHeat() == 1) {
                return;//no choice to log here
            }
            int count = inputParams.get(InputFlag.DISCARD_HEAT.getId()).getFirst();

            PolicyRecord temp = record;

            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.powerInfrastructure = true;

            for (int i = 0; i < count; i++) {
                // ---- decision in S_t ----
                record.setAction(ActionInputService.exchange1Heat());

                if (record.heat[0] == 1) {
                    //no choice to log here, exit, rollout not needed
                    return;
                }

                // ---- S_{t+1} ----
                temp = record;
                record = GameArenaContext.current().next();
                record.copyFrom(temp);
                record.mc[0]++;
                record.heat[0]--;
            }

            record.setAction(ActionInputService.passAction());

            return;
        }

        if (card.getClass() == RedraftedContracts.class) {
            if (player.getHand().size() == 1) {
                return;//no choice to log here
            }
            List<Integer> cardsToDiscard = inputParams.get(InputFlag.CARD_CHOICE.getId());

            PolicyRecord prev = record;
            record = GameArenaContext.current().next();
            record.copyFrom(prev);
            record.redraftedContracts = true;
            record.redraftedDiscarded = 0;

            for (int i = 0; i < cardsToDiscard.size(); i++) {
                // ---- decision in S_t ----
                Card cardToDiscard = cardService.getCard(cardsToDiscard.get(i));
                record.setAction(ActionInputService.sellDiscardCardAction(cardToDiscard));

                if (record.handSize[0] == 1 || record.redraftedDiscarded == 2) {
                    //no choice to log here, exit, rollout not needed
                    return;
                }

                // ---- S_{t+1} ----
                prev = GameArenaContext.current().next();
                prev.copyFrom(record);
                prev.redraftedDiscarded++;
                prev.removeCardFromHand(cardToDiscard);

                record = prev;
            }

            record.setAction(ActionInputService.passAction());
            return;
        }

        if (card.getClass() == ExperimentalTechnology.class || card.getClass() == VirtualEmployeeDevelopment.class) {
            Integer phaseUpgrade = inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst();


            PolicyRecord prev = record;
            record = GameArenaContext.current().next();
            record.copyFrom(prev);
            record.universalPhaseUpgradeCount = 1;
            if (player.countPhaseUpgrades() == 5 && player.hasPhaseUpgrade(phaseUpgrade)) {
                record.setAction(ActionInputService.passAction());
            } else {
                record.setAction(ActionInputService.choosePhaseUpgradeAction(phaseUpgrade));
            }
            return;
        }

        if (card.getClass() == FibrousCompositeMaterial.class) {
            List<Integer> addDiscardInput = inputParams.get(InputFlag.ADD_DISCARD_MICROBE.getId());
            boolean shouldTakeScience = false;
            if (addDiscardInput.getFirst() > 1) {
                int phaseUpgrade = inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst();
                if (player.countPhaseUpgrades() == 5 && player.hasPhaseUpgrade(phaseUpgrade)) {
                    shouldTakeScience = true;
                }
            }

            PolicyRecord temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.fibrousComposite = true;
            if (shouldTakeScience) {
                record.setAction(ActionInputService.actionTakeMicrobe());
                return;
            }
            shouldTakeScience = addDiscardInput.getFirst() == 1;
            record.setAction(shouldTakeScience ? ActionInputService.actionTakeMicrobe() : ActionInputService.actionUseMicrobe());

            if (!shouldTakeScience) {
                temp = record;
                record = GameArenaContext.current().next();
                record.copyFrom(temp);
                record.fibrousComposite = false;
                record.universalPhaseUpgradeCount = 1;
                record.setAction(ActionInputService.choosePhaseUpgradeAction(inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId()).getFirst()));
            }

            return;
        }

        if (card.getClass() == ResearchGrant.class) {
            List<TagEffect> effects = List.of(
                    new DynamicTagEffect(inputParams),
                    new ViralEnhancersPlantsEffect(inputParams, player, cardService),
                    new ViralEnhancersResourceEffect(inputParams, cardService),
                    new DecomposersEffect(inputParams),
                    new MarsUniversityEffect(inputParams, player, cardService, card)
            );

            effectChainProcessor.process(effects, game, player, anotherPlayer, null);
            return;
        }
    }


}
