package com.terraforming.ares.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class ShuffleService {
    private final Random random = new Random(0);

    public <T> void shuffle(List<T> elements) {
        for (int i = 0; i < elements.size() - 1; i++) {
            int swapWith = random.nextInt(elements.size());

            T temp = elements.get(i);
            elements.set(i, elements.get(swapWith));
            elements.set(swapWith, temp);
        }
    }

}
