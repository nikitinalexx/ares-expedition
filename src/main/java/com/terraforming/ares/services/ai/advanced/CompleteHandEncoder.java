package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import com.terraforming.ares.services.ai.advanced.features.hand.*;
import com.terraforming.ares.services.ai.advanced.features.table.MilestonesAwardsFeature;
import com.terraforming.ares.services.ai.advanced.features.table.TableCardEffectsFeature;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class CompleteHandEncoder {
    private final CardService cardService;
    private final SpecialEffectsService specialEffectsService;
    private final DraftCardsService draftCardsService;
    private final WinPointsService winPointsService;

    public static final List<FeatureBlock> GENERIC_HAND_FEATURES = List.of(
            new MarsAndPlayerIncomesFeature(),
            new MilestonesAwardsFeature(),
            new PlayedTagsFeature(),
            new CorporationsHandFeature(),
            new AllHandCardsFeature(),
            new HandFromTableTagIncomeFeature(),
            new TableCardEffectsFeature()
    );

    public void encodeHand(MarsGame game, Player player, Player opponent, FeatureWriter featureWriter) {
        TableContext playerContext = new TableContext(
                game, player, opponent, cardService, winPointsService, draftCardsService, specialEffectsService
        );

        for (FeatureBlock genericTableFeature : GENERIC_HAND_FEATURES) {
            genericTableFeature.encode(playerContext, featureWriter);
        }
    }

    public static List<String> getAllFeatureNames() {
        List<String> finalResult = new ArrayList<>();
        for (FeatureBlock genericTableFeature : GENERIC_HAND_FEATURES) {
            finalResult.addAll(genericTableFeature.getFeatureNames());
        }

        return finalResult;
    }


}
