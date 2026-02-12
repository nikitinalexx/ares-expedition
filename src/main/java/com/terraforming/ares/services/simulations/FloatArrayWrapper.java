package com.terraforming.ares.services.simulations;

import java.util.Arrays;

public record FloatArrayWrapper(float[] data) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FloatArrayWrapper that)) return false;
        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
