package com.terraforming.ares.services.ai.network;

import java.io.Serializable;
import java.util.function.Function;

public class DataColumn implements Serializable {
    private final int cols;
    private final int rows;
    private float[] values;

    public DataColumn(int cols, int rows, float[] values) {
        this.cols = cols;
        this.rows = rows;
        this.values = values;
    }

    public DataColumn(float... values) {
        this.rows = 1;
        this.cols = values.length;
        this.values = values;
    }

    public DataColumn(int cols) {
        if (cols <= 0) {
            throw new IllegalArgumentException("Invalid data parameters " + cols);
        }
        this.cols = cols;
        this.rows = 1;
        this.values = new float[cols];
    }

    public DataColumn(int rows, int cols, int depth) {
        if (rows <= 0 || cols <= 0 || depth <= 0) {
            throw new IllegalArgumentException("Invalid data parameters " + rows + " " + cols + " " + depth);
        }
        this.rows = rows;
        this.cols = cols;
        this.values = new float[rows * cols * depth];
    }

    public float get(int i) {
        return this.values[i];
    }


    public float get(int row, int col) {
        int idx = row * this.cols + col;
        return this.values[idx];
    }

    public float[] getValues() {
        return this.values;
    }

    public void setValues(float... values) {
        if (values.length != this.values.length) {
            throw new IllegalArgumentException("Invalid size of input data");
        }
        this.values = values;
    }

    public void copyFrom(float[] src) {
        System.arraycopy(src, 0, this.values, 0, this.values.length);
    }

    public int getCols() {
        return this.cols;
    }

    public int getRows() {
        return this.rows;
    }

    public int size() {
        return this.values.length;
    }


    public void add(float value, int i) {
        this.values[i] += value;
    }

    public void apply(Function<Float, Float> f) {
        for (int i = 0; i < this.values.length; ++i) {
            this.values[i] = f.apply(this.values[i]);
        }
    }

    public final void div(DataColumn another) {
        for (int i = 0; i < this.values.length; i++) {
            this.values[i] /= another.values[i];
        }
    }

}
