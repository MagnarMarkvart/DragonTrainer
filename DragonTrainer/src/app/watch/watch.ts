import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { BackButton } from '../back/back';
import { GameApi } from '../game/game-api.service';
import { HUD_ASSETS, LANE_BG, preloadAssets } from '../game/preload';
import { GameStateService } from '../game/game-state.service';
import { Reputation } from '../game/game.models';
import { Spinner } from '../spinner/spinner';

@Component({
  selector: 'app-watch',
  imports: [BackButton, Spinner],
  templateUrl: './watch.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Watch {
  private readonly api = inject(GameApi);
  private readonly state = inject(GameStateService);
  private readonly router = inject(Router);
  private stop = false;

  readonly text = signal('The dragon stirs.');
  readonly reputation = signal<Reputation | null>(null);
  readonly finished = signal(false);
  readonly ready = signal(false);

  constructor() {
    inject(DestroyRef).onDestroy(() => {
      this.stop = true;
    });
    void this.openPicture();
  }

  leave(): void {
    this.stop = true;
    this.state.leaveToMenu();
    void this.router.navigate(['/']);
  }

  private async openPicture(): Promise<void> {
    await preloadAssets([LANE_BG, ...HUD_ASSETS]);
    this.ready.set(true);
    void this.play();
  }

  private async play(): Promise<void> {
    const game = this.state.activeGame();
    const savedId = this.state.savedGame()?.id;
    if (!game || game.id === savedId) {
      await this.router.navigate(['/']);
      return;
    }
    const sessionId = game.id;
    while (!this.stop) {
      try {
        const step = await firstValueFrom(this.api.botStep(sessionId));
        if (this.stop) {
          return;
        }
        this.state.applyBotStep(sessionId, step);
        this.text.set(step.text);
        this.reputation.set(step.action === 'SKIP' ? (step.reputation ?? null) : null);
        if (step.finished) {
          this.finished.set(true);
          return;
        }
        await this.pause(1000);
      } catch {
        if (!this.stop) {
          this.text.set('The bot lost the trail.');
          this.reputation.set(null);
          this.finished.set(true);
        }
        return;
      }
    }
  }

  private pause(ms: number): Promise<void> {
    return new Promise((resolve) => {
      setTimeout(resolve, ms);
    });
  }
}
