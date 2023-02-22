package com.terraforming.ares.cards.corporations;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 21.02.2023
 */
@Getter
public class ModproCorporation implements CorporationCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public ModproCorporation(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Modpro")
                .description("37 Mc. Choose a tag and add to this card. Action: reveal 4 cards from the deck and take those that match tag on your corporation.")
                .cardAction(CardAction.MODPRO_CORPORATION)
                .build();
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> tagInput = inputParams.get(InputFlag.TAG_INPUT.getId());

        final Tag tag = Tag.byIndex(tagInput.get(0));

        assert tag != null;

        marsContext.getPlayer().getCardToTag().put(ModproCorporation.class, List.of(tag));
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean isActiveCard() {
        return true;
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        Player player = marsContext.getPlayer();
        player.setMc(37);
        return null;
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.DYNAMIC);
    }

    @Override
    public Expansion getExpansion() {
        return Expansion.DISCOVERY;
    }

    @Override
    public int getPrice() {
        return 37;
    }

}
