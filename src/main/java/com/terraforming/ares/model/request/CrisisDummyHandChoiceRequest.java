package com.terraforming.ares.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by oleksii.nikitin
 * Creation date 12.05.2022
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CrisisDummyHandChoiceRequest {
    private String playerUuid;
    private List<String> choiceOptions;
}
