import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-spinner',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="spinner-screen" role="status" aria-live="polite" aria-label="Loading">
      <span class="spinner"></span>
    </div>
  `,
})
export class Spinner {}
