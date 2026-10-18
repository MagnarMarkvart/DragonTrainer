import { Routes } from '@angular/router';

import { humanGameGuard } from './game/human-game.guard';
import { Home } from './home/home';
import { Play } from './play/play';
import { ReputationScreen } from './reputation/reputation';
import { Shop } from './shop/shop';
import { Tasks } from './tasks/tasks';
import { Watch } from './watch/watch';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'play', component: Play, canActivate: [humanGameGuard] },
  { path: 'tasks', component: Tasks, canActivate: [humanGameGuard] },
  { path: 'shop', component: Shop, canActivate: [humanGameGuard] },
  { path: 'reputation', component: ReputationScreen, canActivate: [humanGameGuard] },
  { path: 'watch', component: Watch },
];
