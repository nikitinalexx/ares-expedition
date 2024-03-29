package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionRedCard;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.model.parameters.OceanRequirement;
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
public class ControlledBloom implements ExperimentExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ControlledBloom(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Controlled Bloom")
                .description("Gain 3 plants. Add 3 microbes to ANOTHER card.")
                .bonuses(List.of(Gain.of(GainType.PLANT, 3)))
                .cardAction(CardAction.CONTROLLED_BLOOM)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> microbesInput = input.get(InputFlag.ADD_MICROBE.getId());
        Integer microbesCardId = microbesInput.get(0);

        final Player player = marsContext.getPlayer();

        if (microbesCardId != InputFlag.SKIP_ACTION.getId()) {
            Card microbeCard = marsContext.getCardService().getCard(microbesCardId);
            marsContext.getCardResourceService().addResources(player, microbeCard, 3);
        }
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
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE, Tag.PLANT, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 13;
    }


    @Override
    public OceanRequirement getOceanRequirement() {
        return OceanRequirement.builder().minValue(3).maxValue(Constants.MAX_OCEANS).build();
    }

    @Override
    public int getWinningPoints() {
        return 1;
    }

}
