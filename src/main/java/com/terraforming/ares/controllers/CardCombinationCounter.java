package com.terraforming.ares.controllers;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class CardCombinationCounter {

    // Маппинг реальных ID карт в битовые позиции (0-127)
    private final Map<Integer, Integer> cardIdToBitPosition = new ConcurrentHashMap<>();
    private int nextBitPosition = 0;

    // Хранит все уникальные комбинации (пара long'ов)
    private final Set<CardCombination> uniqueCombinations = ConcurrentHashMap.newKeySet();

    // Lock для обеспечения атомарности присвоения битовых позиций
    private final Lock lock = new ReentrantLock();

    /**
     * Класс для хранения комбинации из двух long (128 бит)
     */
    private static class CardCombination {
        final long low;  // биты 0-63
        final long high; // биты 64-127

        CardCombination(long low, long high) {
            this.low = low;
            this.high = high;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CardCombination)) return false;
            CardCombination that = (CardCombination) o;
            return low == that.low && high == that.high;
        }

        @Override
        public int hashCode() {
            return Objects.hash(low, high);
        }

        @Override
        public String toString() {
            return String.format("CardCombination{low=%016X, high=%016X}", low, high);
        }
    }

    /**
     * Обрабатывает результат одной игры (потокобезопасно)
     */
    public void processGame(List<Integer> playedCardIds) {
        CardCombination combination = convertToBitMask(playedCardIds);
        uniqueCombinations.add(combination);
    }

    /**
     * Преобразует список ID карт в битовую маску (2 long)
     * Потокобезопасно создаёт маппинг для новых ID
     */
    private CardCombination convertToBitMask(List<Integer> cardIds) {
        long low = 0L;
        long high = 0L;

        for (Integer cardId : cardIds) {
            int bitPosition = getOrCreateBitPosition(cardId);

            if (bitPosition < 64) {
                low |= (1L << bitPosition);
            } else {
                high |= (1L << (bitPosition - 64));
            }
        }

        return new CardCombination(low, high);
    }

    /**
     * Получает существующую или создаёт новую битовую позицию для ID
     * Использует double-checked locking для производительности
     */
    private int getOrCreateBitPosition(Integer cardId) {
        // Быстрый путь - позиция уже есть
        Integer position = cardIdToBitPosition.get(cardId);
        if (position != null) {
            return position;
        }

        // Медленный путь - нужно создать новую позицию
        lock.lock();
        try {
            // Double-check: может другой поток уже создал
            position = cardIdToBitPosition.get(cardId);
            if (position != null) {
                return position;
            }

            // Создаём новую позицию
            if (nextBitPosition >= 128) {
                throw new IllegalStateException(
                        "Превышено количество уникальных карт (>128). Найдено: " + (nextBitPosition + 1)
                );
            }

            int newPosition = nextBitPosition++;
            cardIdToBitPosition.put(cardId, newPosition);
            return newPosition;

        } finally {
            lock.unlock();
        }
    }

    /**
     * Возвращает количество уникальных комбинаций
     */
    public int getUniqueCount() {
        return uniqueCombinations.size();
    }

    /**
     * Возвращает количество различных карт, встреченных за все игры
     */
    public int getTotalUniqueCards() {
        return cardIdToBitPosition.size();
    }

    /**
     * Декодирует комбинацию обратно в список ID (для отладки)
     */
    public List<Integer> decodeCombination(CardCombination combination) {
        List<Integer> cardIds = new ArrayList<>();

        // Обратный маппинг: позиция -> ID
        Map<Integer, Integer> positionToId = new HashMap<>();
        cardIdToBitPosition.forEach((id, pos) -> positionToId.put(pos, id));

        // Проверяем биты в low
        for (int i = 0; i < 64; i++) {
            if ((combination.low & (1L << i)) != 0) {
                Integer cardId = positionToId.get(i);
                if (cardId != null) {
                    cardIds.add(cardId);
                }
            }
        }

        // Проверяем биты в high
        for (int i = 0; i < 64; i++) {
            if ((combination.high & (1L << i)) != 0) {
                Integer cardId = positionToId.get(i + 64);
                if (cardId != null) {
                    cardIds.add(cardId);
                }
            }
        }

        return cardIds;
    }

}