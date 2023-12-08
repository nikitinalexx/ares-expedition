package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class CommunicationCenter implements ExperimentExpansionBlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public CommunicationCenter(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Communication Center")
                .actionDescription("When anyone plays event, get 1 science. During phase 4, remove every 3 science and get 1 card for each triplet.")
                .description("Add 2 science to this card.")
                .cardAction(CardAction.COMMUNICATION_CENTER)
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.SCIENCE;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.initResources(this);
        marsContext.getCardResourceService().addResources(player, this, 2);

        player.setHeatIncome(player.getHeatIncome() - 1);

        return null;
    }

    @Override
    public void collectExtraIncome(MarsGame game, CardService cardService, Player player) {
        Integer resourcesCount = player.getCardResourcesCount().get(this.getClass());
        if (resourcesCount >= 3) {
            player.getHand().addCards(cardService.dealCards(game, resourcesCount / 3));
            resourcesCount = resourcesCount % 3;
        }
        player.getCardResourcesCount().put(this.getClass(), resourcesCount);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToAllPlayers() {
        return true;
    }

    @Override
    public void postProjectBuiltEffectForAll(MarsContext marsContext, Card projectThatIsBuild, Player anotherPlayer, Map<Integer, List<Integer>> inputParams) {
        if (marsContext.getCardService().countCardTags(projectThatIsBuild, Set.of(Tag.EVENT), inputParams) > 0) {
            marsContext.getCardResourceService().addResources(
                    anotherPlayer, this, 1
            );
        }
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE, Tag.BUILDING);
    }

    @Override
    public int getPrice() {
        return 8;
    }
}
