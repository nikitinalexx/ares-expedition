package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
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
public class ExocorpCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ExocorpCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Exocorp")
                .description("26 Mc. Upgrade your phase 5 card. Effect: Cards you discard for MC are worth an additional 1 MC.")
                .cardAction(CardAction.EXOCORP_CORPORATION)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> cardInput = inputParams.get(InputFlag.PHASE_UPGRADE_CARD.getId());

        final MarsGame game = marsContext.getGame();

        UpgradePhaseHelper.upgradePhase(marsContext.getPlayer(), cardInput.get(0));
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(26);
        return null;
    }

    @Override
    public Set<SpecialEffect> getSpecialEffects() {
        return Set.of(SpecialEffect.EXOCORP_SOLD_CARDS_COST_1_MC_MORE);
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.SCIENCE);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.DISCOVERY;
    }

    @Override
    public int getPrice() {
        return 26;
    }

}
