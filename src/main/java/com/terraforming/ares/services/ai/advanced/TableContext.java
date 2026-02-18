package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.model.SpecialEffect;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.terraforming.ares.services.ai.advanced.features.hand.AllHandCardsFeature.ALLCARDS_ORDER;
import static com.terraforming.ares.services.ai.advanced.features.hand.AllHandCardsFeature.INDEX_BY_CLASS;

@Getter
public class TableContext {
    private final MarsGame game;
    private final Player player;
    private final Player opponent;

    private final CardService cardService;
    private final WinPointsService winPointsService;
    private final DraftCardsService draftCardsService;

    private final int[] playedTagToCount;
    private final Map<CardAction, Long> playedCardActions;
    private final Set<Class<?>> playedCardClasses;
    private final Set<SpecialEffect> specialEffects;

    private final List<Card> playedCards;
    private final BitSet handMask;


    public TableContext(MarsGame game, Player player, Player opponent, CardService cardService, WinPointsService winPointsService, DraftCardsService draftCardsService, SpecialEffectsService specialEffectsService) {
        this.game = game;
        this.player = player;
        this.opponent = opponent;
        this.cardService = cardService;
        this.winPointsService = winPointsService;
        this.draftCardsService = draftCardsService;
        this.playedTagToCount = cardService.countPlayedTags(player);

        this.playedCardActions = player.getPlayed().getCards().stream()
                .map(cardService::getCard)
                .map(card -> card.getCardMetadata().getCardAction())
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()
                ));


        this.playedCardClasses = player.getPlayed().getCards().stream().map(cardService::getCard).map(Card::getClass).collect(Collectors.toSet());
        this.specialEffects = specialEffectsService.getPlayerSpecialEffects(player);

        this.playedCards = player.getPlayed().getCards().stream().map(cardService::getCard).collect(Collectors.toList());

        BitSet mask = new BitSet(ALLCARDS_ORDER.size());

        for (int cardRef : player.getHand().getCards()) {
            Card card = cardService.getCard(cardRef);
            if (card == null) continue;

            Integer index = INDEX_BY_CLASS.get(card.getClass());
            if (index != null) {
                mask.set(index);
            }
        }

        this.handMask = mask;
    }

}
