package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.features.FeatureBlock;
import com.terraforming.ares.services.ai.advanced.features.hand.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CompleteHandEncoder {
    private final CardService cardService;
    private final SpecialEffectsService specialEffectsService;
    private final DraftCardsService draftCardsService;
    private final HandEncoderHelperService handEncoderHelperService;
    private final WinPointsService winPointsService;

    public static final List<FeatureBlock> GENERIC_HAND_FEATURES = List.of(
            new HandPlayerFeature(),
            new CardIncomeStatsFeature(),
            new CardGainStatsFeature(),
            new HandTagsFeature(),
            new PlayedTagsFeature(),
            new CorporationsHandFeature(),
            new CardRequirementsFeature(),
            new SingleParamHandFeature(),
            new MultiParamHandFeature(),
            new MicrobeFeature()
    );

    public void evaluate(MarsGame game, Player player, Player opponent, FeatureWriter featureWriter) {
        if (player.getHand().size() == 0) {
            return;
        }

        TableContext playerContext = new TableContext(
                game, player, opponent, cardService, winPointsService, draftCardsService, specialEffectsService, handEncoderHelperService
        );

        for (FeatureBlock handFeature : GENERIC_HAND_FEATURES) {
            handFeature.encode(playerContext, featureWriter);
        }
    }


}
