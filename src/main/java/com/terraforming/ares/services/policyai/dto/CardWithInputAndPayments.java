package com.terraforming.ares.services.policyai.dto;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.model.payments.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Builder
public class CardWithInputAndPayments {
    private Card card;
    private Map<Integer, List<Integer>> inputParameters;
    private List<Payment> payments;
}
