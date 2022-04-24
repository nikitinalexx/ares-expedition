package com.alex.nikitin.terraformingm.model;

import lombok.Builder;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Builder
public class PlayerContext {
    private Deck hand;
    private Deck played;
}
