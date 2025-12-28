package com.terraforming.ares.services.ai.dto;

import com.terraforming.ares.model.payments.Payment;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class BuildContext {
    private final List<Payment> payments;
    private final Map<Integer, List<Integer>> inputParams;
    private final int cardId;

    public BuildContext(List<Payment> payments, Map<Integer, List<Integer>> inputParams, int cardId) {
        this.payments = payments;
        this.inputParams = inputParams;
        this.cardId = cardId;
    }

    public BuildContext(BuildContext copy) {
        this(new ArrayList<>(copy.getPayments()), new HashMap<>(copy.getInputParams()), copy.cardId);
    }

}
