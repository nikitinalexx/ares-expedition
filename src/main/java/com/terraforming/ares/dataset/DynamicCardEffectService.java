package com.terraforming.ares.dataset;

import com.terraforming.ares.cards.CardMetadata;
import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.CardAction;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Service
public class DynamicCardEffectService {
    private final CardService cardService;

    private final List<CardAction> sortedCards;
    private final Map<CardAction, BiFunction<MarsGame, Player, Integer>> CARD_TO_EFFECT_FUNCTION;

    public DynamicCardEffectService(CardService cardService) {
        this.cardService = cardService;

        sortedCards = List.of(CardAction.CITY_COUNCIL, CardAction.COMMUNITY_AFFORESTATION, CardAction.GAS_COOLED_REACTORS);

        CARD_TO_EFFECT_FUNCTION = Map.of(
                CardAction.CITY_COUNCIL, this::getCityCouncilCardEffect,
                CardAction.COMMUNITY_AFFORESTATION, this::getCommunityAfforestationEffect,
                CardAction.GAS_COOLED_REACTORS, this::getGasCooledReactorsEffect
        );
    }

    public List<Integer> getAllDynamicEffects(MarsGame game, Player player) {
        return player.getPlayed().getCards().stream().map(cardService::getCard)
                .map(Card::getCardMetadata)
                .filter(Objects::nonNull)
                .map(CardMetadata::getCardAction)
                .filter(Objects::nonNull)
                .filter(sortedCards::contains)
                .map(cardAction -> CARD_TO_EFFECT_FUNCTION.get(cardAction).apply(game, player))
                .collect(Collectors.toList());
    }

    private Integer getCityCouncilCardEffect(MarsGame game, Player player) {
        int milestonesAchieved = (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();

        return milestonesAchieved + 1;
    }

    private Integer getCommunityAfforestationEffect(MarsGame game, Player player) {
        return (int) game.getMilestones().stream().filter(milestone -> milestone.isAchieved(player)).count();
    }

    private Integer getGasCooledReactorsEffect(MarsGame game, Player player) {
        return player.countPhaseUpgrades();
    }


}
