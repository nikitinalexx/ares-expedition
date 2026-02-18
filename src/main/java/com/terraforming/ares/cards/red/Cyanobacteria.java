package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.cards.green.ExperimentExpansionGreenCard;
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
public class Cyanobacteria implements ExperimentExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Cyanobacteria(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Cyanobacteria")
                .description("Raise Oxygen 1 step. Put microbe on another card for every revealed ocean.")
                .bonuses(List.of(Gain.of(GainType.OXYGEN, 1)))
                .cardAction(CardAction.CYANOBACTERIA)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> microbesInput = input.get(InputFlag.ADD_MICROBE.getId());
        Integer microbesCardId = microbesInput.getFirst();

        final Player player = marsContext.getPlayer();

        int revealedOceans = marsContext.getGame().getPlanetAtTheStartOfThePhase().getRevealedOceans().size();
        if (microbesCardId != InputFlag.SKIP_ACTION.getId() && revealedOceans > 0) {
            Card microbeCard = marsContext.getCardService().getCard(microbesCardId);
            marsContext.getCardResourceService().addResources(player, microbeCard, revealedOceans);
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        marsContext.getTerraformingService().raiseOxygen(marsContext);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.MICROBE);
    }

    @Override
    public int getPrice() {
        return 12;
    }

}
