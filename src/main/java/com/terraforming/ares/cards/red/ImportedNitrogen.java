package com.terraforming.ares.cards.red;

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
public class ImportedNitrogen implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ImportedNitrogen(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Imported Nitrogen")
                .description("Raise your TR 1 step. Gain 4 plants. Add 2 animals to ANOTHER card. Add 3 microbes to ANOTHER card.")
                .bonuses(List.of(Gain.of(GainType.TERRAFORMING_RATING, 1), Gain.of(GainType.PLANT, 4)))
                .cardAction(CardAction.IMPORTED_NITROGEN)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> animalsInput = input.get(InputFlag.IMPORTED_NITROGEN_ADD_ANIMALS.getId());
        List<Integer> microbesInput = input.get(InputFlag.IMPORTED_NITROGEN_ADD_MICROBES.getId());


        Integer animalsCardId = animalsInput.get(0);
        Integer microbesCardId = microbesInput.get(0);

        if (animalsCardId != InputFlag.SKIP_ACTION.getId()) {
            Card animalsCard = cardService.getCard(animalsCardId);
            player.addResources(animalsCard, 2);
        }

        if (microbesCardId != InputFlag.SKIP_ACTION.getId()) {
            Card microbeCard = cardService.getCard(microbesCardId);
            player.addResources(microbeCard, 3);
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setTerraformingRating(player.getTerraformingRating() + 1);
        player.setPlants(player.getPlants() + 4);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EARTH, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 20;
    }

}
