package com.terraforming.ares.model.winPoints;

import com.terraforming.ares.model.CardCollectableResource;
import lombok.Builder;
import lombok.Data;

/**
 * Created by oleksii.nikitin
 * Creation date 15.05.2022
 */
@Data
@Builder
public class WinPointsInfo {
    private CardCollectableResource type;
    private int resources;
    private int points;
}
