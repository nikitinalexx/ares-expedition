package com.terraforming.ares.services.policyai.action;

/**
 * Общий интерфейс для всех действий всех голов.
 *
 * <p>Все реализации — кэшированные объекты, созданные один раз при старте.
 * Фабричные методы enum-голов никогда не аллоцируют новые объекты.
 *
 * <pre>
 *   HeadAction action = ActionInputService.buildProjectAction(card);
 *   record.chosenAction     = action.globalIndex();   // текущий формат
 *
 *   // будущий формат:
 *   record.chosenHead       = (byte)  action.head().ordinal();
 *   record.chosenLocalIndex = (short) action.localIndex();
 * </pre>
 */
public interface HeadAction {

    /** Голова, которой принадлежит это действие. */
    ActionHead head();

    /** Локальный индекс внутри головы (0-based). Используется как целевой класс при обучении. */
    int localIndex();

    /** Глобальный индекс в едином пространстве всех голов. */
    default int globalIndex() {
        return head().toGlobal(localIndex());
    }
}
