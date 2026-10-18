# DragonTrainer frontend

Angular client for the [Dragontrainer backend](../dragontrainer-bigbank/README.md). The repository overview is in the [root README](../README.md).

## Run

From the repository root, `.\start.ps1` (PowerShell) or `./start.sh` (bash) starts Postgres, the backend, and this app. The [root README](../README.md) also lists the three commands if you would rather start them yourself.

To run only the frontend, from this folder:

```bash
npm start
```

The app is at http://localhost:4200 unless that port is taken. The start script then picks the next free port and prints it. The app calls the backend at http://localhost:8080 (`src/app/game/api-base.ts`).

## How a game is kept

The backend stores the game under a session id. This app writes that id to `localStorage`, so closing the browser and opening it again can continue the same game. The external Mugloar `gameId` is not the resume key.

The opening screen has three choices:

1. **Start New Game** replaces the saved id and opens Shop, Tasks, and Reputation.
2. **Continue Game** appears when the saved id still exists on the backend.
3. **Watch DragonBot** starts a separate session, fades the screen to black, and shows each bot step in the center. Gold and score stay in the corner. This does not replace the saved game.

## Screens

The lane photograph is the home, play, shop, and reputation background. The stone wall is the task board. Each task is a parchment shape tinted by difficulty. Gold and score sit in the top right on every screen, once a game exists.

The board shows the message, difficulty, and id the backend stored. When the game sends an ad as Base64 (`encrypted` 1) or ROT13 (`encrypted` 2), the backend decodes it first, so the card is already plain text and the solve uses the id the game accepts.

## Assets

- `public/images/pexels-nc-farm-bureau-mark-31125297.jpg` — main background
- `public/images/pexels-magda-ehlers-pexels-13246806.jpg` — task wall
- `public/vectors/Parchment--Arvin61r58.svg` — task card shape
- `public/vectors/gold-pot-gold-svgrepo-com.svg` — gold
- `public/vectors/credit-cards-svgrepo-com.svg` — score
