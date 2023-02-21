package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.ParameterColor;
import com.terraforming.ares.services.CardService;
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
public class EosChasmaNationalPark implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public EosChasmaNationalPark(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Eos Chasma National Park")
                .description("Requires red temperature or warmer. Add 1 animal to ANOTHER card and gain 3 plants. During the production phase, this produces 2 MC.")
                .incomes(List.of(Gain.of(GainType.MC, 2)))
                .bonuses(List.of(Gain.of(GainType.PLANT, 3)))
                .cardAction(CardAction.EOS_CHASMA)
                .resourceOnBuild(
                        PutResourceOnBuild.builder()
                                .type(CardCollectableResource.ANIMAL)
                                .count(1)
                                .paramId(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId())
                                .build()
                )
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + 2);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.EOS_CHASMA_PUT_RESOURCE.getId());


        Integer cardId = cardInput.get(0);

        if (cardId == InputFlag.SKIP_ACTION.getId()) {
            return;
        }

        Card inputCard = marsContext.getCardService().getCard(cardId);

        marsContext.getPlayer().addResources(inputCard, 1);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlants(player.getPlants() + 3);

        return null;
    }

    @Override
    public List<ParameterColor> getTemperatureRequirement() {
        return List.of(ParameterColor.R, ParameterColor.Y, ParameterColor.W);
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.BUILDING, Tag.PLANT);
    }

    @Override
    public int getPrice() {
        return 16;
    }
}
