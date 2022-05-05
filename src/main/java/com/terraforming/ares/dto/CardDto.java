package com.terraforming.ares.dto;

import com.terraforming.ares.model.GenericCard;
import lombok.Value;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
public class CardDto {
    String description;

    public static CardDto from(GenericCard card) {
        return new CardDto(card.description());
    }
}
