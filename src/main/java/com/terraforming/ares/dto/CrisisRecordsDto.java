package com.terraforming.ares.dto;

import com.terraforming.ares.entity.CrisisRecordEntity;

import java.util.List;

public class CrisisRecordsDto {
    List<CrisisRecordEntity> topTwentyRecordsByPoints;
    List<CrisisRecordEntity> topTwentyRecordsByTurns;

    public CrisisRecordsDto(List<CrisisRecordEntity> topTwentyRecordsByPoints, List<CrisisRecordEntity> topTwentyRecordsByTurns) {
        this.topTwentyRecordsByPoints = topTwentyRecordsByPoints;
        this.topTwentyRecordsByTurns = topTwentyRecordsByTurns;
    }
}
