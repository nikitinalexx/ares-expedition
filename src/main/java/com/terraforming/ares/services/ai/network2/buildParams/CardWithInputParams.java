package com.terraforming.ares.services.ai.network2.buildParams;

import com.terraforming.ares.model.Card;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CardWithInputParams {
    Card card;
    List<Map<Integer, List<Integer>>> inputParamsVariations;

    public CardWithInputParams(Card card, List<Map<Integer, List<Integer>>> inputParamsVariations) {
        this.card = card;
        this.inputParamsVariations = inputParamsVariations;
    }

    public CardWithInputParams(Card card, Map<Integer, List<Integer>> inputParamsVariations) {
        this.card = card;
        this.inputParamsVariations = List.of(inputParamsVariations);
    }
}
