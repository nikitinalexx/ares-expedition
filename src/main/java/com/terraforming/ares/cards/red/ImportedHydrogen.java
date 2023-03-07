package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
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
public class ImportedHydrogen implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ImportedHydrogen(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Imported Hydrogen")
                .description("Flip an ocean tile. Gain 3 plants, or add 3 microbes or 2 animals to ANOTHER card.")
                .bonuses(List.of(Gain.of(GainType.OCEAN, 1)))
                .cardAction(CardAction.IMPORTED_HYDROGEN)
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
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.revealOcean(marsContext);

        return null;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        final Player player = marsContext.getPlayer();

        if (input.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId())) {
            player.setPlants(player.getPlants() + 3);
            return;
        }

        int inputCardId = input.get(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId()).get(0);

        Card inputCard = marsContext.getCardService().getCard(inputCardId);
        int resourcesToAdd = 2;
        if (inputCard.getCollectableResource() == CardCollectableResource.MICROBE) {
            resourcesToAdd = 3;
        }

        marsContext.getCardResourceService().addResources(player, inputCard, resourcesToAdd);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 17;
    }

}
