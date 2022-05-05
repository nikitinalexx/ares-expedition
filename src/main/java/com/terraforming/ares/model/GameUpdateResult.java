package com.terraforming.ares.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

/**
 * Created by oleksii.nikitin
 * Creation date 05.05.2022
 */
@Value
@Builder
public class GameUpdateResult<T> {
    T result;
    String error;

    public boolean finishedWithError() {
        return StringUtils.hasLength(error);
    }

}
