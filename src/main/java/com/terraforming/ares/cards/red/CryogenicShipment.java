package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.DiscoveryExpansionRedCard;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@RequiredArgsConstructor
@Getter
public class CryogenicShipment implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CryogenicShipment(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Cryogenic Shipment")
                .description("Upgrade a Phase card. Add 3 microbes or 2 animals to ANOTHER card.")
                .cardAction(CardAction.CRYOGENIC_SHIPMENT)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        final Player player = marsContext.getPlayer();

        int inputCardId = input.get(InputFlag.CRYOGENIC_SHIPMENT_PUT_RESOURCE.getId()).get(0);

        if (inputCardId != InputFlag.SKIP_ACTION.getId()) {
            Card inputCard = marsContext.getCardService().getCard(inputCardId);
            int resourcedToAdd = 2;
            if (inputCard.getCollectableResource() == CardCollectableResource.MICROBE) {
                resourcedToAdd = 3;
            }

            player.addResources(inputCard, resourcedToAdd);
        }

        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        final MarsGame game = marsContext.getGame();

        UpgradePhaseHelper.upgradePhase(marsContext.getCardService(), game, player, cardInput.get(0));
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 12;
    }

}
