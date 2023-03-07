package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.dto.blueAction.AutoPickCardsAction;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.TerraformingService;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 17.02.2023
 */
@RequiredArgsConstructor
@Getter
public class OreLeaching implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public OreLeaching(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Ore Leaching")
                .description("Upgrade a phase card. Raise the temperature 2 steps. Draw two cards.")
                .cardAction(CardAction.UPDATE_PHASE_CARD)
                .bonuses(List.of(
                        Gain.of(GainType.TEMPERATURE, 2),
                        Gain.of(GainType.CARD, 2)
                ))
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        UpgradePhaseHelper.upgradePhase(marsContext.getCardService(), marsContext.getGame(), marsContext.getPlayer(), cardInput.get(0));
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        TerraformingService terraformingService = marsContext.getTerraformingService();

        terraformingService.increaseTemperature(marsContext);
        terraformingService.increaseTemperature(marsContext);

        AutoPickCardsAction.AutoPickCardsActionBuilder resultBuilder = AutoPickCardsAction.builder();

        for (Integer card : marsContext.getCardService().dealCards(marsContext.getGame(), 2)) {
            marsContext.getPlayer().getHand().addCard(card);
            resultBuilder.takenCard(CardDto.from(marsContext.getCardService().getCard(card)));
        }

        return resultBuilder.build();
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.JUPITER, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 35;
    }

}
