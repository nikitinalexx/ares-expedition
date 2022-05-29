package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
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

        terraformingService.revealOcean(marsContext.getGame(), marsContext.getPlayer());

        return null;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> input) {
        if (input.containsKey(InputFlag.IMPORTED_HYDROGEN_PICK_PLANT.getId())) {
            player.setPlants(player.getPlants() + 3);
            return;
        }

        int inputCardId = input.get(InputFlag.IMPORTED_HYDROGEN_PUT_RESOURCE.getId()).get(0);

        Card inputCard = cardService.getCard(inputCardId);
        int resourcedToAdd = 2;
        if (inputCard.getCollectableResource() == CardCollectableResource.MICROBE) {
            resourcedToAdd = 3;
        }

        player.addResources(inputCard, resourcedToAdd);
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
