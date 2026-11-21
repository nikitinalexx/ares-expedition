package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        if (cardInput == null || cardInput.isEmpty()) {
            return;
        }

        int targetCardId = cardInput.get(0);
        // The AI's own build-params service answers SKIP_ACTION when there is no red card to return (AiCardBuildParamsService),
        // and the projection path builds cards without running the validator. getCard(SKIP) is null, and countCardTagsWithDynamic
        // then threw `Cannot invoke Card.getTags() because "card" is null` - which killed a whole 2 000-game measurement at game 1 408.
        if (targetCardId == InputFlag.SKIP_ACTION.getId()) {
            return;
        }

        Card targetCard = marsContext.getCardService().getCard(targetCardId);
        if (targetCard == null) {
            return; // an id that is not in the catalog has no tags to revert
        }

        final Player player = marsContext.getPlayer();

        player.getPlayed().removeCards(List.of(targetCardId));
        player.getHand().addCard(targetCardId);


        player.getPlayed().getCards().stream().map(marsContext.getCardService()::getCard).filter(Objects::nonNull).forEach(
                card -> card.revertPlayedTags(marsContext.getCardService(), targetCard, player)
        );

        player.getCardToTag().remove(targetCard.getClass());
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
