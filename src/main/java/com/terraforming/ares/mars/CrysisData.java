package com.terraforming.ares.mars;

import com.terraforming.ares.model.CrisisDummyCard;
import com.terraforming.ares.model.Deck;
import com.terraforming.ares.model.DetrimentToken;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
@NoArgsConstructor
@Getter
@Setter
public class CrysisData {
    private Set<DetrimentToken> detrimentTokens;
    private Deck crysisCards;
    private Map<String, Integer> forbiddenPhases = new HashMap<>();

    private List<Integer> openedCards = new ArrayList<>();
    private Map<Integer, Integer> cardToTokensCount = new HashMap<>();
    private int crisisVp = 0;
    private List<CrisisDummyCard> usedDummyCards = new ArrayList<>();
    private List<CrisisDummyCard> currentDummyCards;
    private List<Integer> chosenDummyPhases;
    private boolean wonGame;
    private int easyModeTurnsLeft;
    private boolean easyMode;
}
