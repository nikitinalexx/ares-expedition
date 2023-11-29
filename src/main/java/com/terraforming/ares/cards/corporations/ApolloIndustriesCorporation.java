package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.UpgradePhaseHelper;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Getter
public class ApolloIndustriesCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ApolloIndustriesCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Apollo Industries")
                .description("33 Mc. Upgrade your phase 2 card. Effect: When you play a Science tag, draw a card.")
                .cardAction(CardAction.APOLLO_CORPORATION)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        final CardService cardService = marsContext.getCardService();

        final int scienceTagsCount = cardService.countCardTags(project, Set.of(Tag.SCIENCE), inputParams);

        marsContext.getPlayer().getHand().addCards(cardService.dealCards(marsContext.getGame(), scienceTagsCount));

        if (project.getClass() == ApolloIndustriesCorporation.class) {
            List<Integer> cardInput = inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId());

            final MarsGame game = marsContext.getGame();

            UpgradePhaseHelper.upgradePhase(marsContext.getPlayer(), cardInput.get(0));
        }
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(33);
        return null;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.APOLLO_INDUSTRIES);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SPACE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.DISCOVERY;
    }

    @Override
    public int getPrice() {
        return 33;
    }

}
