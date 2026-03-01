package com.terraforming.ares.services.policyai.dto;

import java.util.Arrays;

public final class GameRecordArena {

    private PolicyRecord[] records = new PolicyRecord[512];
    private int size;

    public PolicyRecord next() {

        if (size == records.length) {
            records = Arrays.copyOf(
                    records,
                    records.length * 2);
        }

        PolicyRecord r = records[size];

        if (r == null) {
            r = new PolicyRecord();
            records[size] = r;
        } else {
            r.reset();
        }

        size++;

        return r;
    }

    public void add(PolicyRecord policyRecord) {
        if (size == records.length) {
            records = Arrays.copyOf(
                    records,
                    records.length * 2);
        }
        records[size++] = policyRecord;
    }

    public PolicyRecord[] records() {
        return records;
    }

    public int size() {
        return size;
    }
}
