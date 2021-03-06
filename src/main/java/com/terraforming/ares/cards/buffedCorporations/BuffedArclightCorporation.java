package com.terraforming.ares.cards.buffedCorporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.model.winPoints.WinPointsInfo;
import com.terraforming.ares.services.CardService;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 27.04.2022
 */
@Getter
public class BuffedArclightCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    @Override
    public CardCollectableResource getCollectableResource() {
        return CardCollectableResource.ANIMAL;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.PLANT, Tag.ANIMAL);
    }

    public BuffedArclightCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Arclight")
                .description("46 Mc. When you play an Animal/Plant/Microbe tag including this, add an Animal to this card. 1 vp for 2 animals on this card.")
                .cardAction(CardAction.ARCLIGHT_CORPORATION)
                .winPointsInfo(
                        WinPointsInfo.builder()
                                .type(CardCollectableResource.ANIMAL)
                                .resources(2)
                                .points(1)
                                .build()
                )
                .build();
    }

    @Override
    public boolean isActiveCard() {
        return false;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(CardService cardService, MarsGame marsGame, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        int affectedTagsCount = (int) project.getTags().stream().filter(tag ->
                tag == Tag.ANIMAL || tag == Tag.PLANT || tag == Tag.MICROBE
        ).count();

        player.addResources(this, affectedTagsCount);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(46);
        player.initResources(this);
        player.addResources(this, 2);
        return null;
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.BUFFED_CORPORATION;
    }

    @Override
    public int getPrice() {
        return 46;
    }
}
