package com.terraforming.ares.model.request;

import com.terraforming.ares.model.payments.Payment;
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
public class BuildProjectRequest {
    private String playerUuid;
    private Integer cardId;
    private List<Payment> payments;
    private Map<Integer, List<Integer>> inputParams;
}
