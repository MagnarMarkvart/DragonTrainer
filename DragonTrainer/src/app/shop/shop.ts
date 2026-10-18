import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BackButton } from '../back/back';
import { Ended } from '../game/ended';
import { GameApi } from '../game/game-api.service';
import { HUD_ASSETS, LANE_BG, preloadAssets } from '../game/preload';
import { GameStateService } from '../game/game-state.service';
import { ShopItem } from '../game/game.models';
import { Spinner } from '../spinner/spinner';

@Component({
  selector: 'app-shop',
  imports: [BackButton, Ended, Spinner],
  templateUrl: './shop.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shop {
  private readonly api = inject(GameApi);
  private readonly state = inject(GameStateService);

  readonly items = signal<ShopItem[]>([]);
  readonly busy = signal(false);
  readonly note = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly ready = signal(false);

  constructor() {
    void this.load();
  }

  async buy(item: ShopItem): Promise<void> {
    const game = this.state.activeGame();
    if (!game || game.lives <= 0 || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.ready.set(false);
    this.error.set(null);
    try {
      const result = await firstValueFrom(this.api.buy(game.id, item.id));
      this.note.set(result.success ? `Bought ${item.name}.` : `Could not buy ${item.name}.`);
      await this.state.refreshActive();
      await this.load();
    } catch {
      this.error.set('Could not buy that item.');
      this.ready.set(true);
    } finally {
      this.busy.set(false);
    }
  }

  private async load(): Promise<void> {
    const game = this.state.activeGame();
    const assets = preloadAssets([LANE_BG, ...HUD_ASSETS]);
    if (!game || game.lives <= 0) {
      await assets;
      this.ready.set(true);
      return;
    }
    try {
      const [items] = await Promise.all([firstValueFrom(this.api.shop(game.id)), assets]);
      this.items.set(items);
    } catch {
      this.error.set('Could not load the shop.');
    } finally {
      this.ready.set(true);
    }
  }
}
