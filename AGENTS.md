# AGENTS.md — ares-expedition

Онлайн-реализация настольной игры *Terraforming Mars: Expedition Ares* (Spring Boot 3 + PostgreSQL, фронтенд на Angular в `frontend/`),
с компьютерными игроками разной силы. Продакшен — Heroku, автодеплой при пуше ветки `main`.

## Сборка и запуск
- Нужна **JDK 21+** (`pom.xml`: `release 21`). На JDK 25 `mvn compile` падает на Lombok (javac 23+ по умолчанию отключает annotation processing) —
  используй JDK 21 или добавь `<proc>full</proc>` в maven-compiler-plugin.
- Если глобальный `~/.m2/settings.xml` указывает на сторонний репозиторий, используй проектный `.mvn/settings.xml`
  (только Maven Central): `mvn -s .mvn/settings.xml compile`.
- БД: PostgreSQL (docker-команда в `README.md`), Spring-профиль `local` (`application-local.properties`). Heroku — `application-prod.properties`.
- Heroku берёт Java из `system.properties` (сейчас 21). Прод/Heroku сейчас вне объёма работ — сначала сильный локальный ИИ.
- Фронтенд: `cd frontend && ng serve`.

## Структура кода (`src/main/java/com/terraforming/ares`)
`cards` (карты по цветам и корпорации, `buffed*` — усиленные версии), `model` / `mars` (состояние игры), `processors` / `states` / `services`
(правила и переходы фаз), `validation` (валидация ввода игрока), `controllers/GameController` (REST + симуляции), `entity`/`repositories` (БД).

## Компьютерные игроки (`model/PlayerDifficulty`, `services/ai`)
| Difficulty | Что это |
|---|---|
| `RANDOM`, `SMART` | простые эвристики (`AiService`, `FileCardValueService`, `turnProcessors`) |
| `NETWORK` | старая сеть (`DeepNetwork` + `services/ai/network`, самописный feed-forward). Веса: `1701242492845_epoch1_result_N_reg4_smart_1.dnet_epoch_1.txt`. **Это то, что сейчас на проде в `main`.** |
| `NETWORK_EXPERIMENT` | вариант `NETWORK` |
| `NETWORK_V2` | новая, заметно более сильная (только в ветке `advanced_ai`). Код в `services/ai/network2` |

### NETWORK_V2
- Это **value-сеть**: по вектору состояния игры возвращает вероятность победы (0..1). ИИ перебирает/проецирует возможные ходы
  (`network2/*Projector`, `projection/`, `buildParams/`), кодирует каждое получившееся состояние и выбирает ход с максимальной вероятностью.
- Кодирование состояния: `services/ai/advanced` (`CompleteTableEncoder` + `CompleteHandEncoder`, `features/`). Вектор = `[table(417) | hand(481)]`,
  размеры — `AiConstants.TABLE_VECTOR_SIZE/HAND_VECTOR_SIZE`. **Меняя фичи, ты ломаешь совместимость с обученными весами** — нужно переобучение.
- Инференс: `services/ai/dl4j/NNService` → `PureJavaValueNetwork`. Это **чистая Java, без CUDA/ND4J/DL4J**, работает на любом CPU (локально и на Heroku).
  Читает обычный DL4J-zip (`coefficients.bin`) напрямую. Архитектура зашита в `NNService` (константы `DENSE_*`) и проверяется по числу параметров
  при загрузке; при смене архитектуры в обучении — обнови константы/класс.
- Модель сейчас: `iter4_epoch_2_93.zip` в корне репозитория (для FIRST и SECOND бота — одна и та же). Остальные `*.zip`, `251_win/`, `epoch2/`, `epoch3/` —
  архив промежуточных чекпоинтов. Они в git и уходят в slug Heroku — не раздувай (лимит slug 500 МБ).
- Граф: `tableInput(417)→Dense512(LReLU)` и `handInput(481)→Dense512(LReLU)` → concat → `Dense1024(LReLU, dropout)` → `Dense1(sigmoid)`.
- Подвох: ~70% весов < 1e-6 (L2), 4% — субнормальные float; арифметика на них в 30–50× медленнее, поэтому `PureJavaValueNetwork` обнуляет их при загрузке
  (расхождение с DL4J < 1e-6). Не убирай это.
- Скорость: ~0.7 мс на 1 состояние, ~18 мс на 300 состояний. Для пошаговой игры с 2–3 игроками более чем достаточно.

## Обучение (отдельный проект `deep`, лежит рядом с этим репозиторием)
Здесь (в `ares-expedition`) обучения нет. Пайплайн:
1. Самоигра/симуляции в этом приложении (`GameController`: `/simulations`, `/simulations/v2`, `/simulations/v3`; `services/simulations`,
   `AdvancedAiDataCollectionService`) пишет датасет `sim/full/<timestamp>/dataset_N.bin` (строки `features + label`, FLOAT16 big-endian, `DatasetWriter`).
2. Проект `deep` (DL4J + CUDA, пакет `learning.dj4`: `fast/FastTrainer`, `FastDatasetBuilder`, `policy/*`) обучает `ComputationGraph` и сохраняет zip
   через `ModelSerializer`. Папки `NNN_win/` содержат `config` (гиперпараметры + метрики обучения) и итоговый zip.
3. Новый zip кладётся в корень `ares-expedition`, имя прописывается в `NNService.FIRST_MODEL/SECOND_MODEL`.
Старые сети (`NETWORK`) учились кодом из пакетов `network` и `learning/classification` проекта `deep`.
Для **обучения** CUDA нужна; для **игры** — нет.

## Правила работы
- Прод — `main` → Heroku. Не пушь в `main` и не меняй `system.properties`/pom без явной просьбы.
- Не добавляй ND4J/DL4J/CUDA обратно в `pom.xml` приложения: Heroku без GPU, а инференс уже не требует этих библиотек.
- Тестов в репозитории практически нет; проверяй правки компиляцией (JDK 21) и, для AI, симуляциями/партиями.
- Секреты (`.mvn/settings.xml`, application-*.properties) не светить в выводе и коммитах.

## Тесты и документы
- `mvn test` — тесты движка без БД (`EngineTestBase` поднимает контекст с заглушками репозиториев): `GameInvariantsTest` (инварианты правил на каждом шаге случайных партий),
  `CopyFidelityTest` (копия игры = оригинал и не делит изменяемые объекты), `ShuffleServiceTest`. Известные открытые проблемы перечислены в `GameInvariantsTest.KNOWN_ISSUES` и `docs/BUGS.md`.
- `docs/GAME_RULES.md` — правила, восстановленные по коду; `docs/AI_PLAN.md` — план нового ИИ; `docs/BUGS.md` — найденные баги.
- Сравнение сил: `GET /simulations/compare?games=...&a=NETWORK&b=NETWORK_V2` и `GET /simulations/compare/models?dir=candidates&games=100` (чередуют места, дают доверительный интервал).
- Режим инференса: `-Dares.nn.backend=java|dl4j|verify` (`dl4j`/`verify` требуют сборки `-Pdl4j-reference,dl4j-cuda` или `dl4j-cpu`).
