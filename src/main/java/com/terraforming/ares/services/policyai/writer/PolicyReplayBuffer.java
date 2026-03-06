package com.terraforming.ares.services.policyai.writer;

import com.terraforming.ares.services.policyai.dto.PolicyRecord;

import java.util.List;
import java.util.Random;

public final class PolicyReplayBuffer {

    private final PolicyRecord[] buffer;
    private final Random rnd = new Random();

    private int size = 0;

    public PolicyReplayBuffer(int capacity) {
        buffer = new PolicyRecord[capacity];
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return buffer.length;
    }

    public void add(PolicyRecord r) {

        if (size < buffer.length) {
            buffer[size++] = r;
            return;
        }

        // overwrite random (AlphaZero style)
        int replace = rnd.nextInt(buffer.length);
        buffer[replace] = r;
    }

    public void sample(List<PolicyRecord> out, int count) {

        for (int i = 0; i < count; i++) {

            int idx = rnd.nextInt(size);

            out.add(buffer[idx]);

            // remove via swap
            buffer[idx] = buffer[size - 1];
            buffer[size - 1] = null;

            size--;
        }
    }

}