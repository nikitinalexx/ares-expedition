package com.terraforming.ares.services.policyai.action;

/**
 * Перечисление всех голов (output heads) политики.
 *
 * <p>Порядок объявления = ordinal() = то, что пишется в PolicyRecord.chosenHead.
 * Не менять порядок без миграции сохранённых записей.
 */
public enum ActionHead {

    BUILD       (BuildAction.COUNT),
    SELL_DISCARD(SellDiscardAction.COUNT),
    TARGET      (TargetAction.COUNT),
    BLUE        (BlueAction.COUNT),
    CORPORATION (CorporationAction.COUNT),
    SMALL       (SmallAction.COUNT),
    SYSTEM      (SystemAction.COUNT);

    private final int count;
    private int start;

    ActionHead(int count) {
        this.count = count;
    }

    static {
        int offset = 0;
        for (ActionHead head : values()) {
            head.start = offset;
            offset += head.count;
        }
    }

    public int globalStart() { return start; }
    public int count()       { return count; }

    public int toGlobal(int localIndex) {
        if (localIndex < 0 || localIndex >= count)
            throw new IllegalArgumentException(name() + ": localIndex " + localIndex + " out of [0," + count + ")");
        return start + localIndex;
    }

    public int toLocal(int globalIndex) {
        int local = globalIndex - start;
        if (local < 0 || local >= count)
            throw new IllegalArgumentException(name() + ": globalIndex " + globalIndex + " not in [" + start + "," + (start + count) + ")");
        return local;
    }

    public boolean contains(int globalIndex) {
        return globalIndex >= start && globalIndex < start + count;
    }

    /** O(n) — не вызывать в горячем пути. */
    public static ActionHead of(int globalIndex) {
        for (ActionHead h : values())
            if (h.contains(globalIndex)) return h;
        throw new IllegalArgumentException("No ActionHead contains globalIndex=" + globalIndex);
    }

    public static int totalActions() {
        ActionHead[] h = values();
        return h[h.length - 1].start + h[h.length - 1].count;
    }
}
