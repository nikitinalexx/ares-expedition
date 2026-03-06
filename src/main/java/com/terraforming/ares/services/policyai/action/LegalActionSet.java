package com.terraforming.ares.services.policyai.action;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public final class LegalActionSet {
    private final List<HeadAction> actions;

    public List<HeadAction> actions() {
        return actions;
    }
}
