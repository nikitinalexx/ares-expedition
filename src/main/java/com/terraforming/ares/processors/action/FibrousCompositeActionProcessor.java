package com.terraforming.ares.processors.action;

import com.terraforming.ares.cards.blue.FibrousCompositeMaterial;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.InputFlag;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.services.CardResourceService;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 19.02.2022
 */
@Component
@RequiredArgsConstructor
public class FibrousCompositeActionProcessor implements BlueActionCardProcessor<FibrousCompositeMaterial> {
    private final CardService cardService;
    private final CardResourceService cardResourceService = new CardResourceService();

    @Override
    public Class<FibrousCompositeMaterial> getType() {
        return FibrousCompositeMaterial.class;
    }

    @Override
    public TurnResponse process(MarsGame game, Player player, Card actionCard, Map<Integer, List<Integer>> inputParameters) {
        final List<Integer> addDiscardInput = inputParameters.get(InputFlag.ADD_DISCARD_MICROBE.getId());

        final Integer input = addDiscardInput.get(0);

        if (input == 1) {
//            player.getCardResourcesCount().put(
//                    FibrousCompositeMaterial.class,
//                    player.getCardResourcesCount().get(FibrousCompositeMaterial.class) + 1
//            );
            cardResourceService.addResources(player,FibrousCompositeMaterial.class,player.getCardResourcesCount().get(FibrousCompositeMaterial.class) + 1);
        } else if (input == 3) {
            UpgradePhaseHelper.upgradePhase(cardService, game, player, inputParameters.get(InputFlag.PHASE_UPGRADE_CARD.getId()).get(0));
            player.getCardResourcesCount().put(
                    FibrousCompositeMaterial.class,
                    player.getCardResourcesCount().get(FibrousCompositeMaterial.class) - 3
            );
        } else {
            throw new IllegalArgumentException("Unexpected error");
        }

        return null;
    }
}
