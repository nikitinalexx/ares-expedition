package com.terraforming.ares.services.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class CardValueResponse {
    Integer cardId;
    float worth;

    public static CardValueResponse of(int cardId, float worth) {
        return new CardValueResponse(cardId, worth);
    }

    public static CardValueResponse notUsableCard(int cardId) {
        return new CardValueResponse(cardId, -1);
    }
}
