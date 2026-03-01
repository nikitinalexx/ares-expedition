package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Map;

public class PolicyTableGreenCardsFeature implements PlayerPolicyFeatureBlock {

    @Override
    public void encode(TableContext ctx, PolicyRecord dto, int playerIndex) {
        Player player = ctx.getPlayer();

        dto.playedGreenCards[playerIndex] = ctx.getTableGreenCardsMask();

        short maxMcIncomeFromSingleCard = 0;//can be around 20-30 max
        short maxHeatIncomeFromSingleCard = 0;
        byte maxPlantsIncomeFromSingleCard = 0;
        float maxCardsIncomeFromSingleCard = 0;//the only one who can be float

        for (Card playedCard : ctx.getPlayedCards()) {
            List<Gain> incomes = playedCard.getCardMetadata().getIncomes();
            for (Gain income : incomes) {
                switch (income.getType()) {
                    case MC:
                        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, income.getValue());
                        break;
                    case HEAT:
                        maxHeatIncomeFromSingleCard = (short) Math.max(maxHeatIncomeFromSingleCard, income.getValue());
                        break;
                    case PLANT:
                        maxPlantsIncomeFromSingleCard = (byte) Math.max(maxPlantsIncomeFromSingleCard, income.getValue());
                        break;
                    case CARD:
                        maxCardsIncomeFromSingleCard = Math.max(maxCardsIncomeFromSingleCard, income.getValue());
                        break;
                }
            }
        }

        Map<CardAction, Long> played = ctx.getPlayedCardActions();
        int[] tagCounts = ctx.getPlayedTagToCount();

        int earth = tagCounts[Tag.EARTH.ordinal()];
        int animal = tagCounts[Tag.ANIMAL.ordinal()];
        int plant = tagCounts[Tag.PLANT.ordinal()];
        int science = tagCounts[Tag.SCIENCE.ordinal()];
        int building = tagCounts[Tag.BUILDING.ordinal()];
        int energy = tagCounts[Tag.ENERGY.ordinal()];
        int space = tagCounts[Tag.SPACE.ordinal()];
        int event = tagCounts[Tag.EVENT.ordinal()];
        int microbe = tagCounts[Tag.MICROBE.ordinal()];
        int forests = player.getForests();

        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_ANIMAL_PLANT_INCOME, animal + plant));
        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_SCIENCE_INCOME, science));
        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_2_BUILDING_INCOME, building * 0.5f));
        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_ENERGY_INCOME, energy));
        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_SPACE_INCOME, space));
        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_EVENT_INCOME, event));
        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_FOREST_INCOME, forests));
        maxMcIncomeFromSingleCard = (short) Math.max(maxMcIncomeFromSingleCard, engine(played, CardAction.MC_EARTH_INCOME, earth));


        maxHeatIncomeFromSingleCard = (short) Math.max(maxHeatIncomeFromSingleCard, engine(played, CardAction.HEAT_EARTH_INCOME, earth));
        maxHeatIncomeFromSingleCard = (short) Math.max(maxHeatIncomeFromSingleCard, engine(played, CardAction.HEAT_SPACE_INCOME, space));
        maxHeatIncomeFromSingleCard = (short) Math.max(maxHeatIncomeFromSingleCard, engine(played, CardAction.HEAT_ENERGY_INCOME, energy));

        maxPlantsIncomeFromSingleCard = (byte) Math.max(maxPlantsIncomeFromSingleCard, engine(played, CardAction.PLANT_PLANT_INCOME, plant));
        maxPlantsIncomeFromSingleCard = (byte) Math.max(maxPlantsIncomeFromSingleCard, engine(played, CardAction.PLANT_MICROBE_INCOME, microbe));

        maxCardsIncomeFromSingleCard = Math.max(maxCardsIncomeFromSingleCard, engine(played, CardAction.CARD_SCIENCE_INCOME, science * (1f / 3f)));

        dto.maxMcIncome[playerIndex] = maxMcIncomeFromSingleCard;
        dto.maxHeatIncome[playerIndex] = maxHeatIncomeFromSingleCard;
        dto.maxPlantsIncome[playerIndex] = maxPlantsIncomeFromSingleCard;
        dto.maxCardsIncome[playerIndex] = maxCardsIncomeFromSingleCard;
    }

    private static float engine(
            Map<CardAction, Long> played,
            CardAction action,
            float value
    ) {
        boolean isPlayed = played.get(action) != null;

        if (isPlayed) {
            return played.get(action) * value;
        }
        return 0f;
    }

    public static IncomeCategory resolveIncomeCategory(
            Card card,
            PolicyRecord policyRecord,
            short forests
    ) {

        float mc = 0;
        float heat = 0;
        float plant = 0;
        float cards = 0;

        // ---- direct incomes ----
        List<Gain> incomes = card.getCardMetadata().getIncomes();

        for (Gain income : incomes) {
            switch (income.getType()) {
                case MC:
                    mc += income.getValue();
                    break;
                case HEAT:
                    heat += income.getValue();
                    break;
                case PLANT:
                    plant += income.getValue();
                    break;
                case CARD:
                    cards += income.getValue();
                    break;
            }
        }

        // ---- tag engines ----
        int earth = policyRecord.earthTags[0];
        int animal = policyRecord.animalTags[0];
        int plantTag = policyRecord.plantTags[0];
        int science = policyRecord.scienceTags[0];
        int building = policyRecord.buildingTags[0];
        int energy = policyRecord.energyTags[0];
        int space = policyRecord.spaceTags[0];
        int event = policyRecord.eventTags[0];
        int microbe = policyRecord.microbeTags[0];

        CardAction action = card.getCardMetadata().getCardAction();

        if (action != null) {
            switch (action) {

                case MC_ANIMAL_PLANT_INCOME:
                    mc += animal + plantTag;
                    break;

                case MC_SCIENCE_INCOME:
                    mc += science;
                    break;

                case MC_2_BUILDING_INCOME:
                    mc += building * 0.5f;
                    break;

                case MC_ENERGY_INCOME:
                    mc += energy;
                    break;

                case MC_SPACE_INCOME:
                    mc += space;
                    break;

                case MC_EVENT_INCOME:
                    mc += event;
                    break;

                case MC_FOREST_INCOME:
                    mc += forests;
                    break;

                case MC_EARTH_INCOME:
                    mc += earth;
                    break;

                case HEAT_EARTH_INCOME:
                    heat += earth;
                    break;

                case HEAT_SPACE_INCOME:
                    heat += space;
                    break;

                case HEAT_ENERGY_INCOME:
                    heat += energy;
                    break;

                case PLANT_PLANT_INCOME:
                    plant += plantTag;
                    break;

                case PLANT_MICROBE_INCOME:
                    plant += microbe;
                    break;

                case CARD_SCIENCE_INCOME:
                    cards += science / 3f;
                    break;
            }
        }

        // ---- choose max ----
        float max = Math.max(
                Math.max(mc, heat),
                Math.max(plant, cards)
        );

        if (max == 0) return IncomeCategory.NONE;
        if (max == mc) return IncomeCategory.MC;
        if (max == heat) return IncomeCategory.HEAT;
        if (max == plant) return IncomeCategory.PLANT;
        return IncomeCategory.CARD;
    }

}
