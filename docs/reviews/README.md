# reviews/ — the adversarial review exchange

How this works, so that a future reader does not have to guess the protocol.

1. **LETTER_n.md** — a self-contained brief written by our assistant for an outside model. It states the project, the constraints, the claims (C1,
   C2, ...), the questions, the known failure modes, and what would make us drop the plan. It also lists the files the reviewer should read
   (`docs/PLAN.md`, `docs/LEDGER.md`, `docs/METHOD.md`, `docs/RESULTS.md`, `docs/GAME_RULES.md`).
2. **CRITIQUE_n.md** — written by the reviewer, in the format the letter specifies: one block per claim with a verdict, an argument, evidence from
   our files, and the cheapest test that would settle it, plus "what I would do first" and "where this plan is most likely to be wrong".
3. **REPLY_n.md** — our point-by-point answer. For each objection: accepted (with the measurement that will settle it, or the change to the plan),
   rejected (with the measurement that contradicts it), or deferred (with the reason). A reply may not answer an objection with an opinion.

A reviewer's claim has the same status as ours: it is a hypothesis until measured. If a critique changes the plan, the change goes into
`docs/PLAN.md` and the ledger keeps the measurement that justified it.

Files:

* `LETTER_1.md` — the first brief (state of 20/11).
* `CRITIQUE_1.md` — expected.
* `REPLY_1.md` — expected.
