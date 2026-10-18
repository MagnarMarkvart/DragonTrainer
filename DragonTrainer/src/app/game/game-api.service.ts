import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { API_BASE } from './api-base';
import { BotStep, GameState, PurchaseResult, Reputation, ShopItem, SolveResult, TaskAd } from './game.models';

@Injectable({ providedIn: 'root' })
export class GameApi {
  private readonly http = inject(HttpClient);
  private readonly games = `${API_BASE}/api/games`;

  start() {
    return this.http.post<GameState>(this.games, null);
  }

  get(id: string) {
    return this.http.get<GameState>(`${this.games}/${id}`);
  }

  tasks(id: string) {
    return this.http.get<TaskAd[]>(`${this.games}/${id}/tasks`);
  }

  solve(id: string, adId: string) {
    return this.http.post<SolveResult>(
      `${this.games}/${id}/tasks/${encodeURIComponent(adId)}/solve`,
      null,
    );
  }

  shop(id: string) {
    return this.http.get<ShopItem[]>(`${this.games}/${id}/shop`);
  }

  buy(id: string, itemId: string) {
    return this.http.post<PurchaseResult>(
      `${this.games}/${id}/shop/${encodeURIComponent(itemId)}`,
      null,
    );
  }

  reputation(id: string) {
    return this.http.post<Reputation>(`${this.games}/${id}/reputation`, null);
  }

  botStep(id: string) {
    return this.http.post<BotStep>(`${this.games}/${id}/bot/step`, null);
  }
}
