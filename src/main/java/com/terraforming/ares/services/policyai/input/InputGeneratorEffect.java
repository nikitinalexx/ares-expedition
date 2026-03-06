package com.terraforming.ares.services.policyai.input;

import com.terraforming.ares.model.Card;
import com.terraforming.ares.services.ai.advanced.TableContext;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;
import com.terraforming.ares.services.policyai.rollout.EffectStateReader;

import java.util.List;
import java.util.Map;

public interface InputGeneratorEffect {
    boolean isPresent(PolicyRecord record, TableContext tableContext, Card card, Map<Integer, List<Integer>> input);
    void generateAndApply(PolicyRecord record, TableContext tableContext, Map<Integer, List<Integer>> input, Card card);
}
