package com.terraforming.ares.dto.blueAction;

import com.terraforming.ares.dto.CardDto;
import com.terraforming.ares.model.TurnResponse;
import com.terraforming.ares.model.TurnResponseType;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
@Builder
public class AutoPickCardsAction implements TurnResponse {
    @Singular
    List<CardDto> takenCards;

    @Override
    public TurnResponseType getResponseType() {
        return TurnResponseType.AUTO_PICK_CARDS;
    }

}
