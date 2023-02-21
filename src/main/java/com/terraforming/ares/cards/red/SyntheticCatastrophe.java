package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
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
public class SyntheticCatastrophe implements BaseExpansionRedCard {
    private final int id;
    private final CardMetadata cardMetadata;

    public SyntheticCatastrophe(int id) {
        this.id = id;
        this.cardMetadata = CardMetadata.builder()
                .name("Synthetic Catastrophe")
                .description("You may return another red card you have in play to your hand.")
                .cardAction(CardAction.SYNTHETIC_CATASTROPHE)
                .build();
    }

    @Override
    public CardMetadata getCardMetadata() {
        return cardMetadata;
    }

    @Override
    public boolean onBuiltEffectApplicableToItself() {
        return true;
    }

    @Override
    public void postProjectBuiltEffect(MarsContext marsContext, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> cardInput = inputParams.get(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId());

        int targetCardId = cardInput.get(0);

        Card targetCard = marsContext.getCardService().getCard(targetCardId);
        final Player player = marsContext.getPlayer();

        player.getPlayed().removeCards(List.of(targetCardId));
        player.getHand().addCard(targetCardId);


        player.getPlayed().getCards().stream().map(marsContext.getCardService()::getCard).forEach(
                card -> card.revertPlayedTags(marsContext.getCardService(), targetCard, player)
        );
    }

    @Override
    public List<Tag> getTags() {
        return List.of(Tag.EVENT);
    }

    @Override
    public int getPrice() {
        return 0;
    }

}
