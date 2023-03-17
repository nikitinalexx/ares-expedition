package com.terraforming.ares.dto;

import com.terraforming.ares.mars.MarsGame;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GameWithState {
    MarsGame game;
    float state;
}
