package com.terraforming.ares.cards.red;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.TerraformingService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 08.05.2022
 */
@RequiredArgsConstructor
@Getter
public class LargeConvoy implements BaseExpansionRedCard {
    private final int id;

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> input) {
        if (input.containsKey(InputFlag.LARGE_CONVOY_PICK_PLANT.getId())) {
            player.setPlants(player.getPlants() + 5);
            return;
        }

        List<Integer> animalsInput = input.get(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId());
        Integer animalsCardId = animalsInput.get(0);

        ProjectCard animalsCard = cardService.getProjectCard(animalsCardId);

        player.getCardResourcesCount().put(
                animalsCard.getClass(),
                player.getCardResourcesCount().get(animalsCard.getClass()) + 3
        );
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.revealOcean(marsContext.getGame(), marsContext.getPlayer());

        Deck deck = marsContext.getGame().getProjectsDeck().dealCards(2);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : deck.getCards()) {
            marsContext.getPlayer().getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(marsContext.getCardService().getProjectCard(card)));
        }

        return resultBuilder.build();
    }

    @Override
    public String description() {
        return "Flip an ocean tile. Draw 2 cards. Gain 5 plants or add 3 animals to ANY card.";
    }

    @Override
    public int getWinningPoints() {
        return 2;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 36;
    }

}
