package com.terraforming.ares.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by oleksii.nikitin
 * Creation date 25.04.2022
 */
@Service
public class ShuffleService {

    // Fisher-Yates: position i is swapped with a random position among the not yet fixed ones [0, i]. Swapping with a random
    // index of the WHOLE list (what was here before) makes some permutations several times more likely than others.
    public <T> void shuffle(List<T> elements) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = elements.size() - 1; i > 0; i--) {
            int swapWith = random.nextInt(i + 1);
            T temp = elements.get(i);
            elements.set(i, elements.get(swapWith));
            elements.set(swapWith, temp);
        }
    }
}
