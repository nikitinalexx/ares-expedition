package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionRedCard;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
public class ImportedNutrients implements ExperimentExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ImportedNutrients(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Imported Nutrients")
                .description("Gain 4 plants. Add 4 microbes to ANOTHER card.")
                .bonuses(List.of(Gain.of(GainType.PLANT, 4)))
                .cardAction(CardAction.IMPORTED_NUTRIENTS)
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
            marsContext.getCardResourceService().addResources(player, microbeCard, 4);
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setPlants(player.getPlants() + 4);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EARTH, Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 14;
    }

}
