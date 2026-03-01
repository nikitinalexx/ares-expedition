package com.terraforming.ares.services.ai.advanced;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.*;
import com.terraforming.ares.services.CardService;
import com.terraforming.ares.services.DraftCardsService;
import com.terraforming.ares.services.SpecialEffectsService;
import com.terraforming.ares.services.WinPointsService;
import com.terraforming.ares.services.ai.AiConstants;
import lombok.Getter;

import java.util.*;

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
    private final long[] handMaskWords = new long[5];

    private final long tableRedCardsMask;
    private final int tableGreenCardsMask;

    public TableContext(
            MarsGame game,
            Player player,
            Player opponent,
            CardService cardService,
            WinPointsService winPointsService,
            DraftCardsService draftCardsService,
            SpecialEffectsService specialEffectsService
    ) {
        this.game = game;
        this.player = player;
        this.opponent = opponent;
        this.cardService = cardService;
        this.winPointsService = winPointsService;
        this.draftCardsService = draftCardsService;

        this.playedTagToCount = cardService.countPlayedTags(player);

        this.playedCardActions = new HashMap<>();
        this.playedCardClasses = new HashSet<>();
        this.playedCards = new ArrayList<>();

        List<Integer> playedRefs = player.getPlayed().getCards();

        long tableRedCardsMask = 0;

        int tableGreenCardsMask = 0;

        for (int ref : playedRefs) {
            Card card = cardService.getCard(ref);
            if (card == null) continue;

            playedCards.add(card);

            // class set
            playedCardClasses.add(card.getClass());

            // action counting
            CardAction action = card.getCardMetadata().getCardAction();
            if (action != null) {
                playedCardActions.merge(action, 1L, Long::sum);
            }

            if (card.getColor() == CardColor.RED) {
                Integer redCardIndex = AiConstants.RED_CARDS_INDEX_BY_CLASS.get(card.getClass());
                tableRedCardsMask |= (1L << (redCardIndex & 63));
            }

            if (card.getColor() == CardColor.GREEN) {
                Integer greenCardIndex = AiConstants.GREEN_CARDS_INDEX_BY_CLASS.get(card.getClass());
                if (greenCardIndex != null) {
                    tableGreenCardsMask |= (1 << greenCardIndex);
                }
            }
        }

        this.tableRedCardsMask = tableRedCardsMask;
        this.tableGreenCardsMask = tableGreenCardsMask;

        this.specialEffects = specialEffectsService.getPlayerSpecialEffects(player);

        // -------- hand mask --------
        Arrays.fill(handMaskWords, 0L);

        for (int cardRef : player.getHand().getCards()) {

            Card card = cardService.getCard(cardRef);
            if (card == null) continue;

            Integer index =
                    AiConstants.ALL_CARDS_INDEX_BY_CLASS
                            .get(card.getClass());

            if (index == null) continue;

            handMaskWords[index >>> 6] |= (1L << (index & 63));
        }

    }

}
