import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type ChipTone = 'success' | 'warning' | 'error' | 'neutral' | 'info';

const ICONS: Record<ChipTone, string> = {
  success: 'check_circle',
  warning: 'warning',
  error: 'error',
  neutral: 'radio_button_unchecked',
  info: 'info',
};

/** Chip de estado com ícone + texto (a cor nunca é a única informação — WCAG 1.4.1). */
@Component({
  selector: 'app-status-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  template: `<span class="chip" [class]="'tone-' + tone()"><mat-icon aria-hidden="true">{{ icon() }}</mat-icon>{{ label() }}</span>`,
  styleUrl: './status-chip.component.scss',
})
export class StatusChipComponent {
  readonly label = input.required<string>();
  readonly tone = input<ChipTone>('neutral');
  readonly icon = computed(() => ICONS[this.tone()]);
}
