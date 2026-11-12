# Observation feature review

Audit of the V2-derived feature vector (tools: `ml/audit_features.py`). NETWORK_V2 still uses the old full vector; the **agent** uses observation **v3**.

## Decisions taken

1. V2 encoding is basically sound (no constants, no dead card slots, no linear duplicates beyond exact copies).
2. **210 / 898 columns were exact copies** (hand re-encoding milestones/awards/tags already on the table). Removed in v3 via mask `src/main/resources/observation/v3_mask.json` (`ObservationMaskTest`).
3. **Round-context block** added (whose phases, current phase, builds left, blue activations, draft modifiers). Hidden while phases are being chosen; visible inside the round (`ObservationCompletenessTest`).
4. Features with no *linear* link to outcome were **kept** (may be nonlinear). Candidate list for permutation importance later.
5. Trainer standardises inputs from data — static V2 max-normalisation quirks do not matter for the agent.

## Audit snapshot (915 cols, 66k states)

| issue | count |
|---|---|
| constants | 0 |
| near-constants (>99.9% one value) | 9 |
| exact copies (redundant) | 210 |
| linear copies beyond exact | 0 |
| outside [0, 1] (bad max) | 1 (`extra:round/40`) |
| use <10% of range | 2 (bird/animal counts) |
| dead card slots | 0 |
| no linear outcome link among varying | 108 |

Regenerate mask: `python ml/audit_features.py … --write-mask`. Changing layout changes the schema hash and rejects old data/models.
