package com.terraforming.ares.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 26.04.2022
 */
@Data
@Builder
public class CardWithChoiceTurnDto extends TurnDto {
    private String playerUuid;
    private Integer card;
}
