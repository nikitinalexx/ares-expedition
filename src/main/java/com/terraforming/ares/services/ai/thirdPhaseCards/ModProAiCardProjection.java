package com.terraforming.ares.services.ai.thirdPhaseCards;

import com.terraforming.ares.cards.corporations.ModproCorporation;
import com.terraforming.ares.dataset.MarsGameRowDifference;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Constants;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.Tag;
import com.terraforming.ares.services.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ModProAiCardProjection<T extends Card> implements AiCardProjection<ModproCorporation> {
    private final CardService cardService;

    @Override
    public Class<ModproCorporation> getType() {
        return ModproCorporation.class;
    }

    @Override
    public MarsGameRowDifference project(MarsGameRowDifference initialDifference, MarsGame game, Player player, Card card, int network) {
        Tag selectedTag = player.getCardToTag().get(ModproCorporation.class).get(0);

        LinkedList<Integer> hand = player.getHand().getCards();
        LinkedList<Integer> played = player.getPlayed().getCards();

        long alreadyUsedCards = Stream
                .concat(
                        Stream.concat(hand.stream(), played.stream()),
                        game.getPlayerUuidToPlayer().values().stream().filter(p -> !p.getUuid().equals(player.getUuid())).flatMap(p -> p.getPlayed().getCards().stream())
                )
                .map(cardService::getCard)
                .filter(c -> c.getTags().contains(Tag.DYNAMIC) || c.getTags().contains(selectedTag))
                .count();

        float cardsIncome = (float) (Constants.TAG_TO_CARDS_COUNT.get(selectedTag) + Constants.CARDS_WITH_DYNAMIC_TAG - alreadyUsedCards) * 4 / Constants.TOTAL_CARDS_COUNT;

        //TODO take into account that tags are not equally distributed
        return MarsGameRowDifference.builder()
                .greenCards(cardsIncome * Constants.GREEN_CARDS_RATIO)
                .redCards(cardsIncome * Constants.RED_CARDS_RATIO)
                .blueCards(cardsIncome * Constants.BLUE_CARDS_RATIO)
                .build();
    }

}
