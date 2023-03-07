package com.terraforming.ares.dto;

import com.terraforming.ares.model.CrisisDummyCard;
import com.terraforming.ares.model.DetrimentToken;
import lombok.Builder;
import lombok.Data;

import java.util.*;

/**
 * Created by oleksii.nikitin
 * Creation date 03.03.2023
 */
@Data
@Builder
public class CrysisDto {
    private Set<DetrimentToken> detrimentTokens;
    private List<CrysisCardDto> openedCards;
    private Map<Integer, Integer> cardToTokensCount;
    private Map<String, Integer> forbiddenPhases;
    private List<CrisisDummyCard> currentDummyCards;
    private List<Integer> chosenDummyPhases;
    private boolean wonGame;
}
