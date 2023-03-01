package com.terraforming.ares.cards.green;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
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
public class MartianStudiesScholarship implements DiscoveryExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public MartianStudiesScholarship(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Martian Studies")
                .description("Upgrade a phase 2 card. Gain the \"Bonus\" this phase.")
                .cardAction(CardAction.UPDATE_PHASE_2_CARD)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> cardInput = input.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        final MarsGame game = marsContext.getGame();
        final Player player = marsContext.getPlayer();

        UpgradePhaseHelper.upgradePhase(marsContext.getCardService(), game, player, cardInput.get(0));

        if (player.hasPhaseUpgrade(Constants.PHASE_2_UPGRADE_PROJECT_AND_CARD)) {
            player.getBuilds().add(new BuildDto(BuildType.BLUE_RED_OR_CARD));
        } else {
            player.getBuilds().add(new BuildDto(BuildType.BLUE_RED_OR_MC));
        }
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
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 11;
    }

}
