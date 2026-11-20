"""Round-level score bookkeeping, read from a pack's label files only.

Why a separate module: two premise measurements of 20/11 need the same small piece of arithmetic - "what was each seat's score at the
END of each round, and by which of the game's seven channels" - and both must agree about the definition. `points_by_round.py` asks
WHEN the points are created (if they are made in the last two rounds, long-horizon plan choice is secondary); `behaviour_regions.py`
asks whether strong games occupy distinct regions of behaviour space (if they do not, coverage chases ghosts).

The definition used everywhere here, and the reason it needs stating:
  * a row's `breakdown` is the score AT THAT ROW, not at the end of the round;
  * so the round-end score is the row with the largest `step` for that (game, seat, round);
  * a round's points are the difference between consecutive round-end scores, and round 1's points are its level (the score at the
    end of round 1 against zero; the starting corporation and TR are part of it).

Nothing here reads models. Both premise scripts read labels only, except `points_by_round.py`, which needs two observation columns
(`score:my_winning_points`, `score:opponent_winning_points`) because the pack's `breakdown` field is the FINAL score of the game (it is
written once per game and replicated on every row) - not the score at that row. That distinction cost an investigation on 20/11: a
"points per round" chart built from `breakdown` shows 100% of the points in round 0, which is an artefact, not a fact.
"""
import json
import os

import numpy as np

# The order of WinPointsService.ScoreBreakdown, as DecisionLogs writes it (line ~371).
CHANNELS = ["tr", "forests", "cards", "resource_cards", "milestones", "awards", "extra"]

SCORE_COLUMNS = ("score:my_winning_points", "score:opponent_winning_points")

FIELDS = ("outcome", "margin", "seat", "context", "round", "step", "game", "breakdown", "keep")


def load_labels(pack_dir, types=None, include_afterstates=False):
    """Concatenate the label arrays of every decision type in a pack.

    `keep` is applied here: it is the de-duplication mask the pack computed once (the same state recorded by several decisions), and
    leaving duplicates in would weight a round by how many decision types it happened to contain - the same style-weighting defect the
    owner identified for rows.
    """
    with open(os.path.join(pack_dir, "manifest.json"), encoding="utf-8") as handle:
        manifest = json.load(handle)
    names = [name for name in manifest["types"] if manifest["types"][name]["rows"] > 0]
    if types:
        wanted = set(types)
        names = [name for name in names if name in wanted]
    if not include_afterstates:
        names = [name for name in names if name != "AFTERSTATE"]
    if not names:
        raise SystemExit(f"{pack_dir}: no label files for types {types}")
    collected = {field: [] for field in FIELDS}
    for name in names:
        with np.load(os.path.join(pack_dir, f"{name}.lab.npz")) as labels:
            keep = labels["keep"].astype(bool)
            for field in FIELDS:
                collected[field].append(np.asarray(labels[field])[keep])
    out = {field: np.concatenate(values) for field, values in collected.items()}
    out["types"] = names
    out["manifest"] = manifest
    return out


def round_table(labels):
    """One row per (game, seat, round): the last decision of that round, i.e. the round-end score.

    Returns arrays game, seat, round, step, breakdown(n,7), margin(n), outcome(n) - one entry per seat-round.
    """
    game = np.asarray(labels["game"]).astype(np.int64)
    seat = np.asarray(labels["seat"]).astype(np.int64)
    rnd = np.asarray(labels["round"]).astype(np.int64)
    step = np.asarray(labels["step"]).astype(np.int64)
    if len(game) == 0:
        empty = np.zeros(0, dtype=np.int64)
        return {"game": empty, "seat": empty, "round": empty, "step": empty,
                "breakdown": np.zeros((0, len(CHANNELS)), dtype=np.float32),
                "margin": np.zeros(0, dtype=np.float32), "outcome": np.zeros(0, dtype=np.float32)}
    order = np.lexsort((step, rnd, seat, game))
    game, seat, rnd, step = game[order], seat[order], rnd[order], step[order]
    # last row of each (game, seat, round): the group changes where any key changes, so keep the element BEFORE a change
    changes = np.ones(len(game), dtype=bool)
    if len(game) > 1:
        changes[:-1] = (game[1:] != game[:-1]) | (seat[1:] != seat[:-1]) | (rnd[1:] != rnd[:-1])
    keep = np.flatnonzero(changes)
    breakdown = np.asarray(labels["breakdown"]).astype(np.float32)[order][keep]
    return {"game": game[keep], "seat": seat[keep], "round": rnd[keep], "step": step[keep],
            "breakdown": breakdown,
            "margin": np.asarray(labels["margin"]).astype(np.float32)[order][keep],
            "outcome": np.asarray(labels["outcome"]).astype(np.float32)[order][keep]}


def round_deltas(table):
    """Points created in each round, by channel: round 1 against zero, then consecutive round-end differences."""
    channels = np.zeros_like(table["breakdown"])
    if len(channels) == 0:
        return channels
    previous = np.zeros_like(table["breakdown"])
    same = np.zeros(len(channels), dtype=bool)
    same[1:] = (table["game"][1:] == table["game"][:-1]) & (table["seat"][1:] == table["seat"][:-1])
    previous[1:][same[1:]] = table["breakdown"][:-1][same[1:]]
    return table["breakdown"] - previous


def final_score(table):
    """The score at the last recorded round of each (game, seat) - the game's own total for that seat."""
    n = len(table["game"])
    if n == 0:
        return np.zeros((0, len(CHANNELS)), dtype=np.float32)
    last = np.ones(n, dtype=bool)
    if n > 1:
        last[:-1] = (table["game"][1:] != table["game"][:-1]) | (table["seat"][1:] != table["seat"][:-1])
    return table["breakdown"][last]


def share_created_in_last(per_round, rounds_left):
    """Share of a seat's total points that was created in its last `rounds_left` rounds.

    Written as a function because the number is quoted: "X% of the points are made in the last 3 rounds" decides whether the plan is
    a whole-game object or an end-game object, and an off-by-one here would decide a direction of the project.
    """
    total = per_round.sum()
    if total <= 0:
        return float("nan")
    tail = per_round[-rounds_left:].sum() if rounds_left > 0 else 0.0
    return float(tail / total)


def summarise(labels, tail_rounds=3):
    """Everything the two premise scripts need, computed once."""
    table = round_table(labels)
    deltas = round_deltas(table)
    if len(table["game"]) == 0:
        raise SystemExit("no rows")
    order = np.lexsort((table["round"], table["seat"], table["game"]))
    game, seat, rnd = table["game"][order], table["seat"][order], table["round"][order]
    per_row = deltas[order]
    rows = []
    start = 0
    for i in range(1, len(game) + 1):
        if i == len(game) or game[i] != game[start] or seat[i] != seat[start]:
            sl = slice(start, i)
            per = per_row[sl]
            rows.append({
                "game": int(game[start]),
                "seat": int(seat[start]),
                "rounds": int(rnd[sl].max()),
                "score": float(per.sum()),
                "tail": _tail_share(rnd[sl], per, tail_rounds),
            })
            start = i
    return {"table": table, "deltas": deltas, "per_seat": rows, "tail_rounds": tail_rounds}


def _tail_share(rnd, per_round, tail_rounds):
    """Share of a seat's points created in its last `tail_rounds` rounds, on its own round grid."""
    totals = {}
    for r in np.unique(rnd):
        totals[int(r)] = per_round[rnd == r].sum(axis=0)
    total = sum(float(v.sum()) for v in totals.values())
    if total <= 0:
        return float("nan")
    rounds = sorted(totals)
    tail = rounds[-tail_rounds:] if tail_rounds > 0 else []
    return float(sum(float(totals[r].sum()) for r in tail) / total)


def descriptor_shares(breakdown):
    """Behaviour descriptor: the share of a score that came from each group of channels.

    Three groups, not seven: the seven channels are highly correlated and a coarse descriptor is what coverage needs (a fine one
    would report "192 cells, all occupied" and mean nothing). Grouping is by the game's own scoring structure: terraforming (TR +
    forests), printed/resource card points, and awards/milestones/extra.
    """
    b = np.asarray(breakdown, dtype=np.float64)
    tr = b[..., 0] + b[..., 1]
    cards = b[..., 2] + b[..., 3]
    other = b[..., 4] + b[..., 5] + b[..., 6]
    total = tr + cards + other
    out = np.zeros(b.shape[:-1] + (3,), dtype=np.float64)
    nonzero = total > 0
    out[..., 0][nonzero] = tr[nonzero] / total[nonzero]
    out[..., 1][nonzero] = cards[nonzero] / total[nonzero]
    out[..., 2][nonzero] = other[nonzero] / total[nonzero]
    return out


def cell_index(shares, bins):
    """Coarse cell id for one behaviour point, or -1 when a channel is undefined.

    `bins` is the number of equal-width bins per share; the last channel is dropped because the three shares sum to one and would make
    a third of the cells impossible. Coarse on purpose: the owner's question is whether distinct strategies exist at all, and a
    descriptor that separates every game answers nothing.
    """
    s = np.asarray(shares, dtype=np.float64)
    if s.shape[-1] < 2 or not np.all(np.isfinite(s)):
        return -1
    ix = np.clip((s[:2] * bins).astype(int), 0, bins - 1)
    return int(ix[0] * bins + ix[1])


def summarise_cells(shares, scores, margins, bins):
    """Occupancy of the behaviour archive: one entry per non-empty cell.

    `shares` rows are (tr, cards, other) fractions; only the first two define the cell (the third is implied). Score and margin are
    carried along so the archive can be read as "which behaviours are strong" rather than "which behaviours exist" - the two questions
    the owner asked are one table.
    """
    shares = np.asarray(shares, dtype=np.float64)
    scores = np.asarray(scores, dtype=np.float64)
    margins = np.asarray(margins, dtype=np.float64)
    cells = np.array([cell_index(s, bins) for s in shares], dtype=np.int64)
    rows = []
    for cell in np.unique(cells):
        if cell < 0:
            continue
        mask = cells == cell
        rows.append({
            "cell": int(cell),
            "tr_bin": int(cell // bins),
            "card_bin": int(cell % bins),
            "n": int(mask.sum()),
            "score": float(scores[mask].mean()),
            "margin": float(margins[mask].mean()),
            "margin_sd": float(margins[mask].std()) if mask.sum() > 1 else 0.0,
        })
    return rows


def top_cell_share(scores, cells, percentile=75):
    """Share of the strongest game-seats that sit in the single most populated cell of that strong subset.

    The question it answers: are strong games behaviourally diverse, or is there one way to win? A share close to 100% means coverage
    would chase ghosts - there would be nothing to cover.
    """
    scores = np.asarray(scores, dtype=np.float64)
    cells = np.asarray(cells, dtype=np.int64)
    valid = cells >= 0
    if valid.sum() == 0:
        return float("nan"), 0
    cut = np.percentile(scores[valid], percentile)
    strong = valid & (scores >= cut)
    if strong.sum() == 0:
        return float("nan"), 0
    counts = np.bincount(cells[strong])
    return float(counts.max() / strong.sum()), int(strong.sum())


def load_round_grid(pack_dir, names, type_name="PICK_PHASE"):
    """The round grid: one row per seat and round (the phase pick), with the LIVE score at that row.

    Why the phase pick defines the grid: it is the only decision that happens exactly once per round per seat, so it needs no
    interpolation. The score at that row is the score at the START of the round, i.e. the END of the previous one - which is what makes
    "points created in round r" a difference of two recorded numbers rather than a guess.

    Returns a dict of arrays, keep-filtered: game, seat, round, step, live_me, live_op, margin, outcome, final (n,7 breakdown).
    """
    with open(os.path.join(pack_dir, "manifest.json"), encoding="utf-8") as handle:
        manifest = json.load(handle)
    info = manifest["types"].get(type_name)
    if not info or info["rows"] == 0:
        raise SystemExit(f"{pack_dir}: no {type_name} rows")
    rows = info["rows"]
    obs = np.memmap(os.path.join(pack_dir, f"{type_name}.obs.f16"), dtype="<f2", mode="r",
                    shape=(rows, manifest["obsSize"]))
    mine = names.index(SCORE_COLUMNS[0])
    other = names.index(SCORE_COLUMNS[1])
    live = np.asarray(obs[:, [mine, other]], dtype=np.float32)
    with np.load(os.path.join(pack_dir, f"{type_name}.lab.npz")) as labels:
        keep = labels["keep"].astype(bool)
        out = {field: np.asarray(labels[field])[keep] for field in FIELDS if field != "keep"}
    out["live_me"] = live[keep, 0]
    out["live_op"] = live[keep, 1]
    out["final"] = out.pop("breakdown").astype(np.float32)
    return out


def points_per_round(grid):
    """Per game-seat: the round numbers, the points created in each of them, and the end-game settlement.

    Definition, and it is the one the owner's 90 -> 180 example needs: the points of round r are `live(r+1) - live(r)`; the last round's
    points are `final - live(last)`, i.e. everything the game awards at the end (final card conversions, awards that settle late). That
    residual is reported separately, because "the score jumps at the end" has two very different meanings: late PLAY, or a late
    SETTLEMENT of a position that was built over the whole game.
    """
    game = np.asarray(grid["game"]).astype(np.int64)
    seat = np.asarray(grid["seat"]).astype(np.int64)
    rnd = np.asarray(grid["round"]).astype(np.int64)
    step = np.asarray(grid["step"]).astype(np.int64)
    live = np.asarray(grid["live_me"], dtype=np.float64)
    final = np.asarray(grid["final"], dtype=np.float64).sum(axis=1)
    order = np.lexsort((step, rnd, seat, game))
    game, seat, rnd, live, final = game[order], seat[order], rnd[order], live[order], final[order]
    out = []
    start = 0
    for i in range(1, len(game) + 1):
        if i == len(game) or game[i] != game[start] or seat[i] != seat[start]:
            sl = slice(start, i)
            rounds = rnd[sl]
            scores = live[sl]
            points = np.zeros(len(rounds), dtype=np.float64)
            if len(rounds) > 0:
                points[:-1] = np.diff(scores)
                points[-1] = final[sl][-1] - scores[-1]
            out.append({"game": int(game[start]), "seat": int(seat[start]),
                        "rounds": rounds, "points": points,
                        "final": float(final[sl][-1]),
                        "settlement": float(points[-1]) if len(points) else 0.0})
            start = i
    return out
