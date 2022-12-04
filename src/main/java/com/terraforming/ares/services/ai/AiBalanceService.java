package com.terraforming.ares.services.ai;

import com.terraforming.ares.model.Player;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiBalanceService {
    public static final double VALUE_WORTH_TO_DISCARD = 56;
    public static final double FUTURE_TURN_CARD_STATS_TO_CONSIDER = 10;
    public static final double MINIMUM_BUILD_CARD_WORTH = 52;


    public boolean isCardWorthToDiscard(Player player, double value) {
        return value <= VALUE_WORTH_TO_DISCARD;
    }

    public double getFutureTurnCardStatsToConsider() {
        return FUTURE_TURN_CARD_STATS_TO_CONSIDER;
    }

    public double getMinimumBuildCardWorth() {
        return MINIMUM_BUILD_CARD_WORTH;
    }





    public static final double DEFAULT_CALIBRATION_VALUE = 50;
    public static double CHANGABLE_CALIBRATION_VALUE = DEFAULT_CALIBRATION_VALUE;

    public double getBuildCardWorth(Player player) {
        if (player.getUuid().endsWith("0")) {
            return CHANGABLE_CALIBRATION_VALUE;
        } else {
            return DEFAULT_CALIBRATION_VALUE;
        }
    }


    public List<Integer> calibrationValues() {
        List<Integer> calibrationParams = new ArrayList<>();
        for (int i = 45; i < 60; i++) {
            calibrationParams.add(i);
        }
        return calibrationParams;
    }

    public void setCalibrationValue(double calibrationValue) {
        CHANGABLE_CALIBRATION_VALUE = calibrationValue;
    }
}