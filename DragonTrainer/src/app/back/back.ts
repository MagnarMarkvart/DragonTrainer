import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-back',
  imports: [RouterLink],
  templateUrl: './back.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BackButton {
  readonly to = input('/');
  readonly pressed = output<void>();
}
