package com.terraforming.ares.services.ai.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
public class ActionInputParamsResponse {
    private static final ActionInputParamsResponse makeActionResponse = ActionInputParamsResponse.builder().makeAction(true).build();
    private static final ActionInputParamsResponse noActionResponse = ActionInputParamsResponse.builder().makeAction(false).build();


    boolean makeAction;
    Map<Integer, List<Integer>> inputParams;

    public static ActionInputParamsResponse makeAction() {
        return makeActionResponse;
    }

    public static ActionInputParamsResponse noAction() {
        return noActionResponse;
    }

    public static ActionInputParamsResponse makeActionWithParams(Map<Integer, List<Integer>> params) {
        return ActionInputParamsResponse.builder().makeAction(true).inputParams(params).build();
    }

}
