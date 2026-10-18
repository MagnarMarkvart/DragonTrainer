# DragonTrainer

A fullstack client for [Dragons of Mugloar](https://dragonsofmugloar.com). The backend plays the game and stores each session. The Angular app is the board: play it yourself, or watch the bot.

## What you need

- Java 25
- Node.js 20.19+, 22.12+, or 24, plus npm 11. This project was run on Node 24.12 and npm 11.7.
- Docker Desktop, for Postgres 16

## Score

Bot games against the live API finish around 3200 points. Playing the board by hand can match that or go past it. The assignment's 1000 point bar is the policy in `BotTurnPolicy`.

## Tests

Backend, from `dragontrainer-bigbank`:

```powershell
.\gradlew.bat test
```

```bash
./gradlew test
```

Frontend, from `DragonTrainer`:

```bash
npm test
```

## Two parts:

- [dragontrainer-bigbank](dragontrainer-bigbank/README.md) is the Java Spring Boot backend. It plays Dragons of Mugloar, stores each game in Postgres, and exposes `/api/games`. Ads the game sends as Base64 or ROT13 are decoded before they are stored, so the bot and the frontend both see the plain title, difficulty, and id.
- [DragonTrainer](DragonTrainer/README.md) is the Angular frontend. Start a game, continue it in the same browser, or watch the bot.

## Stack

### Backend

`dragontrainer-bigbank` is a Java 25 Spring Boot 4.1.1 app, built with Gradle. The HTTP API is Spring Web MVC. Persistence is Spring Data JPA. The `dev` profile uses Postgres 16 from `compose.yaml` (container `dragontrainer-postgres`, database `dragontrainer`). Tests use H2 and a local MockWebServer, so they do not call the live game or Postgres.

The external game is [Dragons of Mugloar](https://dragonsofmugloar.com) `/api/v2`. `HttpGameApiClient` is a `RestClient` pointed at `GAME_API_BASE_URL` (default `https://dragonsofmugloar.com`). Our own API is `/api/games`. CORS allows `GET` and `POST` from `http://localhost:*` and `http://127.0.0.1:*`.

Code is the package `ee.dragon.dragontrainer`:

- `api` — `GameController` and the response records. This is the only HTTP surface.
- `services` — start a session, fetch and store the board, solve, buy, check reputation, take one bot step, and rebuild the current lives, gold, score, level, and turn.
- `bot.policy` — `BotTurnPolicy.decide` is a pure function of the board, the shop, and the current numbers. It does not touch HTTP or the database.
- `gameclient` — the Mugloar client and its DTOs.
- `model` and `repository` — JPA entities and Spring Data repositories.

`POST /api/games` starts a Mugloar game and stores a `GameSession`. That row is the opening snapshot (external `gameId`, lives, gold, level, score, high score, turn). Later solves and purchases are their own rows. `GameProgressService` rebuilds the current state from the snapshot plus those rows. The id the browser keeps is this session UUID, not the Mugloar `gameId`.

The board is `GET /api/games/{id}/tasks`. A solve is `POST /api/games/{id}/tasks/{adId}/solve`. The shop is `GET /api/games/{id}/shop` and `POST /api/games/{id}/shop/{itemId}`. Reputation is `POST /api/games/{id}/reputation`. One bot turn is `POST /api/games/{id}/bot/step`. `GAME_BOT_ENABLED` is false, so `bootRun` does not play by itself. The page asks for one step at a time.

Difficulty is the Mugloar `probability` phrase, matched by `TaskDifficulty.fromMessage`. A single board can mix plain ads with ciphered ones. `encrypted` `1` is Base64 and `encrypted` `2` is ROT13, applied to the ad id, the message, and the probability. `Base64Ad` opens those three fields while `MessageResponse` is built, before the task is stored, so the bot and the board both use the plain id that `/solve` accepts. The cipher details are in the [backend README](dragontrainer-bigbank/README.md).

### Frontend

`DragonTrainer` is an Angular 21 app: standalone components, signals, `HttpClient`, and the router. Styling is Tailwind CSS 4 plus `src/styles.css`. `npm start` runs `ng serve`. The API base is the constant `http://localhost:8080` in `src/app/game/api-base.ts`.

Routes are `''` (home), `play`, `tasks`, `shop`, and `reputation`. Those four game screens use `humanGameGuard`, which continues the saved session or sends you home. `watch` is the bot screen and is not that guard.

`GameStateService` holds the active game in signals. It writes the session UUID to `localStorage` under `dragontrainer.sessionId`. Opening the app does not enter a game. Continue appears only when that id still exists on the backend. Watch DragonBot starts a second session and does not replace the saved id.

The HUD is fixed at the top right on every screen once a game exists: a pawn and the turn, the level mark and the level, the gold pot and gold, the score icon and score, and three hearts (filled while that life remains). Below 768px the bar is hidden. Only the level icon stays, with no number. Clicking it covers the screen and shows those same stats on a card in the center. The Back control is a labelled button on a wide screen and only the arrow below 768px.

A screen stays behind a spinner until its background image, the SVGs on that screen, and the text for that view have all been fetched. Then the whole picture is shown together.

The home, play, shop, and reputation screens use the lane photograph. The task board uses the stone wall. Each ad is a button masked with `Parchment--Arvin61r58.svg`, tinted by difficulty, kept at the SVG's aspect ratio, with the text inset so it stays on the parchment. Clicking an ad fades the screen, shows "You attempt to:" and the message, then "You Succeed!" or "You fail!".

## Start everything

The script starts Postgres only when that container is not already running, then the backend, then the Angular app. It prints both addresses when they are up. If port 4200 is already taken, the frontend starts on the next free port and that address is the one printed.

PowerShell:

```powershell
.\start.ps1
```

Bash:

```bash
./start.sh
```

- Front: http://localhost:4200, or the next free port if that one is taken
- Back: http://localhost:8080

## Start each part yourself

Docker Desktop has to be running.

1. Postgres, from `dragontrainer-bigbank`. Copy `.env.example` to `.env` if that file is missing.

```bash
cd dragontrainer-bigbank
docker compose up -d
```

2. Backend, from the same folder. It listens on port 8080.

```powershell
.\gradlew.bat bootRun
```

```bash
./gradlew bootRun
```

3. Frontend, from `DragonTrainer`. It listens on port 4200 and calls the backend at `http://localhost:8080`.

```bash
cd DragonTrainer
npm start
```
