package com.terraforming.ares.model.build;

import com.terraforming.ares.model.CardCollectableResource;
import lombok.Builder;
import lombok.Data;

/**
 * Created by oleksii.nikitin
 * Creation date 15.05.2022
 */
@Data
@Builder
public class PutResourceOnBuild {
    private int count;
    private CardCollectableResource type;
    private int paramId;
}
