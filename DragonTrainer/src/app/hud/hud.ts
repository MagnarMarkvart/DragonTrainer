import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';

import { GameStateService } from '../game/game-state.service';

@Component({
  selector: 'app-hud',
  imports: [NgTemplateOutlet],
  templateUrl: './hud.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'close()',
  },
})
export class Hud {
  private readonly state = inject(GameStateService);
  readonly game = this.state.hudGame;
  readonly statsOpen = signal(false);

  hearts(lives: number): boolean[] {
    return [0, 1, 2].map((index) => index < lives);
  }

  toggle(): void {
    this.statsOpen.update((open) => !open);
  }

  close(): void {
    this.statsOpen.set(false);
  }
}
