package com.terraforming.ares.services.ai;

import com.terraforming.ares.mars.MarsGame;
import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.Player;
import com.terraforming.ares.services.ai.dto.CardValueResponse;

import java.util.List;

public interface ICardValueService {

    CardValueResponse getWorstCard(MarsGame game, Player player, List<Integer> cards, int turn);

    Card getBestCardToBuild(MarsGame game, Player player, List<Card> cards, int turn, boolean ignoreCardIfBad);

    Integer getBestCard(MarsGame game, Player player, List<Integer> cards, int turn);


}
