package com.terraforming.ares.dto;

import com.terraforming.ares.model.awards.AwardType;
import com.terraforming.ares.model.awards.BaseAward;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Created by oleksii.nikitin
 * Creation date 13.06.2022
 */
@Builder
@Getter
public class AwardDto {
    private final AwardType type;
    private final Map<String, Integer> winPoints;

    public static AwardDto from(BaseAward award) {
        return AwardDto.builder()
                .type(award.getType())
                .winPoints(award.getAwards())
                .build();
    }
}
