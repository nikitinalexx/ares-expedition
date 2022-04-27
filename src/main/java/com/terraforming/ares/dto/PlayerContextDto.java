package com.terraforming.ares.dto;

import com.terraforming.ares.model.Deck;
import lombok.Builder;
import lombok.Getter;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@Builder
@Getter
public class PlayerContextDto {
    private Deck corporationsChoice;
    private Integer corporationId;
    private Integer stage;
}
