"""The one number that says whether a critic is worth measuring: what it pays for an income stream.

Reads the JSON of `--mode audit` and prints, for the perturbations that matter, the price in MC (using the +10 MC
perturbation as the exchange rate), the round profile, and the symmetry check. The rules make +1 income worth about
15 MC at the median round (round 8 of ~23) and less as the game shortens.

    py -3 ml/price_report.py D:/ares/tmp/audit.json
"""
import json
import sys


def price(by_round, mc_per_point, rows):
    result = {}
    for band, value in (by_round or {}).items():
        result[band] = round(value / mc_per_point, 1) if mc_per_point else None
    return result


def main():
    report = json.load(open(sys.argv[1], encoding='utf-8'))
    rows = {p['perturbation']: p for p in report['perturbations']}
    mc = rows.get('mc +10', {}).get('meanDelta')
    if not mc:
        raise SystemExit('the audit has no "mc +10" row: cannot compute an exchange rate')
    mc_per_unit = mc / 10.0
    income = rows.get('income mcIncome +1', {})
    income_value = income.get('meanDelta')
    line = [f"income +1 = {income_value / mc_per_unit:.1f} MC" if income_value is not None else "income row missing",
            f"by round {price(income.get('byRound'), mc_per_unit, rows)}",
            f"mc+10 {mc:+.4f}"]
    tr = rows.get('tr +1', {}).get('meanDelta')
    if tr is not None:
        line.append(f"tr+1 = {tr / mc_per_unit:.1f} MC")
    point = rows.get('extraPoint +1', {}).get('meanDelta')
    if point is not None:
        line.append(f"point = {point / mc_per_unit:.2f} MC-equivalent (1 point should be {1 / (mc_per_unit * 20):.1f} MC at 20-point scale)")
    calibration = report.get('calibration', {})
    line.append(f"V(me)+V(opp) {calibration.get('median_V_me_plus_V_opp')}")
    print('; '.join(line))


if __name__ == '__main__':
    main()
