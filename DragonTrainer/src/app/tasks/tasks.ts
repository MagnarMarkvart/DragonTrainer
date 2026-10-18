import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { BackButton } from '../back/back';
import { difficultyStyle } from '../game/difficulty';
import { Ended } from '../game/ended';
import { GameApi } from '../game/game-api.service';
import { HUD_ASSETS, PARCHMENT, WALL_BG, preloadAssets } from '../game/preload';
import { GameStateService } from '../game/game-state.service';
import { TaskAd } from '../game/game.models';
import { Spinner } from '../spinner/spinner';

type AttemptPhase = 'reading' | 'fading' | 'success' | 'fail';

@Component({
  selector: 'app-tasks',
  imports: [BackButton, Ended, RouterLink, Spinner],
  templateUrl: './tasks.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Tasks {
  private readonly api = inject(GameApi);
  private readonly state = inject(GameStateService);

  readonly tasks = signal<TaskAd[]>([]);
  readonly busy = signal(false);
  readonly ready = signal(false);
  readonly error = signal<string | null>(null);
  readonly attempt = signal<TaskAd | null>(null);
  readonly phase = signal<AttemptPhase | null>(null);
  readonly styleOf = difficultyStyle;

  constructor() {
    void this.load();
  }

  async solve(task: TaskAd): Promise<void> {
    const game = this.state.activeGame();
    if (!game || game.lives <= 0 || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.attempt.set(task);
    this.phase.set('reading');
    const shownFor = this.pause(2000);
    try {
      const result = await firstValueFrom(this.api.solve(game.id, task.adId));
      await shownFor;
      this.phase.set('fading');
      await this.pause(450);
      this.phase.set(result.success ? 'success' : 'fail');
      await this.pause(1400);
      await this.state.refreshActive();
      await this.load();
    } catch {
      this.error.set('Could not solve that task.');
    } finally {
      this.attempt.set(null);
      this.phase.set(null);
      this.busy.set(false);
    }
  }

  private async load(): Promise<void> {
    const game = this.state.activeGame();
    const assets = preloadAssets([WALL_BG, PARCHMENT, ...HUD_ASSETS]);
    if (!game || game.lives <= 0) {
      await assets;
      this.ready.set(true);
      return;
    }
    this.ready.set(false);
    this.error.set(null);
    try {
      const [ads] = await Promise.all([firstValueFrom(this.api.tasks(game.id)), assets]);
      this.tasks.set(ads);
    } catch (error) {
      const gone = error instanceof HttpErrorResponse && (error.status === 404 || error.status === 410);
      this.tasks.set([]);
      if (gone) {
        this.state.abandonSaved();
        this.error.set('This game is no longer available. Start a new one.');
      } else {
        this.error.set('Could not load tasks. The game server did not answer.');
      }
    } finally {
      this.ready.set(true);
    }
  }

  private pause(ms: number): Promise<void> {
    return new Promise((resolve) => {
      setTimeout(resolve, ms);
    });
  }
}
