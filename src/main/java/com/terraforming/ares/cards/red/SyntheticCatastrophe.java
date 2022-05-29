package com.terraforming.ares.cards.red;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
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
    public void postProjectBuiltEffect(CardService cardService, MarsGame game, Player player, Card project, Map<Integer, List<Integer>> inputParams) {
        List<Integer> cardInput = inputParams.get(InputFlag.SYNTHETIC_CATASTROPHE_CARD.getId());

        int targetCardId = cardInput.get(0);

        Card targetCard = cardService.getCard(targetCardId);
        player.getPlayed().removeCards(List.of(targetCardId));
        player.getHand().addCard(targetCardId);


        player.getPlayed().getCards().stream().map(cardService::getCard).forEach(
                card -> card.revertPlayedTags(cardService, targetCard.getTags(), player)
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
