import { Component, computed, inject } from '@angular/core';

import { BackButton } from '../back/back';
import { GameStateService } from './game-state.service';

@Component({
  selector: 'app-ended',
  imports: [BackButton],
  template: `
    @if (ended()) {
      <div class="ended">
        <p>Game over.</p>
        <app-back (pressed)="leave()" />
      </div>
    }
  `,
})
export class Ended {
  private readonly state = inject(GameStateService);
  readonly ended = computed(() => (this.state.activeGame()?.lives ?? 1) <= 0);

  leave(): void {
    this.state.leaveToMenu();
  }
}
