import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BackButton } from '../back/back';
import { Ended } from '../game/ended';
import { GameApi } from '../game/game-api.service';
import { HUD_ASSETS, LANE_BG, preloadAssets } from '../game/preload';
import { GameStateService } from '../game/game-state.service';
import { Spinner } from '../spinner/spinner';

@Component({
  selector: 'app-reputation',
  imports: [BackButton, Ended, Spinner],
  templateUrl: './reputation.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReputationScreen {
  private readonly api = inject(GameApi);
  private readonly state = inject(GameStateService);

  readonly game = this.state.activeGame;
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly ready = signal(false);

  constructor() {
    void preloadAssets([LANE_BG, ...HUD_ASSETS]).then(() => this.ready.set(true));
  }

  async check(): Promise<void> {
    const game = this.state.activeGame();
    if (!game || game.lives <= 0 || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.ready.set(false);
    this.error.set(null);
    try {
      await firstValueFrom(this.api.reputation(game.id));
      await this.state.refreshActive();
    } catch {
      this.error.set('Could not check reputation.');
    } finally {
      this.busy.set(false);
      this.ready.set(true);
    }
  }
}
