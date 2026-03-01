package com.terraforming.ares.services.policyai;

import com.terraforming.ares.services.policyai.dto.GameRecordArena;

import java.lang.ScopedValue;

public final class GameArenaContext {

    public static final ScopedValue<GameRecordArena>
            ARENA = ScopedValue.newInstance();

    public static GameRecordArena current() {
        return ARENA.get();
    }

}
