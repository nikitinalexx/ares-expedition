package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.build.PutResourceOnBuild;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
public class Astrofarm implements BaseExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Astrofarm(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Astrofarm")
                .description("Add 2 microbes to ANOTHER card. During the production phase, this produces 1 plant and 3 heat.")
                .incomes(List.of(
                        Gain.of(GainType.PLANT, 1),
                        Gain.of(GainType.HEAT, 3)
                ))
                .cardAction(CardAction.ASTROFARM)
                .resourceOnBuild(
                        PutResourceOnBuild.builder()
                                .type(CardCollectableResource.MICROBE)
                                .count(2)
                                .paramId(InputFlag.ASTROFARM_PUT_RESOURCE.getId())
                                .build()
                )
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setPlants(player.getPlants() + 1);
        player.setHeat(player.getHeat() + 3);
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.ASTROFARM_PUT_RESOURCE.getId());

        Integer cardId = cardInput.get(0);

        if (cardId == InputFlag.SKIP_ACTION.getId()) {
            return;
        }

        Card inputCard = marsContext.getCardService().getCard(cardId);

        marsContext.getPlayer().addResources(inputCard, 2);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();

        player.setPlantsIncome(player.getPlantsIncome() + 1);
        player.setHeatIncome(player.getHeatIncome() + 3);

        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public int getPrice() {
        return 21;
    }
}
