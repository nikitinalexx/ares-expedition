package com.terraforming.ares.cards.green;

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
 * Creation date 19.02.2023
 */
@RequiredArgsConstructor
@Getter
public class PoliticalInfluence implements DiscoveryExpansionGreenCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public PoliticalInfluence(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Political Influence")
                .description("Choose a tag and add to this card. During production phase, this produces 3 MC")
                .cardAction(CardAction.CHOOSE_TAG)
                .incomes(List.of(Gain.of(GainType.MC, 3)))
                .build();
    }

    @Override
    public void payAgain(MarsGame game, CardService cardService, Player player) {
        player.setMc(player.getMc() + 3);
    }

    @Override
    public TurnResponse buildProject(MarsContext marsContext) {
        final Player player = marsContext.getPlayer();

        player.setMcIncome(player.getMcIncome() + 3);

        return null;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> input) {
        List<Integer> tagInput = input.get(InputFlag.TAG_INPUT.getId());

        final Tag tag = Tag.byIndex(tagInput.get(0));

        marsContext.getPlayer().getCardToTag().put(PoliticalInfluence.class, tag);
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public boolean onBuiltEffectApplicableToOther() {
        return false;
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }


    @Override
    public List<Tag> getTags() {
        return List.of(Tag.DYNAMIC);
    }

    @Override
    public int getPrice() {
        return 10;
    }

}
