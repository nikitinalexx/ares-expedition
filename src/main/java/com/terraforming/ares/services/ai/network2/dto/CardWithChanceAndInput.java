package com.terraforming.ares.services.ai.network2.dto;

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
public class CardWithChanceAndInput {
    private Card card;
    private double chance;
    private Map<Integer, List<Integer>> inputParameters;
    private List<Payment> payments;
}
