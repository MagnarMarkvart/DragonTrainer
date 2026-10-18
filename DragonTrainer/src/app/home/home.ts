import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { HUD_ASSETS, LANE_BG, preloadAssets } from '../game/preload';
import { GameStateService } from '../game/game-state.service';
import { Spinner } from '../spinner/spinner';

@Component({
  selector: 'app-home',
  imports: [Spinner],
  templateUrl: './home.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {
  private readonly state = inject(GameStateService);
  private readonly router = inject(Router);

  readonly canContinue = this.state.canContinue;
  readonly checking = this.state.checkingSave;
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly ready = signal(false);

  constructor() {
    this.state.leaveToMenu();
    void this.openPicture();
  }

  async startNew(): Promise<void> {
    await this.run(async () => {
      await this.state.startNew();
      await this.router.navigate(['/play']);
    }, 'Could not start a game.');
  }

  continueGame(): void {
    this.state.continueSaved();
    void this.router.navigate(['/play']);
  }

  async watchBot(): Promise<void> {
    await this.run(async () => {
      await this.state.startWatch();
      await this.router.navigate(['/watch']);
    }, 'Could not start the bot.');
  }

  private async openPicture(): Promise<void> {
    await Promise.all([preloadAssets([LANE_BG, ...HUD_ASSETS]), this.state.ready]);
    this.ready.set(true);
  }

  private async run(action: () => Promise<void>, message: string): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    } catch {
      this.error.set(message);
      this.busy.set(false);
    }
  }
}

