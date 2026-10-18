# Dragontrainer

A Spring Boot app that plays [Dragons of Mugloar](https://dragonsofmugloar.com) (`/api/v2`) and exposes the same game over its own HTTP API. The bot can be stepped one turn at a time, or you can play the board, the shop, and reputation yourself.

The Angular client is [DragonTrainer](../DragonTrainer/README.md). From the repository root, [`start.ps1`](../README.md) or [`start.sh`](../README.md) starts Postgres, this app, and the frontend. The same page lists the manual commands.

## Contents

1. [Running](#running)
2. [Code structure](#code-structure)
3. [What one turn does](#what-one-turn-does)
4. [Difficulties](#difficulties)
5. [Bot tactics](#bot-tactics)
6. [HTTP API](#http-api)
7. [Design decisions](#design-decisions)
8. [Tests](#tests)

## Running

Java 25. The Gradle Wrapper is in the repo. On Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

The default profile is `dev`. The database is Postgres, started from `compose.yaml`. Connection settings come from `.env` (see `application-dev.properties`: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, and optionally `DB_HOST` and `DB_PORT`). The game base URL is `https://dragonsofmugloar.com` unless `GAME_API_BASE_URL` overrides it.

`game.bot.enabled` is `false`. `bootRun` does not start a game on its own, so it does not spend lives. A full automatic game runs only when `GAME_BOT_ENABLED=true`. Then `BotStartupRunner` starts one game and `BotGameService.play` runs it to the end. The normal path is HTTP: the client starts a game and asks the bot for one turn at a time.

Tests do not need Postgres or the live game. They use H2 and a local mock server.

```powershell
.\gradlew.bat test
```

## Code structure

Everything lives in the package `ee.dragon.dragontrainer`.

| Package | What is there |
| --- | --- |
| `api` | `GameController` and the response records. This is the only place that speaks HTTP. |
| `services` | Game steps: start a game, fetch the board, solve, buy, check reputation, take one bot step, rebuild the current state. |
| `bot` | `BotStartupRunner` and `BotProperties`. The runner starts only when the bot is enabled. |
| `bot.policy` | The decision for the current board. `BotTurnPolicy` is the policy the bot actually uses. |
| `gameclient` | Client for the external game. `GameApiClient` is the interface, `HttpGameApiClient` makes the calls. DTOs are under `gameclient.dto`. |
| `model` | JPA entities: game, task, turn, shop item, purchase, reputation. |
| `repository` | Spring Data repositories for those entities. |
| `config` | Bot settings, game API settings, CORS for `localhost`. |

The flow goes one way. The controller asks a service. The service either asks the policy for a decision or asks `GameApiClient` for a game response, then stores the result. The policy knows nothing about HTTP or the database. That is why `BotTurnPolicy` can be tested with plain JUnit, without Spring.

`GameSession` stores the opening state: the external game id, lives, gold, level, score, high score, and turn. Later purchases and solves do not overwrite that row. `GameProgressService` rebuilds the current state from the opening row plus turns and purchases in turn order. The reason is in [Design decisions](#design-decisions).

## What one turn does

`BotGameService.step` does exactly one thing and returns.

1. It reads the state from the database: lives, gold, level, score, consecutive failures, and how many times each piece of gear has been bought.
2. If lives are 0, it returns `STOP` and does not call the game.
3. It fetches the board (`/messages`) and the shop (`/shop`). Neither call spends a turn.
4. `BotTurnPolicy.decide` says whether to solve, buy, skip, or stop.
5. A solve goes to `/solve/{adId}`. A buy goes to `/shop/buy/{itemId}`. A skip is the reputation call `/investigate/reputation`, because that spends one turn in the game and leaves the tasks untouched.

`play` calls `step` until the result is `finished`. An HTTP client does the same thing itself, one request at a time, so each turn's text can be shown on screen.

## Difficulties

The game sends difficulty in the `probability` field. `TaskDifficulty.fromMessage` looks for a known phrase (`contains`, lower case). `Sure thing!` lands on rank 1 because the phrase `sure thing` is inside it. If `probability` is blank, the task's own text is used. A probability that still matches nothing stays `UNKNOWN`. Its rank is `Integer.MAX_VALUE`, so the normal picker never takes it.

A board mixes plain ads and ciphered ones. Plain ads have `encrypted` null. Ciphered ads set `encrypted` on that ad only, and the same three fields are encoded: `adId`, `message`, and `probability`. `reward` and `expiresIn` stay numbers.

`encrypted` `1` is Base64. The live game sends standard Base64, sometimes without `=` padding. `Hmmm....` arrives as `SG1tbS4uLi4=`. A name such as Olegário is UTF-8 inside that Base64, not a second cipher. `Base64Ad` decodes with the standard decoder, and falls back to the URL-safe decoder when the alphabet uses `-` and `_`. Whitespace is stripped first. If the bytes are not UTF-8 text, or the text is blank or contains a control character, the original value is kept. A field that is not actually Base64, such as plain `Sure thing` marked `1` by mistake, is also kept.

`encrypted` `2` is ROT13. Letters rotate by 13. Digits, spaces, and punctuation stay put. `Fher guvat` opens to `Sure thing`.

`Base64Ad.open` runs while `MessageResponse` is built, before the task is stored. Difficulty is then read from the plain probability, and the stored id is the one `/solve` accepts. The ciphertext id gets 400 from the game. The bot and the Angular board both read the stored task, so both see the plain title, the real difficulty, and the id that can be solved. Leaving either cipher closed is what put Base64 on the parchment and labelled the ad `Unknown`.

| Rank | Name in code | Phrase in the game |
| --- | --- | --- |
| 1 | `SURE_THING` | Sure thing |
| 2 | `WALK_IN_THE_PARK` | Walk in the park |
| 3 | `PIECE_OF_CAKE` | Piece of cake |
| 4 | `QUITE_LIKELY` | Quite likely |
| 5 | `GAMBLE` | Gamble |
| 6 | `HMM` | Hmmm.... |
| 7 | `RATHER_DETRIMENTAL` | Rather detrimental |
| 8 | `RISKY` | Risky |
| 9 | `PLAYING_WITH_FIRE` | Playing with fire |
| 10 | `SUICIDE_MISSION` | Suicide mission |
| 11 | `IMPOSSIBLE` | Impossible |

For the bot, a normal task is rank 1 through 5. Gear is bought once rank 1 through 3 is missing from the board. With one life, only rank 1 is allowed.

## Bot tactics

The decision always runs in this order. The first branch that returns something wins. Later rules do not run on that turn.

### The game is over

Zero lives or fewer returns `STOP`. The game is not called. A reputation request on a dead game answers 410 and used to crash the process, so this check sits at the front.

### One life

With one life the bot solves only a real `Sure thing`. If there is none and a healing potion (`hpot`) is affordable, it buys the potion. Harder tasks, ranks 2 through 5, are left alone at one life.

This came from a game where the bot sat on one life and kept solving `Walk in the park` until it died. One life is too thin for that.

`Steal super awesome` does not count as rank 1 here, even when the game labels it `Sure thing`. That message is a trap, described below. If the board is only that trap, the bot buys a potion at one life when it has the gold. If it does not, the decision falls through to the forced board and the broke `Impossible` rule.

The 150 gold reserve does not apply to the potion. A potion costs 50 in the game and may be bought with the last gold. The reserve is only for the expensive items.

### Forced board

A board is forced when it is not empty and every task is either `Impossible` or its message contains `steal super awesome`. Case does not matter. One `Risky`, one `Suicide mission`, or any other ordinary task keeps this branch from running.

A forced board is never skipped. A skip leaves the same tasks in place, and the bot used to sit in an endless loop of reputation calls. Those tasks leave the board only after an attempt.

At two or more lives this is checked before gear and before the normal pick.

- If the previous solve failed (`consecutiveFailures >= 1`), lives are under 3, and a potion is affordable: buy one potion.
- Otherwise take the first `Impossible` in the list that is not itself a trap.
- If there is no `Impossible`: the first trap of rank 1. If there is none, the first trap of any rank.

First means the order the game returned. A bigger reward does not make these attempts safer, so the list is not sorted.

The potion is bought after a failure. Gold is not spent before the first attempt. A successful potion resets the failure counter, so the next turn is a new attempt. At three lives no potion is bought. The observed cap is 3, and a buy at full lives fails. A failed purchase stops the bot.

At one life this same branch is reached only when a potion cannot be afforded. The task is attempted immediately, because a skip would leave the same board up.

### Gear

At two or more lives, when no playable task of rank 1 through 3 remains, the bot buys a level item. Five items raise the level, each costing 300: `iron`, `wingpotmax`, `mtrix`, `ch`, `rf`. Every successful buy, including a repeat, has given +2 level. The bot does not compute the level. It buys the item and the game reports the new level.

The buy order is that same list. The id with the fewest purchases so far is chosen. Ties go to the earlier id in the list. When all five have been bought the same number of times, the next round starts at `iron` again.

An item is bought only when at least 150 gold would remain (`cost + 150 <= gold`). A 300 item therefore needs 450. 150 is three potions. Without that reserve the bot bought a level and died on the next failure, because no potion was left.

If the next item in the rotation is in the shop but the gold plus the reserve is short, the bot does not fall through to a cheaper later item. One expensive item would otherwise break the rotation and the cheaper ones would pile up. If that id is missing from the shop entirely, the next id with the same purchase count is considered.

`hpot` is not in this list. It does not raise the level.

Gear is not bought while a real task of rank 1 through 3 is on the board. An easy task scores immediately. The level can wait.

### A normal task

At two or more lives, once the forced board and the gear check have passed, the bot solves the best task of rank 1 through 5.

Inside the same rank:

1. If any task does not start with `steal`, the tasks that do start with `steal` are dropped.
2. Among what remains, the highest `reward` wins.

`Steal ... and share some of the profits with the people` is a normal task and can succeed. A successful one raised people by 1 and dropped state by 2 in the logs. A defense or escort task of the same rank did not move reputation. So a task whose text does not start with `steal` is preferred when both are equally easy. If every task at that rank is a steal, the one with the highest reward is taken.

Ranks 6 through 10 (`Hmmm` through `Suicide mission`) stay out of the normal pick. They are not attempted just in case. If nothing else fits, the turn is a skip, or the broke rule below.

### Two lives and no normal task

If lives are exactly 2, consecutive failures are under 3, and a potion is affordable, the bot buys a potion. This is for a board with no rank 1 through 5 that is also not forced. Example: only `Risky` and `Playing with fire`.

After three failures that potion buy stops. Otherwise gold would drain into potions on a board the bot does not want to solve. The forced board is separate and still buys one potion after a failure, until lives are 3.

### Out of gold

If no earlier branch chose anything and a healing potion cannot be afforded, the bot takes the first `Impossible`, even when the board also has `Risky` or something else. Without this the bot would skip as soon as gold dropped below the potion price, and the `Impossible` tasks would stay forever.

If a potion is affordable, this branch does not run. The turn is a skip, or a potion buy when lives are 2 and there have been few failures.

### Skip

If nothing above fits, the decision is `SKIP`. The service calls reputation and stores people, state, and underworld. That spends one turn in the game. Lives, gold, and score stay the same.

Skip is the right place for ranks 6 through 10 and for an unknown difficulty, when there is still gold for a potion. Those tasks are not worth solving, and a skip moves the turn forward until the board changes.

If that reputation check is still the latest action and the stored board contains an `Impossible` task, the next step does not fetch the messages again. It attempts that `Impossible` task. Another fetch of the same dead board is what left the bot waiting.

### Traps

A trap is a task whose text contains `steal super awesome`. The game has sent these labeled `Sure thing`, and sometimes `Piece of cake`, with a large reward. Solving one paid no gold, cost one life, and answered that people did not appreciate those dealings.

The normal picker skips them at every rank. Only on a forced board, when there is no `Impossible`, does the bot attempt a trap. Otherwise those tasks would sit on the board forever.

An ordinary `Steal ... share some of the profits` is not a trap. It starts with `steal`, and it does not contain `steal super awesome`.

### The failure counter

`GameProgressService` replays turns and purchases in turn-number order.

- A successful solve sets the counter to 0.
- A failed solve adds one.
- A successful `hpot` sets the counter to 0.
- A gear purchase leaves the counter as it is.

Resetting on the potion is what makes a forced board buy one potion per failure and then try again. If the counter stayed high, the bot would buy potions until lives hit 3 and only then attempt the task.

### Expiry

The bot ignores `expiresIn`. Taking a harder task only because it expired sooner cost a life, and a random pick among hard tasks is what used to end games. The live tactics are in `BotTurnPolicy`.

## HTTP API

The base path is `/api/games`. CORS allows `http://localhost` and `http://127.0.0.1` on any port, so an Angular app on another port can call in. The `id` in the path is our database UUID. The game's own id is the `gameId` field in the response.

| Method and path | What it does |
| --- | --- |
| `POST /api/games` | Starts a game. Response 201, with lives, gold, level, score, high score, and turn. |
| `GET /api/games/{id}` | Current state. `reputation` is the latest check, once one has been made. Otherwise the field is absent. |
| `GET /api/games/{id}/tasks` | A fresh board. Does not spend a turn. |
| `POST /api/games/{id}/tasks/{adId}/solve` | Solves that task. Spends a turn. |
| `GET /api/games/{id}/shop` | The shop list. Does not spend a turn. |
| `POST /api/games/{id}/shop/{itemId}` | A purchase. Spends a turn. |
| `POST /api/games/{id}/reputation` | Reputation. Spends a turn. |
| `POST /api/games/{id}/bot/step` | One bot turn. |

Fields on `bot/step` that a screen needs:

- `action`: `SOLVE`, `BUY_GEAR`, `BUY_HEALING_POTION`, `SKIP`, or `STOP`.
- `text`: the line for the middle of the screen. A solve includes the task text and the game's reply. A buy names the item. A skip says `No normal task.` The end says `Game over.`
- `score`, `gold`, `lives`: for the header, on every turn.
- `reputation`: only on `SKIP`. Then it has `people`, `state`, and `underworld`.
- `finished`: `true` when lives hit 0, a purchase failed, or the game was already over.

`GET /api/games/{id}` uses the same state rebuild as the bot. Gold on the original `GameSession` row stays the gold from the start of the game. The header uses the latest solve or purchase.

## Design decisions

**The policy is a pure function.** `decide` receives lives, gold, failures, the shop, the purchase counts, and the board. It does not read the database or make HTTP calls. A rule can be changed and tested in one class. Side effects (the request, the save, avoiding 410) live in `BotGameService`, which is already the place that talks to the game.

**State is not kept in memory between turns.** HTTP steps arrive as separate requests, and the process can restart in between. Lives, gold, score, level, failures, and gear counts are read back from stored turns and purchases. A map held in memory would be wrong after a restart and broken between two requests.

**`GameSession` stays the opening snapshot.** Every solve and purchase brings new totals from the game, and those totals are already on the `game_turns` and `shop_purchases` rows. A second writable copy on the session would drift from them as soon as one save failed. Reading one history is more code, and the number is always the one the game reported last.

**Responses are records.** `spring.jpa.open-in-view` is off. An entity has a lazy link to the game, and Jackson would touch it after the transaction. The controller copies the fields itself. `reputation` is left out of the JSON when this turn did not check it, so the client can refresh the header on a skip.

**A skip is a reputation call.** The board changes when a turn is spent. Reputation is how this project spends a turn while leaving every task unsolved. The same response supplies people, state, and underworld for the header. Reputation is no longer checked after buying gear. That call existed only to write a log line, and it spent a turn for nothing.

**The bot is off by default.** `bootRun` during development should not start a game by itself. Lives and network calls belong either to the test mock or to an HTTP step a person asked for. `GAME_BOT_ENABLED=true` remains for a full game from the command line.

**Ciphered ads are opened when the message is read.** `encrypted` `1` is Base64 and `encrypted` `2` is ROT13, on the id, the text, and the probability. Opening them in `MessageResponse` keeps that rule in one place. `TaskService` stores the plain task, and both a manual solve and a bot step use that id. The live API returns the board as a JSON array, not wrapped in `messages`, and `GameApiJson` accepts both shapes.

**Tests do not call the live game.** Each integration test starts a `MockWebServer` and points `game.api.base-url` at it. The real API is slow, stateful, and spends lives. The mock queue is part of the test: the bot asks for the board and then the shop, so the queue has to be messages, shop, then solve or buy or reputation. `@Transactional` rolls the H2 data back at the end of the test.

**Hibernate `ddl-auto=update` on the dev database.** The schema grows from the entities. There is no separate migration. A new enum constant on the `tasks` table can get stuck on Postgres's check constraint, because `update` does not widen that constraint. Tests avoid this: H2 rebuilds the schema with `create-drop`.

## Tests

The whole suite:

```powershell
.\gradlew.bat test
```

One class:

```powershell
.\gradlew.bat test --tests ee.dragon.dragontrainer.bot.policy.BotTurnPolicyTest
```

One method:

```powershell
.\gradlew.bat test --tests ee.dragon.dragontrainer.bot.policy.BotTurnPolicyTest.decide_solvesSureThingAtOneLifeInsteadOfHealing
```

The test profile comes from `@ActiveProfiles("test")`. No extra Gradle flag is required. The profile switches on H2, turns Docker Compose off, and keeps the bot disabled. Each integration class overrides the external game URL with its own mock server.

| Test class | What it covers |
| --- | --- |
| `BotTurnPolicyTest` | All of the tactics, with no Spring and no network. Gear rotation and the 150 reserve, one life limited to rank 1, two lives covering ranks 1 through 5, skipping the trap, keeping an ordinary steal, the forced board, a potion after a failure, `Impossible` when gold is gone, skip when no normal task remains. |
| `BotGameServiceIntegrationTest` | `play` with the real services and a mocked game. Iron before the other items, a potion when no easy task remains, a potion at one life in place of a harder task, skip plus reputation, the `Walk in the park` beside a trap, and a Base64 sure thing solved with its decoded id. |
| `GameControllerIntegrationTest` | HTTP. A bot step returns the solve text, score, and gold. A skip adds reputation. Manual play: board, solve, shop, buy, reputation, and `GET` shows the updated state afterwards. |
| `GamePlaythroughIntegrationTest` | One session runs start, board, solve, shop, buy, and reputation in that order. |
| `GameSessionServiceIntegrationTest` | A new game is stored from the external API response. |
| `TaskServiceIntegrationTest` | The board is stored together with its difficulty. A Base64 ad is stored as plain text, the decoded id, and the difficulty from the decoded probability. |
| `GameTurnServiceIntegrationTest` | A solve is stored. The same session can have several turns. |
| `ShopServiceIntegrationTest` | The shop list and a purchase are stored. |
| `ReputationServiceIntegrationTest` | A reputation check is stored. |
| `Base64AdTest` | Base64 ads open to text, including a live ad whose probability is `Hmmm....` and whose message contains `Olegário`. Unpadded Base64 opens. ROT13 (`encrypted` 2) rotates letters only. Plain text and bytes that are not text stay as sent. |
| `DragontrainerApplicationTests` | The context starts on the test profile. |

Policy tests are fast, and a new rule goes there first. An integration test is added when the rule makes a request or writes to the database. The controller test checks that the fields a screen reads (`text`, `score`, `gold`, `reputation`) are actually in the JSON.
