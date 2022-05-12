package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
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
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void onProjectBuiltEffect(CardService cardService, MarsGame game, Player player, ProjectCard project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.ASTROFARM_PUT_RESOURCE.getId());

        Integer cardId = cardInput.get(0);

        if (cardId == InputFlag.SKIP_ACTION.getId()) {
            return;
        }

        ProjectCard inputCard = cardService.getProjectCard(cardId);

        player.getCardResourcesCount().put(
                inputCard.getClass(),
                player.getCardResourcesCount().get(inputCard.getClass()) + 2
        );
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return false;
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
