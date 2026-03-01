package com.terraforming.ares.services.policyai.encoder;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class PolicyTableAndHandEncoder {
    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;
    private final SpecialEffectsService specialEffectsService;

    private static final List<PolicyFeatureBlock> GENERIC_TABLE_FEATURES = List.of(
            new PolicyTerraformingFeature(),
            new PolicyMilestonesAwardsFeature(),
            new PolicyTableContextFeature(),
            new PolicyAllHandCardsFeature(),
            new PolicyTableRedCardsFeature(),
            new PolicyHandCorpCardsFeature()
    );

    private static final List<PlayerPolicyFeatureBlock> PLAYER_TABLE_FEATURES = List.of(
            new PolicyPhaseUpgradeFeature(),
            new PolicyPlayerIncomesFeature(),
            new PolicyPlayedTagsFeature(),
            new PolicyTableTagIncomeFeature(),
            new PolicyCorporationsFeature(),
            new PolicyTableBlueCardsFeature(),
            new PolicyTableGreenCardsFeature(),
            new PolicyTableResourcesFeature()
    );

    public TableContext encode(MarsGame game, Player player, Player opponent, PolicyRecord policyRecord) {
        policyRecord.isFirstPlayer = player.isFirstBot();

        TableContext playerContext = new TableContext(
                game, player, opponent, cardService, winPointsService, draftCardsService, specialEffectsService
        );
        TableContext opponentContext = new TableContext(
                game, opponent, player, cardService, winPointsService, draftCardsService, specialEffectsService
        );
        for (PolicyFeatureBlock genericTableFeature : GENERIC_TABLE_FEATURES) {
            genericTableFeature.encode(playerContext, policyRecord);
        }

        collectGenericPlayerData(playerContext, policyRecord, 0);
        collectGenericPlayerData(opponentContext, policyRecord, 1);

        return playerContext;
    }

    private void collectGenericPlayerData(TableContext context, PolicyRecord policyRecord, int playerIndex) {
        for (PlayerPolicyFeatureBlock playerTableFeature : PLAYER_TABLE_FEATURES) {
            playerTableFeature.encode(context, policyRecord, playerIndex);
        }
    }

}
