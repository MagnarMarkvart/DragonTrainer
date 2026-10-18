import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { Hud } from './hud/hud';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Hud],
  templateUrl: './app.html',
})
export class App {}
