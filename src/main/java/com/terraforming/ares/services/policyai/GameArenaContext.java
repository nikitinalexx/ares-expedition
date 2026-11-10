package com.terraforming.ares.services.policyai;

import com.terraforming.ares.services.policyai.dto.GameRecordArena;
import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.lang.ScopedValue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public final class GameArenaContext {

    public static final ScopedValue<GameRecordArena> ARENA = ScopedValue.newInstance();
    public static final ScopedValue<Boolean> SIMULATION = ScopedValue.newInstance();

    public static ArrayBlockingQueue<PolicyRecord> RECORD_QUEUE = null;

//    private static final GameRecordArena SHARED_ARENA = new GameRecordArena();

    public static GameRecordArena current() {
        return ARENA.get();
//        return SHARED_ARENA;
    }

    public static boolean isArenaAvailable() {
        return !isSimulation() && ARENA.isBound();
    }

    public static boolean isSimulation() {
        return SIMULATION.isBound() && SIMULATION.get();
    }

}
