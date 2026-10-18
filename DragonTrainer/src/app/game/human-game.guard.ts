import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { GameStateService } from './game-state.service';

export const humanGameGuard: CanActivateFn = async () => {
  const state = inject(GameStateService);
  const router = inject(Router);
  await state.ready;
  if (state.watching()) {
    return router.createUrlTree(['/watch']);
  }
  if (!state.activeGame()) {
    const saved = state.savedGame();
    if (saved && saved.lives > 0) {
      state.continueSaved();
    }
  }
  if (!state.activeGame()) {
    return router.createUrlTree(['/']);
  }
  return true;
};
