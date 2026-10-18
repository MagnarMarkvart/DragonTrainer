import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE } from './api-base';
import { GameStateService } from './game-state.service';
import { GameState } from './game.models';

function game(overrides: Partial<GameState> = {}): GameState {
  return {
    id: 'sess-1',
    gameId: 'ext-1',
    lives: 3,
    gold: 10,
    level: 1,
    score: 0,
    highScore: 0,
    turn: 1,
    ...overrides,
  };
}

describe('GameStateService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('offers no continue when nothing is saved', async () => {
    const state = TestBed.inject(GameStateService);
    await state.ready;
    expect(state.canContinue()).toBe(false);
    expect(state.checkingSave()).toBe(false);
  });

  it('drops a saved game that is already over', async () => {
    localStorage.setItem('dragontrainer.sessionId', 'sess-1');
    const state = TestBed.inject(GameStateService);
    http.expectOne(`${API_BASE}/api/games/sess-1`).flush(game({ lives: 0 }));
    await state.ready;
    expect(state.canContinue()).toBe(false);
    expect(localStorage.getItem('dragontrainer.sessionId')).toBeNull();
  });

  it('forgets a saved id the backend no longer has', async () => {
    localStorage.setItem('dragontrainer.sessionId', 'missing');
    const state = TestBed.inject(GameStateService);
    http.expectOne(`${API_BASE}/api/games/missing`).flush('nope', { status: 404, statusText: 'Not Found' });
    await state.ready;
    expect(state.canContinue()).toBe(false);
    expect(localStorage.getItem('dragontrainer.sessionId')).toBeNull();
  });

  it('keeps the saved game when the bot is started', async () => {
    localStorage.setItem('dragontrainer.sessionId', 'sess-1');
    const state = TestBed.inject(GameStateService);
    http.expectOne(`${API_BASE}/api/games/sess-1`).flush(game());
    await state.ready;

    const started = state.startWatch();
    http.expectOne(`${API_BASE}/api/games`).flush(game({ id: 'bot-1', gameId: 'ext-bot' }));
    await started;

    expect(state.savedGame()?.id).toBe('sess-1');
    expect(state.activeGame()?.id).toBe('bot-1');
    expect(localStorage.getItem('dragontrainer.sessionId')).toBe('sess-1');
    expect(state.watching()).toBe(true);
  });
});
