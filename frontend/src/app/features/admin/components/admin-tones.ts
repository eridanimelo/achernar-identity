import { ChipTone } from '../../../shared/components/status-chip/status-chip.component';

export function accountStatusTone(status: string): ChipTone {
  return status === 'ACTIVE' ? 'success' : status === 'DISABLED' ? 'error' : 'warning';
}

export function registryStatusTone(status: string): ChipTone {
  return status === 'ACTIVE' ? 'success' : 'neutral';
}

export function severityTone(severity: string): ChipTone {
  return severity === 'ALERT' ? 'error' : severity === 'NOTICE' ? 'warning' : 'info';
}

export function outcomeTone(outcome: string): ChipTone {
  return outcome === 'SUCCESS' ? 'success' : 'error';
}
