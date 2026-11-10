package com.terraforming.ares.services.agent;

/**
 * One atomic answer for a blue-action input slot.
 *
 * @param id   card id, phase 0..9, tag ordinal, heat/microbe amount, or a sentinel from {@link BlueInputDecisions}
 * @param kind what the id means for this slot
 * @param flag {@link com.terraforming.ares.model.InputFlag} id written into the map, or -1 when the kind packs several
 */
public record BlueInputChoice(int id, int kind, int flag) {
}
