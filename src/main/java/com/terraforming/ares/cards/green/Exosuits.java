package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.income.Gain;
import com.terraforming.ares.model.income.GainType;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 20.02.2023
 */
@RequiredArgsConstructor
@Getter
public class Exosuits implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public Exosuits(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Exosuits")
                .description("Upgrade a phase card. Draw a card.")
                .cardAction(CardAction.UPDATE_PHASE_CARD)
                .bonuses(List.of(Gain.of(GainType.CARD, 1)))
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        final MarsGame game = marsContext.getGame();
        final Player player = marsContext.getPlayer();

        UpgradePhaseHelper.upgradePhase(player, cardInput.get(0));

        player.getHand().addCards(marsContext.getCardService().dealCards(marsContext.getGame(), 1));
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
    public List<Tag> getTags() {
        return List.of(Tag.SPACE, Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 12;
    }

}
