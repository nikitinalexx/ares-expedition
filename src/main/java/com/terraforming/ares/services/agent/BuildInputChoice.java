package com.terraforming.ares.services.agent;

/**
 * One atomic on-built answer: a phase upgrade, a dynamic tag, a resource target, a plant, a skip, and so on.
 * {@link #id()} is what is written to the decision file (u16); {@link #kind()} disambiguates overlapping numeric values.
 *
 * @param id    card id, phase 0..9, tag ordinal, or a special constant from {@link BuildInputDecisions}
 * @param kind  what the id means for this slot
 * @param flag  {@link com.terraforming.ares.model.InputFlag} id this answer writes into, or -1 when the kind packs several
 */
public record BuildInputChoice(int id, int kind, int flag) {
}
