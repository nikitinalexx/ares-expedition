package com.terraforming.ares.services.ai.dl4j;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class PolicyModelInference {

    // --- Ваши константы MAXIMA (должны совпадать с константами обучения) ---
    private static final double[] FIXED_MAXIMA = new double[]{35.25f, 14.25f, 38.25f, 9.25f, 115.75f, 96.25f, 240.25f, 11.25f, 12.25f, 8.25f, 43.25f, 45.25f, 79.25f, 11.25f, 56.25f, 9.25f, 3.25f, 22.25f, 3.25f, 1.25f, 1.00f, 1.00f, 5.25f, 1.25f, 1.00f, 2.25f, 22.25f, 15.25f, 16.25f, 20.25f, 6.25f, 11.25f, 24.25f, 3.25f, 9.25f, 5.25f, 1.00f, 1.25f, 2.25f, 3.25f, 5.25f, 4.25f, 3.25f, 2.25f, 1.25f, 1.00f, 1.25f, 2.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 4.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.00f, 1.00f, 1.00f, 1.25f, 1.00f, 1.25f, 1.25f, 1.25f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.00f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.25f, 1.25f, 1.00f, 1.00f, 1.00f, 115.75f, 96.25f, 240.25f, 11.25f, 12.25f, 8.25f, 43.25f, 45.25f, 79.25f, 11.25f, 56.25f, 9.25f, 3.25f, 22.25f, 3.25f, 1.25f, 1.00f, 1.00f, 5.25f, 1.25f, 1.00f, 2.25f, 22.25f, 15.25f, 16.25f, 20.25f, 6.25f, 11.25f, 24.25f, 3.25f, 9.25f, 5.25f, 1.00f, 1.25f, 2.25f, 3.25f, 5.25f, 4.25f, 3.25f, 2.25f, 1.25f, 1.00f, 1.25f, 2.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 4.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.00f, 1.00f, 1.00f, 1.25f, 1.00f, 1.25f, 1.25f, 1.25f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.00f, 1.00f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.25f, 1.00f, 1.25f, 1.25f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f, 1.00f};

    private static final int NUM_FEATURES = 321;

    private final ThreadLocal<MultiLayerNetwork> firstModel;
    private final ThreadLocal<MultiLayerNetwork> secondModel;
    private final CustomMaxScaler scaler;

    private static final String modelPath1 = "policy_model_epoch_14.zip";
    private static final String modelPath2 = "policy_model_epoch_14.zip";

    /**
     * Конструктор: загружает модель и инициализирует скейлер.
     * * @param modelPath Путь к ZIP-файлу модели DL4J (например, "dl4j_game_policy_model_final.zip").
     * @throws IOException Если файл не найден или ошибка чтения.
     */
    public PolicyModelInference() throws IOException {
        System.out.println("Initializing Policy Model Inference from: " + modelPath1);

        // 1. Загрузка модели
        File modelFile1 = new File(modelPath1);
        if (!modelFile1.exists()) {
            throw new IOException("Model file not found at: " + modelPath1);
        }

        File modelFile2 = new File(modelPath2);
        if (!modelFile2.exists()) {
            throw new IOException("Model file not found at: " + modelPath2);
        }

        firstModel = ThreadLocal.withInitial(() -> {
            try {
                return MultiLayerNetwork.load(modelFile1, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        secondModel = ThreadLocal.withInitial(() -> {
            try {
                return MultiLayerNetwork.load(modelFile2, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Model loaded successfully.");

        // 2. Инициализация Скейлера для предобработки входных данных
        // Мы используем тот же самый CustomMaxScaler, чтобы гарантировать,
        // что входные данные масштабируются точно так же, как и обучающие.
        this.scaler = new CustomMaxScaler(FIXED_MAXIMA);
    }

    /**
     * Выполняет предсказание (инференс) на основе входного массива признаков.
     * * @param inputFeatures Массив из 321 параметра (Float или Double).
     * @return Значение вероятности (Float) между 0.0 и 1.0.
     */
    public float predict(float[] inputFeatures, int model) {
        if (inputFeatures.length != NUM_FEATURES) {
            throw new IllegalArgumentException("Expected " + NUM_FEATURES + " features, but got " + inputFeatures.length);
        }

        // 1. Преобразование входного массива в INDArray
        // Важно: Создаем матрицу 1xN (одна строка, N столбцов)
        INDArray inputNdArray = Nd4j.create(inputFeatures).reshape(1, NUM_FEATURES);

        // 2. Препроцессинг: Масштабирование
        // Мы используем scaler для масштабирования входных данных
        scaler.transform(inputNdArray);

        // 3. Выполнение прямого прохода (Forward Pass)
        // output - это INDArray с одним значением (так как NUM_OUTPUTS=1)
        INDArray output = (model == 1 ? firstModel : secondModel).get().output(inputNdArray, false);

        // 4. Извлечение результата
        // Поскольку выходной слой использует SIGMOID, результат будет float от 0 до 1.
        return output.getFloat(0);
    }
}