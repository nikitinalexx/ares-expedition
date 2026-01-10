package com.terraforming.ares.services.ai.advanced.features.hand;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.services.ai.advanced.FeatureWriter;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;

import java.util.List;

public class CardIncomeStatsFeature implements FeatureBlock {
    @Override
    public int size() {
        return 29;
    }

    public static final List<String> FEATURE_NAMES = List.of(
            // --- COST ---
            "cost_sum",
            "cost_mean",
            "cost_max",
            "cost_min",
            "cost_std",

            // --- VP ---
            "vp_sum",
            "vp_mean",
            "vp_max",
            "vp_std",
            "vp_non_zero_count",
            "vp_negative_penalty",

            // --- MC INCOME ---
            "mc_income_sum",
            "mc_income_max",
            "mc_income_count",

            // --- HEAT INCOME ---
            "heat_income_sum",
            "heat_income_max",
            "heat_income_count",

            // --- PLANT INCOME ---
            "plant_income_sum",
            "plant_income_max",
            "plant_income_count",

            // --- CARD INCOME ---
            "card_income_sum",
            "card_income_max",
            "card_income_count",

            // --- STEEL INCOME ---
            "steel_income_sum",
            "steel_income_max",
            "steel_income_count",

            // --- TITANIUM INCOME ---
            "titanium_income_sum",
            "titanium_income_max",
            "titanium_income_count"
    );

    @Override
    public void encode(TableContext ctx, FeatureWriter out) {
        List<Card> hand = ctx.getHand();
        int size = hand.size();

        // --- COST ---
        float costSum = 0;
        float costMin = 500;
        float costMax = 0;
        float costSqSum = 0;

        // --- VP ---
        float vpSum = 0;
        float vpMax = 0;
        float vpSqSum = 0;
        int vpCount = 0;
        int vpNonZero = 0;
        int negativeVpPenalty = 0;

        // --- INCOMES ---
        int mcSum = 0, mcMax = 0, mcCount = 0;
        int heatSum = 0, heatMax = 0, heatCount = 0;
        int plantSum = 0, plantMax = 0, plantCount = 0;
        int cardSum = 0, cardMax = 0, cardCount = 0;
        int steelSum = 0, steelMax = 0, steelCount = 0;
        int titaniumSum = 0, titaniumMax = 0, titaniumCount = 0;

        for (Card c : hand) {
            // --- COST ---
            float cost = c.getPrice();
            costSum += cost;
            costSqSum += cost * cost;
            if (cost < costMin) costMin = cost;
            if (cost > costMax) costMax = cost;

            // --- VP ---
            int vp = c.getWinningPoints();
            if (vp > 0) {
                vpSum += vp;
                vpSqSum += vp * vp;
                vpCount++;
                vpNonZero++;
                if (vp > vpMax) vpMax = vp;
            } else if (vp < 0) {
                negativeVpPenalty -= vp;
            }

            // --- INCOMES ---
            List<Gain> incomes = c.getCardMetadata().getIncomes();
            if (incomes == null) continue;

            for (Gain g : incomes) {
                int v = g.getValue();
                switch (g.getType()) {
                    case MC -> {
                        mcSum += v;
                        if (v > mcMax) mcMax = v;
                        mcCount++;
                    }
                    case HEAT -> {
                        heatSum += v;
                        if (v > heatMax) heatMax = v;
                        heatCount++;
                    }
                    case PLANT -> {
                        plantSum += v;
                        if (v > plantMax) plantMax = v;
                        plantCount++;
                    }
                    case CARD -> {
                        cardSum += v;
                        if (v > cardMax) cardMax = v;
                        cardCount++;
                    }
                    case STEEL -> {
                        steelSum += v;
                        if (v > steelMax) steelMax = v;
                        steelCount++;
                    }
                    case TITANIUM -> {
                        titaniumSum += v;
                        if (v > titaniumMax) titaniumMax = v;
                        titaniumCount++;
                    }
                }
            }
        }

        // --- WRITE COST ---
        float costMean = costSum / size;
        float costStd = (float) Math.sqrt(costSqSum / size - costMean * costMean);

        out.write(costSum);
        out.write(costMean);
        out.write(costMax);
        out.write(costMin);
        out.write(costStd);

        // --- WRITE VP ---
        float vpMean = vpCount == 0 ? 0 : vpSum / vpCount;
        float vpStd = vpCount == 0 ? 0 : (float) Math.sqrt(vpSqSum / vpCount - vpMean * vpMean);

        out.write(vpSum);
        out.write(vpMean);
        out.write(vpMax);
        out.write(vpStd);
        out.write(vpNonZero);
        out.write(negativeVpPenalty);

        // --- WRITE INCOMES ---
        out.write(mcSum);
        out.write(mcMax);
        out.write(mcCount);

        out.write(heatSum);
        out.write(heatMax);
        out.write(heatCount);

        out.write(plantSum);
        out.write(plantMax);
        out.write(plantCount);

        out.write(cardSum);
        out.write(cardMax);
        out.write(cardCount);

        out.write(steelSum);
        out.write(steelMax);
        out.write(steelCount);

        out.write(titaniumSum);
        out.write(titaniumMax);
        out.write(titaniumCount);
    }

    @Override
    public List<String> getFeatureNames() {
        return FEATURE_NAMES;
    }

}
