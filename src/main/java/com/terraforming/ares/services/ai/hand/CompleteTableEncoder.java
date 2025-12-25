package com.terraforming.ares.services.ai.hand;

import com.terraforming.ares.cards.buffedCorporations.BuffedArclightCorporation;
import com.terraforming.ares.cards.buffedCorporations.BuffedHelionCorporation;
import com.terraforming.ares.cards.corporations.*;
import com.terraforming.ares.dto.DraftCardsDto;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.awards.AbstractAward;
import com.terraforming.ares.model.awards.BaseAward;
import com.terraforming.ares.model.milestones.Milestone;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
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
    public static final int VECTOR_SIZE = 1000;
    private final CardService cardService;
    private final HandEncoderHelperService handEncoderHelperService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;

    public class EncoderCalculator {
        private final MarsGame game;
        private final Player player;
        private final Player opponent;
        private final float[] features;
        private int idx = 0;

        public EncoderCalculator(MarsGame game, Player player, Player opponent) {
            this.game = game;
            this.player = player;
            this.opponent = opponent;
            this.features = new float[VECTOR_SIZE];
        }

        public float[] encodeHand() {


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

            features[idx++] = playedCardActions.containsKey(CardAction.HELION_CORPORATION) ? 1 : 0;//can be buffed corporation, so requires separate field

            {
                boolean arclightCorp = playedCardActions.containsKey(CardAction.ARCLIGHT_CORPORATION);
                features[idx++] = arclightCorp ? 1 : 0;
                features[idx++] = arclightCorp ? player.getCardResourcesCount().getOrDefault(ArclightCorporation.class, 0) + player.getCardResourcesCount().getOrDefault(BuffedArclightCorporation.class, 0) : 0;
            }

            //TODO thorgate
            //TODO teractor
            //TODO sultira
            //TODO phobolog
            //TODO hyperion
            //todo exocorp
            //TODO apollo
            //TODO zetacell with arctic algae? todo check hand common effect
            //TODO austellar
            //TODO ecoline?
            //TODO inventrix
            //TODO nebulabs
            //TODO unmi
            //TODO cinematics

        }

        private List<Class<?>> CORPORATIONS_WITH_SINGLE_FLAG = List.of(
                CelestiorCorporation.class,
                DevTechs.class,
                LaunchStarIncorporated.class,
                CredicorCorporation.class,
                MiningGuildCorporation.class,
                SaturnSystemsCorporation.class,
                ModproCorporation.class,
                MayNiProductionsCorporation.class
        );

    }

}
