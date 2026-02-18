package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import com.terraforming.ares.services.ai.advanced.features.table.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CompleteTableEncoder {
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;
    private final SpecialEffectsService specialEffectsService;

    private static final List<FeatureBlock> GENERIC_TABLE_FEATURES = List.of(
            new TerraformingFeature(),
            new MilestonesAwardsFeature()
            //total 61
    );

    private static final List<FeatureBlock> PLAYER_TABLE_FEATURES = List.of(
            new PhaseUpgradeFeature(),
            new PlayerIncomesFeature(),
            new PlayedTagsFeature(),
            new TableTagIncomeFeature(),
            new CorporationsFeature(),
            new TableCardEffectsFeature(),
            new TableResourcesFeature()
            //total=178

    );

    public void encodeTable(MarsGame game, Player player, Player opponent, FeatureWriter featureWriter) {
        TableContext playerContext = new TableContext(
                game, player, opponent, cardService, winPointsService, draftCardsService, specialEffectsService
        );
        TableContext opponentContext = new TableContext(
                game, opponent, player, cardService, winPointsService, draftCardsService, specialEffectsService
        );
        for (FeatureBlock genericTableFeature : GENERIC_TABLE_FEATURES) {
            genericTableFeature.encode(playerContext, featureWriter);
        }

        collectGenericPlayerData(playerContext, featureWriter);
        collectGenericPlayerData(opponentContext, featureWriter);
    }

    private void collectGenericPlayerData(TableContext context, FeatureWriter featureWriter) {
        for (FeatureBlock playerTableFeature : PLAYER_TABLE_FEATURES) {
            playerTableFeature.encode(context, featureWriter);
        }
    }

    public static List<String> getAllFeatureNames() {
        List<String> finalResult = new ArrayList<>();
        for (FeatureBlock genericTableFeature : GENERIC_TABLE_FEATURES) {
            finalResult.addAll(genericTableFeature.getFeatureNames());
        }
        for (FeatureBlock genericTableFeature : PLAYER_TABLE_FEATURES) {
            finalResult.addAll(genericTableFeature.getFeatureNames());
        }

        for (FeatureBlock genericTableFeature : PLAYER_TABLE_FEATURES) {
            List<String> opponentFeatureNames = genericTableFeature.getFeatureNames();
            List<String> opponentNames = new ArrayList<>();
            for (String opponentFeatureName : opponentFeatureNames) {
                opponentNames.add("op_" + opponentFeatureName);
            }
            finalResult.addAll(opponentNames);
        }

        return finalResult;
    }

}
