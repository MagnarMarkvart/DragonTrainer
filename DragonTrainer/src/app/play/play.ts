import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BackButton } from '../back/back';
import { Ended } from '../game/ended';
import { HUD_ASSETS, LANE_BG, preloadAssets } from '../game/preload';
import { Spinner } from '../spinner/spinner';

@Component({
  selector: 'app-play',
  imports: [RouterLink, BackButton, Ended, Spinner],
  templateUrl: './play.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Play {
  readonly ready = signal(false);

  constructor() {
    void preloadAssets([LANE_BG, ...HUD_ASSETS]).then(() => this.ready.set(true));
  }
}
