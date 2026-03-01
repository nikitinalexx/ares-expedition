package com.terraforming.ares.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Created by oleksii.nikitin
 * Creation date 27.02.2023
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class BuildDto {
    BuildType type;
    int extraDiscount;
    int priceLimit;

    public BuildDto(BuildType type) {
        this.type = type;
        this.extraDiscount = 0;
        this.priceLimit = 0;
    }

    public BuildDto(BuildType type, int extraDiscount) {
        this.type = type;
        this.extraDiscount = extraDiscount;
        this.priceLimit = 0;
    }

    public BuildDto(BuildType type, int extraDiscount, int priceLimit) {
        this.type = type;
        this.extraDiscount = extraDiscount;
        this.priceLimit = priceLimit;
    }
}
