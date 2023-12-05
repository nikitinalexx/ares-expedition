package com.terraforming.ares.model.request;

import com.terraforming.ares.model.StandardProjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 12.05.2022
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class StandardProjectRequest {
    private String playerUuid;
    private StandardProjectType type;
    private Map<Integer, List<Integer>> inputParams;
}
