package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.cards.blue.*;
import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.cards.green.IoMiningIndustries;
import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.awards.AbstractAward;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class CompleteTableEncoder {
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;
    private final SpecialEffectsService specialEffectsService;

    public float[] evaluate(MarsGame game, Player player, Player anotherPlayer, float[] features, int idx) {
        return new EncoderCalculator(game, player, anotherPlayer, features, idx).encodeTable();
    }

    public class EncoderCalculator {
        private final MarsGame game;
        private final Player player;
        private final Player opponent;
        private final float[] features;
        private int idx;

        public EncoderCalculator(MarsGame game, Player player, Player opponent, float[] features, int idx) {
            this.game = game;
            this.player = player;
            this.opponent = opponent;
            this.features = features;
            this.idx = idx;

        }

        public float[] encodeTable() {
            float oxygenLeftRatio = (float) game.getPlanet().oxygenLeft() / game.getPlanet().oxygenMax();
            float temperatureLeftRatio = (float) game.getPlanet().temperatureLeft() / game.getPlanet().temperatureMax();
            float oceansLeftRatio = (float) game.getPlanet().oceansLeft() / game.getPlanet().oceansMaxCount();

            features[idx++] = oxygenLeftRatio;
            features[idx++] = 1f - oxygenLeftRatio;

            features[idx++] = temperatureLeftRatio;
            features[idx++] = 1f - temperatureLeftRatio;

            features[idx++] = oceansLeftRatio;
            features[idx++] = 1f - oceansLeftRatio;

            //milestones
            List<Milestone> milestones = game.getMilestones();
            for (Milestone m : milestones) {
                // 1. Мой прогресс (0.0 - 1.0)
                float myProgress = (float) m.getValue(player, cardService) / m.getMaxValue();
                features[idx++] = myProgress;

                // 2. Прогресс оппонента
                float opponentProgress = (float) m.getValue(opponent, cardService) / m.getMaxValue();
                features[idx++] = opponentProgress;

                boolean achieved = m.isAchieved();
                features[idx++] = achieved ? 1.0f : 0.0f;
                features[idx++] = achieved ? 0 : myProgress - opponentProgress;
            }

            //awards
            List<BaseAward> awards = game.getAwards();
            for (BaseAward award : awards) {
                ToIntFunction<Player> awardProgressExtractor = ((AbstractAward) award).comparableParamExtractor(cardService);
                features[idx++] = (float) (Math.log1p(awardProgressExtractor.applyAsInt(player)) - Math.log1p(awardProgressExtractor.applyAsInt(opponent)));
            }

            collectGenericPlayerData(player);
            collectGenericPlayerData(opponent);


            return features;
        }

        private void collectGenericPlayerData(Player player) {
            for (int i = 0; i < 5; i++) {
                features[idx++] = player.getPhaseCards().get(i) == 1 ? 1 : 0;
                features[idx++] = player.getPhaseCards().get(i) == 2 ? 1 : 0;
            }

            features[idx++] = winPointsService.countWinPointsWithFloats(player, game) - player.getTerraformingRating();//TODO should we round to int? because 0.5vp animal is actually 0vp at the moment
            features[idx++] = player.getTerraformingRating();
            features[idx++] = player.getMcIncome();
            features[idx++] = player.getSteelIncome();
            features[idx++] = player.getTitaniumIncome();
            features[idx++] = player.getPlantsIncome();
            features[idx++] = player.getHeatIncome();
            features[idx++] = player.getCardIncome();
            features[idx++] = player.getMc();
            features[idx++] = player.getPlants();
            features[idx++] = player.getHeat();
            features[idx++] = player.getPlayed().size();
            features[idx++] = player.getHand().size();
            features[idx++] = player.getForests();

            DraftCardsDto draftCardsDto = draftCardsService.countCardsToTakeAndDraft(player);
            features[idx++] = draftCardsDto.getCardsToSee();
            features[idx++] = draftCardsDto.getCardsToTake();

            Map<Tag, Long> playedTagToCount = cardService.countPlayedTagsAsMap(player);
            features[idx++] = playedTagToCount.getOrDefault(Tag.SPACE, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.EARTH, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.EVENT, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.SCIENCE, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.PLANT, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.ENERGY, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.BUILDING, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.ANIMAL, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.JUPITER, 0L).intValue();
            features[idx++] = playedTagToCount.getOrDefault(Tag.MICROBE, 0L).intValue();


            Map<CardAction, Long> playedCardActions = player.getPlayed().getCards().stream()
                    .map(cardService::getCard)
                    .map(card -> card.getCardMetadata().getCardAction())
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(
                            Function.identity(),
                            Collectors.counting()
                    ));

            Set<Class<?>> playedCardClasses = player.getPlayed().getCards().stream().map(cardService::getCard).map(Card::getClass).collect(Collectors.toSet());

            Set<SpecialEffect> specialEffects = specialEffectsService.getPlayerSpecialEffects(player);

            features[idx++] = playedCardActions.getOrDefault(CardAction.HEAT_EARTH_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_ANIMAL_PLANT_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.CARD_SCIENCE_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_EARTH_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.PLANT_PLANT_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_SCIENCE_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_2_BUILDING_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_ENERGY_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_SPACE_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.HEAT_SPACE_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_EVENT_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.HEAT_ENERGY_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.PLANT_MICROBE_INCOME, 0L);
            features[idx++] = playedCardActions.getOrDefault(CardAction.MC_FOREST_INCOME, 0L);

            float forestCost = playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION) ? 7 : 8;
            features[idx++] = player.getPlants() >= forestCost ? 1f : player.getPlants() / forestCost;


            features[idx++] = playedCardActions.containsKey(CardAction.HELION_CORPORATION) ? 1 : 0;//can be buffed corporation, so requires separate field
            features[idx++] = playedCardActions.containsKey(CardAction.ECOLINE_CORPORATION) ? 1 : 0;//can be buffed
            features[idx++] = playedCardActions.containsKey(CardAction.MINING_GUILD_CORPORATION) ? 1 : 0;//can be buffed
            features[idx++] = playedCardActions.containsKey(CardAction.SATURN_SYSTEMS_CORPORATION) ? 1 : 0;//can be buffed

            {
                boolean arclightCorp = playedCardActions.containsKey(CardAction.ARCLIGHT_CORPORATION);
                features[idx++] = arclightCorp ? 1 : 0;
                features[idx++] = arclightCorp ? player.getCardResourcesCount().getOrDefault(ArclightCorporation.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedArclightCorporation.class, 0) : 0;
            }

            {
                boolean thorgateCorp = playedCardActions.containsKey(CardAction.THORGATE_CORPORATION);
                boolean energySubsidies = playedCardActions.containsKey(CardAction.ENERGY_SUBSIDIES);
                features[idx++] = (thorgateCorp ? 3 : 0) + (energySubsidies ? 4 : 0);
                features[idx++] = energySubsidies ? 1 : 0;//gives card per energy tag
            }

            {
                boolean teractorCorp = playedCardActions.containsKey(CardAction.TERACTOR_CORPORATION);
                boolean interplanetaryConference = playedCardActions.containsKey(CardAction.INTERPLANETARY_CONFERENCE);
                features[idx++] = (teractorCorp ? 3 : 0) + (interplanetaryConference ? 3 : 0);//earth discounts
                features[idx++] = interplanetaryConference ? 1 : 0;//gives card per earth/jupiter tag. gives 3mc per jupiter tag
            }

            {
                //advanced alloys +1/+1 steel/titanium
                //phobolog +1 per titanium
                boolean phobologCorp = playedCardActions.containsKey(CardAction.PHOBOLOG_CORPORATION);
                boolean advancedAlloys = playedCardClasses.contains(AdvancedAlloys.class);

                float steelBonus = (advancedAlloys ? 1 : 0);
                float totalSteelDiscount = player.getSteelIncome() * (2 + steelBonus);

                float titaniumBonus = (phobologCorp ? 1 : 0) + (advancedAlloys ? 1 : 0);
                float totalTitaniumDiscount = player.getTitaniumIncome() * (3 + titaniumBonus);

                features[idx++] = totalSteelDiscount;
                features[idx++] = totalTitaniumDiscount;
                features[idx++] = steelBonus;
                features[idx++] = titaniumBonus;
            }

            {
                boolean hyperionCorp = playedCardActions.containsKey(CardAction.HYPERION_SYSTEMS_CORPORATION);
                boolean droneAssistedConstruction = playedCardActions.containsKey(CardAction.DRONE_ASSISTED_CONSTRUCTION);
                boolean communityGardens = playedCardActions.containsKey(CardAction.COMMUNITY_GARDENS);

                features[idx++] = (hyperionCorp ? 1 : 0) + (droneAssistedConstruction ? 2 : 0) + (communityGardens ? 2 : 0);//total MC gain
                features[idx++] = (hyperionCorp ? 1 : 0);//extra MC if chose 3rd
                features[idx++] = droneAssistedConstruction ? 1 : 0;//extra 2mc if played upgraded phase this turn
                features[idx++] = (communityGardens ? 1 : 0);//extra Plant if chose 3rd
            }

            boolean exocorp = playedCardActions.containsKey(CardAction.EXOCORP_CORPORATION);
            boolean compostingFactory = playedCardActions.containsKey(CardAction.COMPOSTING_FACTORY);

            int cardSellBonus = (exocorp ? 1 : 0) + (compostingFactory ? 1 : 0);
            features[idx++] = cardSellBonus;//discount on card sell

            {
                boolean apollo = playedCardActions.containsKey(CardAction.APOLLO_CORPORATION);
                boolean olympusConference = playedCardActions.containsKey(CardAction.OLYMPUS_CONFERENCE);

                features[idx++] = (apollo ? 1 : 0) + (olympusConference ? 1 : 0);//get card on science played
            }

            {
                boolean zetacell = playedCardActions.containsKey(CardAction.ZETACELL_CORPORATION);
                boolean arcticAlgae = playedCardActions.containsKey(CardAction.ARCTIC_ALGAE);

                features[idx++] = zetacell ? 1 : 0;//signals about 2mc discount per ocean
                features[idx++] = (zetacell ? 2 : 0) + (arcticAlgae ? 4 : 0);//signals about plants per ocean
                features[idx++] = (zetacell || arcticAlgae) && !game.getPlanetAtTheStartOfThePhase().isOceansMax() ? 1 : 0;
            }

            {
                features[idx++] = specialEffects.contains(SpecialEffect.AMPLIFY_GLOBAL_REQUIREMENT) ? 1 : 0;
            }

            {
                boolean nebulabsCorp = playedCardActions.containsKey(CardAction.NEBU_LABS_CORPORATION);
                boolean communicationsStreamlining = playedCardActions.containsKey(CardAction.COMMUNICATIONS_STREAMLINING);

                features[idx++] = (nebulabsCorp ? 2 : 0) + (communicationsStreamlining ? 1 : 0);
                features[idx++] = (nebulabsCorp || communicationsStreamlining) ? Math.min(3, (int) player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count()) : 0;
            }

            {
                boolean unmiCorp = playedCardActions.containsKey(CardAction.UNMI_CORPORATION);
                features[idx++] = unmiCorp ? 1 : 0;
                features[idx++] = player.getMc() >= 6 ? 1 : 0;
            }

            {
                boolean cinematicsCorp = playedCardActions.containsKey(CardAction.INTERPLANETARY_CINEMATICS);
                boolean mediaGroup = playedCardActions.containsKey(CardAction.MEDIA_GROUP);
                features[idx++] = (cinematicsCorp ? 2 : 0) + (mediaGroup ? 5 : 0);
            }

            {
                boolean austellarCorp = playedCardActions.containsKey(CardAction.AUSTELLAR_CORPORATION);
                features[idx++] = austellarCorp ? 1 : 0;

                if (austellarCorp) {
                    Milestone m = game.getMilestones().get(player.getAustellarMilestone());

                    float myProgress = (float) m.getValue(player, cardService) / m.getMaxValue();
                    float opponentProgress = (float) m.getValue(opponent, cardService) / m.getMaxValue();

                    boolean achieved = m.isAchieved();

                    features[idx++] = achieved ? 1.0f : myProgress;
                    features[idx++] = achieved ? 1.0f : opponentProgress;
                    features[idx++] = achieved ? 0.0f : (myProgress - opponentProgress);
                    features[idx++] = achieved ? 1.0f : 0.0f;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            for (Class<?> aClass : CORPORATIONS_WITH_SINGLE_FLAG) {
                features[idx++] = playedCardClasses.contains(aClass) ? 1 : 0;
            }

            for (Class<?> aClass : BLUE_CARDS_WITH_SINGLE_FLAG) {
                features[idx++] = playedCardClasses.contains(aClass) ? 1 : 0;
            }

            //MICROBES BLOCK
            int totalMicrobeGenerationStrength = 0;
            int activatesOnMicrobeTag = 0;
            int activatesOnAnimalPlantTag = 0;
            int givesMicrobeToAnotherCapacity = 0;

            {
                boolean anaerobicMicroorganisms = playedCardActions.containsKey(CardAction.ANAEROBIC_MICROORGANISMS);
                if (anaerobicMicroorganisms) {
                    features[idx++] = 1;
                    features[idx++] = player.getCardResourcesCount().get(AnaerobicMicroorganisms.class);
                    activatesOnMicrobeTag++;
                    activatesOnAnimalPlantTag++;
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean conservedBiome = playedCardActions.containsKey(CardAction.CONSERVED_BIOME);
                if (conservedBiome) {
                    features[idx++] = 1;//also means animalGenerationPower, only card that has it
                    features[idx++] = player.getForests() / 2f;//gives 0.5vp per forest
                    givesMicrobeToAnotherCapacity++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean decomposers = playedCardActions.containsKey(CardAction.DECOMPOSERS);
                if (decomposers) {
                    features[idx++] = 1;//also means can trade micrbe to card, only card that has it
                    features[idx++] = player.getCardResourcesCount().get(Decomposers.class);
                    activatesOnMicrobeTag++;
                    activatesOnAnimalPlantTag++;
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }
            {
                boolean decomposingFungus = playedCardActions.containsKey(CardAction.DECOMPOSING_FUNGUS);
                if (decomposingFungus) {
                    features[idx++] = 1;//also means can turn any microbe/animal to 3 plants, only card that has it
                    features[idx++] = player.getCardResourcesCount().get(DecomposingFungus.class);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }
            {
                boolean ecologicalZone = playedCardActions.containsKey(CardAction.ECOLOGICAL_ZONE);
                if (ecologicalZone) {
                    features[idx++] = 1;
                    features[idx++] = player.getCardResourcesCount().get(EcologicalZone.class);//how many we collected so far
                    activatesOnAnimalPlantTag++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean extremeColdFungus = playedCardActions.containsKey(CardAction.EXTREME_COLD_FUNGUS);
                if (extremeColdFungus) {
                    features[idx++] = 1;//also means action OR get 1 plant
                    givesMicrobeToAnotherCapacity++;
                } else {
                    features[idx++] = 0;
                }
            }

            {
                boolean filterFeeders = playedCardActions.containsKey(CardAction.FILTER_FEEDERS);
                if (filterFeeders) {
                    features[idx++] = 1;//unique effect it has
                    features[idx++] = player.getCardResourcesCount().get(FilterFeeders.class);
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean ghgProduction = playedCardActions.containsKey(CardAction.GHG_PRODUCTION);
                if (ghgProduction) {
                    features[idx++] = 1;
                    features[idx++] = game.getPlanetAtTheStartOfThePhase().isTemperatureMax() ? 0 : 1;
                    features[idx++] = player.getCardResourcesCount().get(GhgProductionBacteria.class);
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean nitriteReducting = playedCardActions.containsKey(CardAction.NITRITE_REDUCTING);
                if (nitriteReducting) {
                    features[idx++] = 1;
                    features[idx++] = game.getPlanetAtTheStartOfThePhase().isOceansMax() ? 0 : 1;
                    features[idx++] = player.getCardResourcesCount().get(NitriteReductingBacteria.class);
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean regolithEaters = playedCardActions.containsKey(CardAction.REGOLITH_EATERS);
                if (regolithEaters) {
                    features[idx++] = 1;
                    features[idx++] = game.getPlanetAtTheStartOfThePhase().isOxygenMax() ? 0 : 1;
                    features[idx++] = player.getCardResourcesCount().get(RegolithEaters.class);
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean selfReplicatingBacteria = playedCardActions.containsKey(CardAction.SELF_REPLICATING_BACTERIA);
                if (selfReplicatingBacteria) {
                    features[idx++] = 1;//also means a -25mc discount option
                    features[idx++] = player.getCardResourcesCount().get(SelfReplicatingBacteria.class);
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean symbioticFungus = playedCardActions.containsKey(CardAction.SYMBIOTIC_FUNGUD);
                if (symbioticFungus) {
                    features[idx++] = 1;
                    givesMicrobeToAnotherCapacity++;
                } else {
                    features[idx++] = 0;
                }
            }

            {
                boolean tardigrades = playedCardClasses.contains(Tardigrades.class);
                if (tardigrades) {
                    features[idx++] = 1;
                    features[idx++] = player.getCardResourcesCount().get(Tardigrades.class);
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean viralEnhancers = playedCardActions.containsKey(CardAction.VIRAL_ENHANCERS);
                if (viralEnhancers) {
                    features[idx++] = 1;
                    features[idx++] = player.getCardResourcesCount().getOrDefault(ViralEnhancers.class, 0);
                    activatesOnMicrobeTag++;
                    activatesOnAnimalPlantTag++;
                    totalMicrobeGenerationStrength++;
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            {
                boolean bacterialAggregates = playedCardActions.containsKey(CardAction.BACTERIAL_AGGREGATES);
                if (bacterialAggregates) {
                    features[idx++] = 1;//also means activates on Earth tag
                    Integer microbesAccumulated = player.getCardResourcesCount().get(BacterialAggregates.class);
                    features[idx++] = microbesAccumulated;
                    if (microbesAccumulated < 5) {
                        totalMicrobeGenerationStrength++;
                    }
                } else {
                    features[idx++] = 0;
                    features[idx++] = 0;
                }
            }

            features[idx++] = activatesOnMicrobeTag;
            features[idx++] = activatesOnAnimalPlantTag;
            features[idx++] = totalMicrobeGenerationStrength;
            features[idx++] = givesMicrobeToAnotherCapacity;

            {
                boolean aiCentral = playedCardActions.containsKey(CardAction.AI_CENTRAL);
                boolean circuitBoard = playedCardActions.containsKey(CardAction.CIRCUIT_BOARD);
                boolean cityCouncil = playedCardActions.containsKey(CardAction.CITY_COUNCIL);

                int realGetCards = (aiCentral ? 2 : 0) + (circuitBoard ? 1 : 0);

                if (cityCouncil) {
                    realGetCards += (1 + game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count());
                }

                features[idx++] = realGetCards;
            }

            {
                boolean aquiferPumping = playedCardActions.containsKey(CardAction.AQUIFER_PUMPING);
                features[idx++] = aquiferPumping ? 1 : 0;
                features[idx++] = aquiferPumping && !game.getPlanetAtTheStartOfThePhase().isOceansMax() ? Math.max(0, 10 - player.getTitaniumIncome() * 2) : 0;
            }

            {
                boolean birds = playedCardClasses.contains(Birds.class);
                features[idx++] = birds ? 1 : 0;
                features[idx++] = birds ? player.getCardResourcesCount().get(Birds.class) : 0;
            }

            {
                boolean canTradeMcToTemperature = playedCardActions.containsKey(CardAction.DEVELOPED_INFRASTRUCTURE);
                boolean hasTemperatureLeft = !game.getPlanetAtTheStartOfThePhase().isTemperatureMax();

                features[idx++] = canTradeMcToTemperature && hasTemperatureLeft ? 1 : 0;
                features[idx++] = canTradeMcToTemperature
                        && hasTemperatureLeft ? (player.getPlayed().getCards().stream().map(cardService::getCard).filter(card -> card.getColor() == CardColor.BLUE).limit(5).count() >= 5 ? 5 : 10) : 0;//TODO extract stream of cards into list of cards?
            }

            {
                boolean earthCatapult = playedCardActions.containsKey(CardAction.EARTH_CATAPULT);
                boolean researchOutpost = playedCardActions.containsKey(CardAction.RESEARCH_OUTPOST);
                boolean hohmann = playedCardClasses.contains(HohmannTransferShipping.class);
                features[idx++] = (earthCatapult ? 2 : 0) + (researchOutpost ? 1 : 0) + (hohmann ? 1 : 0);
            }

            {
                boolean fish = playedCardActions.containsKey(CardAction.FISH);
                features[idx++] = fish ? 1 : 0;
                features[idx++] = fish && !game.getPlanetAtTheStartOfThePhase().isOceansMax() ? 1 : 0;
                features[idx++] = fish ? player.getCardResourcesCount().get(Fish.class) : 0;
            }

            {
                boolean herbivores = playedCardActions.containsKey(CardAction.HERBIVORES);
                features[idx++] = herbivores ? 1 : 0;
                features[idx++] = herbivores ? player.getCardResourcesCount().get(Herbivores.class) : 0;
            }

            {
                boolean interplanetaryRelations = playedCardActions.containsKey(CardAction.INTERPLANETARY_RELATIONS);
                features[idx++] = interplanetaryRelations ? 1 : 0;
                features[idx++] = interplanetaryRelations ? (player.getPlayed().size() - 1) : 0;
            }

            {
                boolean assemblyLines = playedCardActions.containsKey(CardAction.ASSEMBLY_LINES);
                features[idx++] = assemblyLines ? 1 : 0;
                features[idx++] = assemblyLines ? playedCardClasses.stream().filter(EncoderConstants.ASSEMBLY_LINES_EASY_CARDS::contains).count() : 0;
            }

            {
                boolean ironWorks = playedCardActions.containsKey(CardAction.IRON_WORKS);
                features[idx++] = ironWorks && !game.getPlanetAtTheStartOfThePhase().isOxygenMax() ? 1 : 0;
            }

            {
                boolean steelWorks = playedCardActions.containsKey(CardAction.STEELWORKS);
                features[idx++] = steelWorks ? 1 : 0;//can get 2mc when spent 6heat
                features[idx++] = steelWorks && !game.getPlanetAtTheStartOfThePhase().isOxygenMax() ? 1 : 0;//loses oxygen raise if it is max
            }

            {
                boolean livestock = playedCardActions.containsKey(CardAction.LIVESTOCK);
                features[idx++] = livestock ? 1 : 0;
                features[idx++] = livestock && !game.getPlanetAtTheStartOfThePhase().isTemperatureMax() ? 1 : 0;
                features[idx++] = livestock ? player.getCardResourcesCount().get(Livestock.class) : 0;
            }

            {
                boolean matterGeneratorSellBonus = playedCardActions.containsKey(CardAction.MATTER_GENERATOR);
                features[idx++] = matterGeneratorSellBonus ? 3 - cardSellBonus : 0;
            }

            {
                boolean physicsComplex = playedCardActions.containsKey(CardAction.PHYSICS_COMPLEX);
                features[idx++] = physicsComplex ? 1 : 0;
                features[idx++] = physicsComplex && !game.getPlanetAtTheStartOfThePhase().isTemperatureMax() ? 1 : 0;
                features[idx++] = physicsComplex ? player.getCardResourcesCount().get(PhysicsComplex.class) : 0;
            }

            {
                boolean canTradeMcToOxygen = playedCardActions.containsKey(CardAction.PROGRESSIVE_POLICIES);
                boolean hasOxygenLeft = !game.getPlanetAtTheStartOfThePhase().isOxygenMax();

                features[idx++] = canTradeMcToOxygen && hasOxygenLeft ? 1 : 0;
                features[idx++] = canTradeMcToOxygen
                        && hasOxygenLeft ? (playedTagToCount.getOrDefault(Tag.EVENT, 0L) >= 4 ? 5 : 10) : 0;
            }

            {
                boolean recycledDetritus = playedCardActions.containsKey(CardAction.RECYCLED_DETRITUS);
                boolean impactAnalysis = playedCardActions.containsKey(CardAction.IMPACT_ANALYSIS);

                features[idx++] = (recycledDetritus ? 2 : 0) + (impactAnalysis ? 1 : 0);
            }

            {
                boolean smallAnimals = playedCardActions.containsKey(CardAction.SMALL_ANIMALS);
                features[idx++] = smallAnimals ? 1 : 0;
                features[idx++] = smallAnimals ? player.getCardResourcesCount().get(SmallAnimals.class) : 0;
            }

            {
                boolean solarPunk = playedCardActions.containsKey(CardAction.SOLAR_PUNK);
                features[idx++] = solarPunk ? 1 : 0;
                features[idx++] = solarPunk ? Math.max(0, 15 - player.getTitaniumIncome() * 2) : 0;
            }

            {
                boolean volcanicPools = playedCardActions.containsKey(CardAction.VOLCANIC_POOLS);
                boolean hasOceansLeft = !game.getPlanetAtTheStartOfThePhase().isOceansMax();

                features[idx++] = volcanicPools && hasOceansLeft ? 1 : 0;
                features[idx++] = volcanicPools && hasOceansLeft ? Math.max(0, 12 - playedTagToCount.getOrDefault(Tag.ENERGY, 0L)) : 0;
            }

            {
                boolean waterImportFromEuropa = playedCardActions.containsKey(CardAction.WATER_IMPORT);
                boolean ioMiningIndustries = playedCardClasses.contains(IoMiningIndustries.class);
                boolean hasOceansLeft = !game.getPlanetAtTheStartOfThePhase().isOceansMax();

                features[idx++] = waterImportFromEuropa && hasOceansLeft ? 1 : 0;
                features[idx++] = waterImportFromEuropa && hasOceansLeft ? Math.max(0, 12 - player.getTitaniumIncome()) : 0;
                features[idx++] = (waterImportFromEuropa ? 1 : 0) + (ioMiningIndustries ? 1 : 0);
            }

            {
                boolean woodBurningStoves = playedCardActions.containsKey(CardAction.WOOD_BURNING_STOVES);
                features[idx++] = woodBurningStoves && !game.getPlanetAtTheStartOfThePhase().isTemperatureMax() ? 1 : 0;
            }

            {
                boolean afforestation = playedCardActions.containsKey(CardAction.COMMUNITY_AFFORESTATION);

                features[idx++] = afforestation ? 1 : 0;
                features[idx++] = afforestation ? Math.max(0, 14 - game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count() * 4) : 0;
            }

            {
                boolean fibrousComposite = playedCardActions.containsKey(CardAction.FIBROUS_COMPOSITE_MATERIAL);
                features[idx++] = fibrousComposite ? 1 : 0;
                features[idx++] = fibrousComposite ? player.getCardResourcesCount().get(FibrousCompositeMaterial.class) : 0;
            }

            {
                boolean gasCooledReactors = playedCardActions.containsKey(CardAction.GAS_COOLED_REACTORS);
                boolean hasTemperatureLeft = !game.getPlanetAtTheStartOfThePhase().isTemperatureMax();

                features[idx++] = gasCooledReactors && hasTemperatureLeft ? 1 : 0;
                features[idx++] = gasCooledReactors && hasTemperatureLeft ? (12 - player.getPhaseCards().stream().filter(phaseCard -> phaseCard != 0).count() * 2) : 0;
            }

            {
                boolean researchGrant = playedCardActions.containsKey(CardAction.RESEARCH_GRANT);
                if (researchGrant) {
                    final List<Tag> cardTags = player.getCardToTag().get(ResearchGrant.class);
                    int dynamicFlagCount = 0;
                    for (Tag cardTag : cardTags) {
                        if (cardTag == Tag.DYNAMIC) {
                            dynamicFlagCount++;
                        }
                    }
                    features[idx++] = dynamicFlagCount / 3f;
                } else {
                    features[idx++] = 0;
                }
            }

            {
                boolean volcanicSoil = playedCardActions.containsKey(CardAction.VOLCANIC_SOIL);
                features[idx++] = volcanicSoil && !game.getPlanetAtTheStartOfThePhase().isTemperatureMax() ? 1 : 0;
            }

            {
                boolean zoos = playedCardActions.containsKey(CardAction.ZOOS);
                features[idx++] = zoos ? 1 : 0;
                features[idx++] = zoos ? player.getCardResourcesCount().get(Zoos.class) : 0;
            }
        }

        private final List<Class<?>> CORPORATIONS_WITH_SINGLE_FLAG = List.of(
                CelestiorCorporation.class,
                DevTechs.class,
                LaunchStarIncorporated.class,
                CredicorCorporation.class,
                ModproCorporation.class,
                MayNiProductionsCorporation.class,
                SultiraCorporation.class
        );

        private final List<Class<?>> BLUE_CARDS_WITH_SINGLE_FLAG = List.of(
                AdvancedScreeningTechnology.class,
                AntiGravityTechnology.class,
                ArtificialJungle.class,
                AssetLiquidation.class,
                BrainstormingSession.class,
                CaretakerContract.class,
                DevelopmentCenter.class,
                FarmersMarket.class,
                FarmingCoops.class,
                GreenHouses.class,
                HydroElectricEnergy.class,
                MarsUniversity.class,
                MatterManufactoring.class,
                OptimalAerobraking.class,
                PowerInfrastructure.class,
                RedraftedContracts.class,
                RestructuredResources.class,
                StandardTechnology.class,
                ThinkTank.class,
                ExperimentalTechnology.class,
                OrbitalOutpost.class,
                SoftwareStreamlining.class,
                VirtualEmployeeDevelopment.class
        );

    }

}
