import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { GameApi } from './game-api.service';
import { BotStep, GameState } from './game.models';

const STORAGE_KEY = 'dragontrainer.sessionId';

@Injectable({ providedIn: 'root' })
export class GameStateService {
  private readonly api = inject(GameApi);

  readonly savedGame = signal<GameState | null>(null);
  readonly activeGame = signal<GameState | null>(null);
  readonly watching = signal(false);
  readonly checkingSave = signal(true);
  readonly canContinue = computed(() => this.savedGame() !== null);
  readonly hudGame = computed(() => this.activeGame() ?? this.savedGame());

  readonly ready: Promise<void>;
  private resolveReady!: () => void;

  constructor() {
    this.ready = new Promise((resolve) => {
      this.resolveReady = resolve;
    });
    void this.validateSaved();
  }

  async startNew(): Promise<GameState> {
    const game = await firstValueFrom(this.api.start());
    this.watching.set(false);
    this.remember(game);
    this.activeGame.set(game);
    return game;
  }

  continueSaved(): void {
    const game = this.savedGame();
    if (game) {
      this.activeGame.set(game);
    }
  }

  async startWatch(): Promise<GameState> {
    const game = await firstValueFrom(this.api.start());
    this.watching.set(true);
    this.activeGame.set(game);
    return game;
  }

  async refreshActive(): Promise<GameState | null> {
    const current = this.activeGame();
    if (!current) {
      return null;
    }
    const game = await firstValueFrom(this.api.get(current.id));
    this.sync(game);
    return game;
  }

  applyBotStep(sessionId: string, step: BotStep): void {
    this.activeGame.update((game) => {
      if (!game || game.id !== sessionId) {
        return game;
      }
      return {
        ...game,
        lives: step.lives,
        gold: step.gold,
        level: step.level,
        score: step.score,
        highScore: step.highScore,
        turn: step.turn,
        reputation: step.reputation ?? game.reputation,
      };
    });
  }

	leaveToMenu(): void {
		this.watching.set(false);
		this.activeGame.set(this.savedGame());
	}

	abandonSaved(): void {
		this.watching.set(false);
		this.forget();
		this.activeGame.set(null);
	}

  private async validateSaved(): Promise<void> {
    const id = localStorage.getItem(STORAGE_KEY);
    if (!id) {
      this.checkingSave.set(false);
      this.resolveReady();
      return;
    }
    try {
      const game = await firstValueFrom(this.api.get(id));
      if (game.lives <= 0) {
        this.forget();
      } else {
        this.savedGame.set(game);
      }
    } catch (error) {
      if (this.isMissing(error)) {
        this.forget();
      }
    } finally {
      this.checkingSave.set(false);
      this.resolveReady();
    }
  }

  private sync(game: GameState): void {
    this.activeGame.set(game);
    if (this.savedGame()?.id !== game.id) {
      return;
    }
    if (game.lives <= 0) {
      this.forget();
      return;
    }
    this.savedGame.set(game);
  }

  private remember(game: GameState): void {
    localStorage.setItem(STORAGE_KEY, game.id);
    this.savedGame.set(game);
  }

  private forget(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.savedGame.set(null);
  }

  private isMissing(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 404;
  }
}
