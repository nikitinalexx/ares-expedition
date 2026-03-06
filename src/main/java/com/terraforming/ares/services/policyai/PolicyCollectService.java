package com.terraforming.ares.services.policyai;

import com.terraforming.ares.cards.blue.BacterialAggregates;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.model.milestones.MilestoneType;
import com.terraforming.ares.model.turn.TurnType;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.ai.AiConstants;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.action.ActionInputService;
import com.terraforming.ares.services.policyai.action.HeadAction;
import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.encoder.IncomeCategory;
import com.terraforming.ares.services.policyai.encoder.PolicyTableAndHandEncoder;
import com.terraforming.ares.services.policyai.encoder.PolicyTableGreenCardsFeature;
import com.terraforming.ares.services.policyai.rollout.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyCollectService {

    private final PolicyTableAndHandEncoder encoder;
    private final CardService cardService;
    private final EffectChainProcessor effectChainProcessor;
    private final SpecialEffectsService specialEffectsService;

    public void sellCards(MarsGame game,
                          Player player,
                          List<Card> cardsToSell) {

        if (!AiConstants.POLICY_TEACHING || cardsToSell.isEmpty()) {
            return;
        }

        final List<Player> players =
                new ArrayList<>(game.getPlayerUuidToPlayer().values());

        Player anotherPlayer =
                players.get(0) == player ? players.get(1) : players.get(0);

        // --- S0 ---
        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.setAction(ActionInputService.enterSellingModeAction());

        PolicyRecord prev = record;
        record = GameArenaContext.current().next();
        record.copyFrom(prev);
        record.sellingCards = true;

        for (Card card : cardsToSell) {

            // --- S_t : decision SELL ---
            record.setAction(ActionInputService.sellDiscardCardAction(card));

            if (record.handSize[0] == 1) {
                return;//after current sell there will be no cards left, so no choice
            }

            // --- S_{t+1} : RESULT of previous action ---
            prev = record;
            record = GameArenaContext.current().next();
            record.copyFrom(prev);
            record.removeCardFromHand(card);
            record.mc[0] += specialEffectsService.getCardPrice(player);
        }

        // --- final decision ---
        record.setAction(ActionInputService.passAction());
    }

    public void helionExchangeHeat(MarsGame game,
                                   Player player,
                                   int count) {

        if (!AiConstants.POLICY_TEACHING || count <= 0) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        // --- S0 ---
        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.setAction(ActionInputService.enterHelionModeAction());

        if (record.heat[0] == 1) {
            return;//there is no choice
        }

        PolicyRecord temp = record;
        record = GameArenaContext.current().next();
        record.copyFrom(temp);
        record.helionExchanging = true;

        for (int i = 0; i < count; i++) {
            record.setAction(ActionInputService.exchange1Heat());

            if (record.heat[0] == 1) {
                //no choice to log here, exit, rollout not needed
                return;
            }

            temp = record;
            record = GameArenaContext.current().next();
            record.copyFrom(temp);
            record.mc[0]++;
            record.heat[0]--;

        }

        record.setAction(ActionInputService.passAction());
    }

    public void discardingFromSelected(
            MarsGame game,
            Player player,
            List<Integer> selectableCards,
            List<Integer> cardsToDiscard
    ) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        if (cardsToDiscard.isEmpty()) {//TODO check
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        GameRecordArena arena = GameArenaContext.current();

        List<Integer> remainingSelectable = new ArrayList<>(selectableCards);


        PolicyRecord mainRecord = arena.next();
        encoder.encode(game, player, anotherPlayer, mainRecord);
        mainRecord.cardsLeftToDiscard = (byte) cardsToDiscard.size();
        mainRecord.discardingFromSelected = true;
        for (Integer cardId : remainingSelectable) {
            mainRecord.selectCard(cardService.getCard(cardId));
        }


        for (int cardToDiscard : cardsToDiscard) {
            Card card = cardService.getCard(cardToDiscard);
            mainRecord.setAction(ActionInputService.sellDiscardCardAction(card));

            if (mainRecord.cardsLeftToDiscard > 1) {
                PolicyRecord policyRecord = arena.next();
                policyRecord.copyFrom(mainRecord);
                policyRecord.removeCardFromHand(card);
                policyRecord.removeCardFromSelected(card);
                policyRecord.cardsLeftToDiscard--;

                mainRecord = policyRecord;
            }
        }
    }

    public void chooseCorporation(MarsGame game, List<Player> players, int corporationId) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        GameRecordArena arena = GameArenaContext.current();

        Player currentPlayer = players.get(0);
        Player anotherPlayer = players.get(1);

        PolicyRecord mainRecord = arena.next();
        encoder.encode(game, currentPlayer, anotherPlayer, mainRecord);
        mainRecord.initialSetup = true;
        mainRecord.pickingCorporation = true;
        mainRecord.setAction(ActionInputService.chooseCorporationAction(cardService.getCard(corporationId)));
    }

    public void choosePhase(MarsGame game, Player player, int phaseId) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        GameRecordArena arena = GameArenaContext.current();

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord mainRecord = arena.next();
        encoder.encode(game, player, anotherPlayer, mainRecord);
        mainRecord.choosingPhase = true;
        mainRecord.setAction(ActionInputService.choosePhaseTurn(phaseId));
    }

    public void sultiraCorporationPhaseChoice(MarsGame game, List<Player> players, Card sultiraCorporation, int upgrade) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        Player player = players.getFirst();

        player.setMc(sultiraCorporation.getPrice());
        player.setHeat(2);//sultira effect on itself
        player.getPlayed().getCards().add(sultiraCorporation.getId());
        player.setSelectedCorporationCard(sultiraCorporation.getId());

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, players.getLast(), mainRecord);
        mainRecord.initialSetup = true;
        mainRecord.choosingPhase1Upgrade = true;
        mainRecord.setAction(ActionInputService.choosePhaseUpgradeAction(upgrade));

        player.setMc(0);
        player.setHeat(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);
    }

    public void apolloCorporationPhaseChoice(MarsGame game, List<Player> players, Card apolloCorporation, int upgrade) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        Player player = players.getFirst();

        player.setMc(apolloCorporation.getPrice());
        player.getPlayed().getCards().add(apolloCorporation.getId());
        player.setSelectedCorporationCard(apolloCorporation.getId());

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, players.getLast(), mainRecord);
        mainRecord.initialSetup = true;
        mainRecord.choosingPhase2Upgrade = true;
        mainRecord.setAction(ActionInputService.choosePhaseUpgradeAction(upgrade));

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);
    }

    public void hyperionCorporationChoice(MarsGame game, List<Player> players, Card hyperionCorporation, int upgrade) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        Player player = players.getFirst();

        player.setMc(hyperionCorporation.getPrice());
        player.getPlayed().getCards().add(hyperionCorporation.getId());
        player.setSelectedCorporationCard(hyperionCorporation.getId());

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, players.getLast(), mainRecord);
        mainRecord.setAction(ActionInputService.choosePhaseUpgradeAction(upgrade));
        mainRecord.initialSetup = true;
        mainRecord.choosingPhase3Upgrade = true;

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);
    }

    public void exocorpCorporationChoice(MarsGame game, List<Player> players, Card exocorpCorporation, int upgrade) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        Player player = players.getFirst();

        player.setMc(exocorpCorporation.getPrice());
        player.getPlayed().getCards().add(exocorpCorporation.getId());
        player.setSelectedCorporationCard(exocorpCorporation.getId());

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, players.getLast(), mainRecord);
        mainRecord.initialSetup = true;
        mainRecord.choosingPhase5Upgrade = true;
        mainRecord.setAction(ActionInputService.choosePhaseUpgradeAction(upgrade));

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);
    }

    public void nebulabsCorporationChoice(MarsGame game, List<Player> players, Card nebulabsCorporation, int upgrade) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        Player player = players.getFirst();

        player.setMc(nebulabsCorporation.getPrice());
        player.getPlayed().getCards().add(nebulabsCorporation.getId());
        player.setSelectedCorporationCard(nebulabsCorporation.getId());

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, players.getLast(), mainRecord);
        mainRecord.universalPhaseUpgradeCount = 1;
        mainRecord.initialSetup = true;
        mainRecord.setAction(ActionInputService.choosePhaseUpgradeAction(upgrade));

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);
    }

    public void modProCorporationChoice(MarsGame game, List<Player> players, Card modproCorporation, int tag) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        Player player = players.getFirst();

        player.setMc(modproCorporation.getPrice());
        player.getPlayed().getCards().add(modproCorporation.getId());
        player.setSelectedCorporationCard(modproCorporation.getId());

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, players.getLast(), mainRecord);
        mainRecord.choosingTag = true;
        mainRecord.initialSetup = true;
        mainRecord.setAction(ActionInputService.chooseTag(tag));

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);
    }

    public void austellarTagAndMilestoneChoice(MarsGame game, List<Player> players, Card austellarCorporation, int milestone, int tag) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        Player player = players.getFirst();

        player.setMc(austellarCorporation.getPrice());
        player.getPlayed().getCards().add(austellarCorporation.getId());
        player.setSelectedCorporationCard(austellarCorporation.getId());

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, players.getLast(), mainRecord);
        mainRecord.choosingTag = true;
        mainRecord.initialSetup = true;
        mainRecord.setAction(ActionInputService.chooseTag(tag));

        PolicyRecord anotherRecord = GameArenaContext.current().next();
        anotherRecord.copyFrom(mainRecord);
        anotherRecord.choosingTag = false;
        anotherRecord.austellarChoosing = true;
        switch (Tag.values()[tag]) {
            case SPACE -> anotherRecord.spaceTags[0]++;
            case EARTH -> anotherRecord.earthTags[0]++;
            case EVENT -> anotherRecord.eventTags[0]++;
            case SCIENCE -> anotherRecord.scienceTags[0]++;
            case PLANT -> anotherRecord.plantTags[0]++;
            case ENERGY -> anotherRecord.energyTags[0]++;
            case BUILDING -> anotherRecord.buildingTags[0]++;
            case ANIMAL -> anotherRecord.animalTags[0]++;
            case JUPITER -> anotherRecord.jupiterTags[0]++;
            case MICROBE -> anotherRecord.microbeTags[0]++;
        }

        List<Milestone> milestones = game.getMilestones();
        MilestoneType chosenMilestone = milestones.get(milestone).getType();
        mainRecord.setAction(ActionInputService.austellarMilestoneChoice(AiConstants.MILESTONE_TYPES.indexOf(chosenMilestone)));

        player.setMc(0);
        player.getPlayed().getCards().clear();
        player.setSelectedCorporationCard(null);
    }

    public void skipTurn(MarsGame game, Player player) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, mainRecord);
        mainRecord.setAction(ActionInputService.passAction());
    }

    public TableContext build(MarsGame game, Player player, Card card) {
        if (!AiConstants.POLICY_TEACHING) {
            return null;
        }
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord mainRecord = GameArenaContext.current().next();
        TableContext playerContext = encoder.encode(game, player, anotherPlayer, mainRecord);
        mainRecord.setAction(ActionInputService.buildProjectAction(card));

        return playerContext;
    }

    public void collectIncomeDoubleCardChoice(MarsGame game, Player player, Card card) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }
        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord mainRecord = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, mainRecord);

        Integer specialGreenCardIndex = AiConstants.GREEN_CARDS_INDEX_BY_CLASS.get(card.getClass());

        HeadAction action;
        if (specialGreenCardIndex != null) {
            action = ActionInputService.specialGreenCardThatPays(specialGreenCardIndex);
        } else {
            IncomeCategory incomeCategory = PolicyTableGreenCardsFeature.resolveIncomeCategory(card, mainRecord, mainRecord.forests[0]);
            action = ActionInputService.specialPhasePaymentByIncomeType(incomeCategory);
        }

        mainRecord.setAction(action);
    }

    public void ceosFavoriteProject(MarsGame game,
                                    Player player,
                                    Map<Integer, List<Integer>> params,
                                    TableContext buildContext,
                                    Card ceosFavoriteCard) {

        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        if (ResourceHelper.countAnyAvailableCollectableResources(buildContext) <= 1) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        Card targetCard = cardService.getCard(params.get(InputFlag.CEOS_FAVORITE_PUT_RESOURCES.getId()).getFirst());

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.ceosFavoriteProjectEffect = true;
        record.selectCard(ceosFavoriteCard);
        if (targetCard.getCollectableResource() == CardCollectableResource.ANIMAL) {
            record.setAction(ActionInputService.getAnimalTargetAction(targetCard));
        } else if (targetCard.getCollectableResource() == CardCollectableResource.MICROBE) {
            record.setAction(ActionInputService.getMicrobeTargetAction(targetCard));
        } else {
            record.setAction(ActionInputService.getScienceResourceTargetAction(targetCard));
        }
    }

    public void syntheticCatastrophe(MarsGame game,
                                     Player player,
                                     Map<Integer, List<Integer>> params,
                                     Card syntheticCatastrophe) {

        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        int redCardsCount = 0;
        for (Integer playedCard : player.getPlayed().getCards()) {
            if (cardService.getCard(playedCard).getColor() == CardColor.RED) {
                redCardsCount++;
            }
        }
        if (redCardsCount == 1) {
            return;//no real choice, only one card on table
        }

        Card targetCard = cardService.getCard(params.get(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId()).getFirst());

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.syntheticCatastropheEffect = true;
        record.selectCard(syntheticCatastrophe);
        record.setAction(ActionInputService.getRedCardTargetAction(targetCard));
    }

    public void unmiTurn(MarsGame game,
                         Player player) {

        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        if (!record.unmiTurnAvailable) {
            throw new IllegalStateException("Making UNMI turn with unmi is not available");
        }
        record.setAction(ActionInputService.unmiAction());
    }

    public void standardProjectTurn(MarsGame game, Player player, StandardProjectType type) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.setAction(ActionInputService.standardProject(type));
    }

    public void mandatoryHeatPlantsConversion(MarsGame game, Player player, TurnType turnType) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.setAction(turnType == TurnType.INCREASE_TEMPERATURE ? ActionInputService.convertHeatToTemperature() : ActionInputService.convertPlantsToForest());
    }

    public void payForTheBuild(MarsGame game, Player player, int type, Card card) {
        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.selectCard(card);
        record.payingForTheBuild = true;

        record.setAction(ActionInputService.payForTheBuild(type));
    }

    public void takeBonusTurn(MarsGame game, Player player) {

        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.setAction(ActionInputService.takeBonusAction());
    }

    public void importedHydrogen(MarsGame game,
                                 Player player,
                                 Map<Integer, List<Integer>> params,
                                 Card importedHydrogen) {

        if (!AiConstants.POLICY_TEACHING) {
            return;
        }

        if (player.getPlayed().getCards().stream().map(cardService::getCard).noneMatch(c -> c.getCollectableResource() == CardCollectableResource.ANIMAL || c.getCollectableResource() == CardCollectableResource.MICROBE && !(c.getClass() == BacterialAggregates.class && player.getCardResourcesCount().get(BacterialAggregates.class) == 5))) {
            //there is no decision to be made, engine will simply take plants
            return;
        }

        boolean isTakingPlants = params.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId());

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        PolicyRecord record = GameArenaContext.current().next();
        encoder.encode(game, player, anotherPlayer, record);
        record.importedHydrogenEffect = true;
        record.selectCard(importedHydrogen);
        if (isTakingPlants) {
            record.setAction(ActionInputService.takePlantTargetAction());
        } else {
            Card targetCard = cardService.getCard(params.get(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId()).getFirst());
            if (targetCard.getCollectableResource() == CardCollectableResource.ANIMAL) {
                record.setAction(ActionInputService.getAnimalTargetAction(targetCard));
            } else {
                record.setAction(ActionInputService.getMicrobeTargetAction(targetCard));
            }
        }
    }

    public void onTagPlayedMixIn(MarsGame game, Player player, Card playedCard, Map<Integer, List<Integer>> params) {
        if (!AiConstants.POLICY_TEACHING) return;

        final List<Player> players = new ArrayList<>(game.getPlayerUuidToPlayer().values());
        Player anotherPlayer = players.get(0) == player ? players.get(1) : players.get(0);

        List<TagEffect> effects = List.of(
                new UpgradePhase1Effect(playedCard, params),
                new UpgradePhase2Effect(playedCard, params),
                new UpgradePhase3Effect(playedCard, params),
                new UpgradePhase4Effect(playedCard, params),
                new UniversalPhaseUpgradeEffect(playedCard, params),
                new DoublePhaseUpgradeEffect(playedCard, params),
                new BiomedicalImportsEffect(playedCard, params, game, player),
                new DynamicTagEffect(params),
                new CryogenicShipmentResourceEffect(params, cardService, player),
                new EosChasmaNationalParkEffect(params, cardService, player),
                new ViralEnhancersPlantsEffect(params, player, cardService),
                new ViralEnhancersResourceEffect(params, cardService),
                new DecomposersEffect(params),
                new MarsUniversityEffect(params, player, cardService, playedCard),
                new ImportedNitrogenMicrobeEffect(params, cardService, player),
                new ImportedNitrogenAnimalEffect(params, cardService, player),
                new LargeConvoyEffect(params, cardService, player),
                new LocalHeatTrappingEffect(params, cardService, player),
                new AstrofarmEffect(params, cardService, player)
        );

        effectChainProcessor.process(effects, game, player, anotherPlayer, playedCard);
    }

}
