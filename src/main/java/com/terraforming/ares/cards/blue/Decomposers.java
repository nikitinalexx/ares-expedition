package com.terraforming.ares.cards.blue;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_CARD;
import static com.terraforming.ares.model.InputFlag.DECOMPOSERS_TAKE_MICROBE;

/**
 * Created by oleksii.nikitin
 * Creation date 06.05.2022
 */
@RequiredArgsConstructor
@Getter
public class Decomposers implements BlueCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Decomposers(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Decomposers")
                .description("Requires red oxygen or higher. When you play an Animal, Microbe, or Plant, including this, add a microbe here or remove a microbe from here to draw a card.")
                .cardAction(CardAction.DECOMPOSERS)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getPlayer().getCardResourcesCount().put(Decomposers.class, 1);
        return null;
    }

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.MICROBE;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BASE;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public List<ParameterColor> getOxygenRequirement() {
        return List.of(ParameterColor.RED, ParameterColor.YELLOW, ParameterColor.WHITE);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return Collections.singletonList(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 7;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard card, Map<Integer, List<Integer>> inputParams) {
        if (!card.getTags().contains(Tag.ANIMAL) &&
                !card.getTags().contains(Tag.MICROBE) &&
                !card.getTags().contains(Tag.PLANT)) {
            return;
        }

        if (inputParams.containsKey(DECOMPOSERS_TAKE_MICROBE.getId()) && !CollectionUtils.isEmpty(inputParams.get(DECOMPOSERS_TAKE_MICROBE.getId()))) {
            player.getCardResourcesCount().put(
                    Decomposers.class,
                    player.getCardResourcesCount().get(Decomposers.class) + inputParams.get(DECOMPOSERS_TAKE_MICROBE.getId()).get(0)
            );
        }

        if (inputParams.containsKey(DECOMPOSERS_TAKE_CARD.getId()) && !CollectionUtils.isEmpty(inputParams.get(DECOMPOSERS_TAKE_CARD.getId()))) {
            Integer takeCardsCount = inputParams.get(DECOMPOSERS_TAKE_CARD.getId()).get(0);
            player.getCardResourcesCount().put(
                    Decomposers.class,
                    player.getCardResourcesCount().get(Decomposers.class) - takeCardsCount
            );

            for (Integer cardId : game.dealCards(takeCardsCount)) {
                player.getHand().addCard(cardId);
            }
        }
    }
}
