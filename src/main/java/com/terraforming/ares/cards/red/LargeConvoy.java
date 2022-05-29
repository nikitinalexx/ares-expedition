package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
    private final CardMetadata cardMetadata;

    public LargeConvoy(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Large Convoy")
                .description("Flip an ocean tile. Draw 2 cards. Gain 5 plants or add 3 animals to ANY card.")
                .bonuses(List.of(Gain.of(GainType.OCEAN, 1), Gain.of(GainType.CARD, 2)))
                .cardAction(CardAction.LARGE_CONVOY)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> input) {
        if (input.containsKey(InputFlag.LARGE_CONVOY_PICK_PLANT.getId())) {
            player.setPlants(player.getPlants() + 5);
            return;
        }

        List<Integer> animalsInput = input.get(InputFlag.LARGE_CONVOY_ADD_ANIMAL.getId());
        Integer animalsCardId = animalsInput.get(0);

        Card animalsCard = cardService.getCard(animalsCardId);

        player.addResources(animalsCard, 3);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();
        MarsGame game = marsContext.getGame();

        terraformingService.revealOcean(game, marsContext.getPlayer());

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : marsContext.getCardService().dealCards(game, 2)) {
            marsContext.getPlayer().getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(marsContext.getCardService().getCard(card)));
        }

        return resultBuilder.build();
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
