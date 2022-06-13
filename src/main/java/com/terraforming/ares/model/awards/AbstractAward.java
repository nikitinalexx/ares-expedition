package com.terraforming.ares.model.awards;

import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.CardService;

import java.util.Collection;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
public abstract class AbstractAward extends BaseAward {

    @Override
    protected Set<Player> getFirstPlaceWinners(Collection<Player> players, CardService cardService) {
        int firstPlaceIncome = getFirstPlaceIncome(players, cardService);

        return getPlayersByValue(players, firstPlaceIncome, cardService);
    }

    @Override
    protected Set<Player> getSecondPlaceWinners(Collection<Player> players, CardService cardService) {
        int firstPlaceValue = getFirstPlaceIncome(players, cardService);
        int secondPlaceValue = getSecondPlaceIncome(players, firstPlaceValue, cardService);
        return getPlayersByValue(players, secondPlaceValue, cardService);
    }

    private int getSecondPlaceIncome(Collection<Player> players, int firstPlaceValue, CardService cardService) {
        ToIntFunction<Player> paramExtractor = comparableParamExtractor(cardService);
        return players.stream().mapToInt(paramExtractor).filter(income -> income < firstPlaceValue).max().orElse(0);
    }

    private int getFirstPlaceIncome(Collection<Player> players, CardService cardService) {
        ToIntFunction<Player> paramExtractor = comparableParamExtractor(cardService);

        return players.stream().mapToInt(paramExtractor).max().orElse(0);
    }

    private Set<Player> getPlayersByValue(Collection<Player> players, int paramValue, CardService cardService) {
        ToIntFunction<Player> paramExtractor = comparableParamExtractor(cardService);
        return players.stream()
                .filter(player -> paramExtractor.applyAsInt(player) == paramValue)
                .collect(Collectors.toSet());
    }

    protected abstract ToIntFunction<Player> comparableParamExtractor(CardService cardService);

}
