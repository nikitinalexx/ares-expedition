# Ревизия признаков наблюдения

## Что решено по итогам ревизии

1. **Кодирование V2 в целом оправдано**: констант нет, «мёртвых» карточных слотов нет, выходов за диапазон нет (кроме моего же `round/40`), линейных копий нет. Выбрасывать «на глаз» нечего.
2. **210 из 898 колонок — точные копии** других (вектор «рука» заново кодирует вехи, награды, теги и эффекты сыгранных карт, которые есть в «столе»). Они **убраны** в наблюдении v3 (`ObservationBuilder`, маска `src/main/resources/observation/v3_mask.json`,
   сгенерирована `ml/audit_features.py --write-mask`). Тест `ObservationMaskTest` на живых партиях проверяет, что убранные колонки остаются точными копиями оставшихся. NETWORK_V2 этого не касается: он использует прежний вектор.
3. Добавлен **блок контекста раунда** (38 значений): чьи фазы выбраны (я, соперник, dummy), текущая фаза, доступные постройки, дополнительные активации синих карт, модификаторы набора карт, число активированных синих карт.
   Выбор текущего раунда **скрыт, пока фаза выбирается** (одновременный выбор), и виден внутри раунда — оба условия проверяет `ObservationCompletenessTest`.
4. 108 признаков без линейной связи с исходом **не удалялись**: связь может быть нелинейной. Это список для проверки важности по обученной модели (перестановочная важность), когда будет что обучать.
5. Модель сама стандартизирует входы по данным обучения, поэтому разные максимумы нормализации V2 ей не мешают.

Данные: 66714 состояний из 4 чанков (самоигра, разные контексты), признаков: 915. Сгенерировано `ml/audit_features.py`.

## Итог

| что | сколько из 915 |
|---|---|
| константы (всегда одно значение) | 0 |
| почти константы (одно значение в >99,9% состояний) | 9 |
| точные копии другого признака (лишних) | 210 в 210 группах |
| линейные копии (|r| ≥ 0,999) сверх точных | 0 пар |
| выходят за [0, 1] (неверный максимум нормализации) | 1 |
| используют <10% диапазона (максимум нормализации сильно завышен) | 2 |
| «мёртвые» карточные слоты (карта никогда не встретилась) | 0 |
| нет даже линейной связи с исходом (|r| < 0,003) среди меняющихся | 108 из 915 |

Модель стандартизирует входы по данным, поэтому масштаб ей не мешает; вред от неверного максимума (значения > 1) и от копий — потеря ёмкости и ложная уверенность в «независимых» признаках.

## По блокам

| блок | признаков | константы | точные копии (лишние) | вне [0,1] | нет связи с исходом |
|---|---|---|---|---|---|
| table: global / own player | 181 | 0 | 0 | 0 | 11 |
| milestones & awards | 58 | 0 | 0 | 0 | 16 |
| opponent table (op_*) | 178 | 0 | 0 | 0 | 8 |
| hand vector: global & player numbers | 117 | 0 | 114 | 0 | 20 |
| hand vector: card identities in the hand / on the table | 364 | 0 | 96 | 0 | 40 |
| extras (round, phases, dummy) | 17 | 0 | 0 | 1 | 13 |

## Константы (0)


## Точные копии (210)

- `oxygen_left` — то же самое: `hand:oxygen_left_ratio`
- `temperature_left` — то же самое: `hand:temperature_left_ratio`
- `oceans_left` — то же самое: `hand:oceans_left_ratio`
- `milestone_DIVERSIFIER_isInGame` — то же самое: `hand:milestone_DIVERSIFIER_isInGame`
- `milestone_DIVERSIFIER_myProgress` — то же самое: `hand:milestone_DIVERSIFIER_myProgress`
- `milestone_DIVERSIFIER_opponentProgress` — то же самое: `hand:milestone_DIVERSIFIER_opponentProgress`
- `milestone_DIVERSIFIER_isAustellarPick` — то же самое: `hand:milestone_DIVERSIFIER_isAustellarPick`
- `milestone_ENERGIZER_isInGame` — то же самое: `hand:milestone_ENERGIZER_isInGame`
- `milestone_ENERGIZER_myProgress` — то же самое: `hand:milestone_ENERGIZER_myProgress`
- `milestone_ENERGIZER_opponentProgress` — то же самое: `hand:milestone_ENERGIZER_opponentProgress`
- `milestone_ENERGIZER_isAustellarPick` — то же самое: `hand:milestone_ENERGIZER_isAustellarPick`
- `milestone_FARMER_isInGame` — то же самое: `hand:milestone_FARMER_isInGame`
- `milestone_FARMER_myProgress` — то же самое: `hand:milestone_FARMER_myProgress`
- `milestone_FARMER_opponentProgress` — то же самое: `hand:milestone_FARMER_opponentProgress`
- `milestone_FARMER_isAustellarPick` — то же самое: `hand:milestone_FARMER_isAustellarPick`
- `milestone_LEGEND_isInGame` — то же самое: `hand:milestone_LEGEND_isInGame`
- `milestone_LEGEND_myProgress` — то же самое: `hand:milestone_LEGEND_myProgress`
- `milestone_LEGEND_opponentProgress` — то же самое: `hand:milestone_LEGEND_opponentProgress`
- `milestone_LEGEND_isAustellarPick` — то же самое: `hand:milestone_LEGEND_isAustellarPick`
- `milestone_MAGNATE_isInGame` — то же самое: `hand:milestone_MAGNATE_isInGame`
- `milestone_MAGNATE_myProgress` — то же самое: `hand:milestone_MAGNATE_myProgress`
- `milestone_MAGNATE_opponentProgress` — то же самое: `hand:milestone_MAGNATE_opponentProgress`
- `milestone_MAGNATE_isAustellarPick` — то же самое: `hand:milestone_MAGNATE_isAustellarPick`
- `milestone_PLANNER_isInGame` — то же самое: `hand:milestone_PLANNER_isInGame`
- `milestone_PLANNER_myProgress` — то же самое: `hand:milestone_PLANNER_myProgress`
- `milestone_PLANNER_opponentProgress` — то же самое: `hand:milestone_PLANNER_opponentProgress`
- `milestone_PLANNER_isAustellarPick` — то же самое: `hand:milestone_PLANNER_isAustellarPick`
- `milestone_SPACE_BARON_isInGame` — то же самое: `hand:milestone_SPACE_BARON_isInGame`
- `milestone_SPACE_BARON_myProgress` — то же самое: `hand:milestone_SPACE_BARON_myProgress`
- `milestone_SPACE_BARON_opponentProgress` — то же самое: `hand:milestone_SPACE_BARON_opponentProgress`
- … ещё 180

## Выходят за [0, 1] (1)

- `extra:round/40` — от 0.0249939 до 1.02539

## Используют <10% диапазона (2)

- `birds_animal_count` — 99,9-й перцентиль 0.000, максимум 0.217
- `op_birds_animal_count` — 99,9-й перцентиль 0.000, максимум 0.217

## Сильнее всего связаны с исходом (линейно) (25)

- `op_win_points` — |r| = 0.153
- `win_points` — |r| = 0.153
- `table_played_energy` — |r| = 0.120
- `op_table_played_energy` — |r| = 0.120
- `hand:played_tag_energy` — |r| = 0.120
- `op_mc_income` — |r| = 0.119
- `mc_income` — |r| = 0.119
- `heat_income` — |r| = 0.117
- `op_heat_income` — |r| = 0.117
- `op_table_played_science` — |r| = 0.113
- `table_played_science` — |r| = 0.113
- `hand:played_tag_science` — |r| = 0.113
- `max_cards_per_card` — |r| = 0.110
- `op_max_cards_per_card` — |r| = 0.110
- `card_income` — |r| = 0.109
- `hand:card_income` — |r| = 0.109
- `op_card_income` — |r| = 0.109
- `max_mc_per_card` — |r| = 0.104
- `op_max_mc_per_card` — |r| = 0.104
- `max_heat_per_card` — |r| = 0.103
- `op_max_heat_per_card` — |r| = 0.103
- `table_played_building` — |r| = 0.101
- `op_table_played_building` — |r| = 0.101
- `hand:played_tag_building` — |r| = 0.101
- `table_played_earth` — |r| = 0.097
