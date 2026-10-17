import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { ApiError, toApiError } from './api-error';

describe('toApiError', () => {
  it('normaliza problem+json (RFC 9457)', () => {
    const err = toApiError(
      new HttpErrorResponse({
        status: 422,
        error: {
          code: 'VALIDATION_ERROR', title: 'x', trace_id: 'tr-1',
          errors: [{ field: 'email', code: 'INVALID_FORMAT', message: 'm' }],
        },
      }),
    );
    expect(err.status).toBe(422);
    expect(err.code).toBe('VALIDATION_ERROR');
    expect(err.traceId).toBe('tr-1');
    expect(err.fieldCode('email')).toBe('INVALID_FORMAT');
    expect(err.fieldCode('outro')).toBeUndefined();
  });

  it('usa retry_after_seconds do corpo ou o cabeçalho Retry-After', () => {
    expect(toApiError(new HttpErrorResponse({ status: 429, error: { code: 'RATE_LIMITED', retry_after_seconds: 30 } })).retryAfterSeconds).toBe(30);
    const viaHeader = new HttpErrorResponse({ status: 429, headers: new HttpHeaders({ 'Retry-After': '12' }), error: null });
    const e = toApiError(viaHeader);
    expect(e.code).toBe('RATE_LIMITED');
    expect(e.retryAfterSeconds).toBe(12);
  });

  it('falha de rede vira NETWORK_ERROR', () => {
    const e = toApiError(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }));
    expect(e.code).toBe('NETWORK_ERROR');
    expect(e.isNetwork).toBe(true);
  });

  it('deriva o código do status quando o corpo não é problem+json', () => {
    expect(toApiError(new HttpErrorResponse({ status: 401, error: '<html>' })).code).toBe('UNAUTHENTICATED');
    expect(toApiError(new HttpErrorResponse({ status: 503 })).code).toBe('SERVICE_UNAVAILABLE');
    expect(toApiError(new HttpErrorResponse({ status: 500 })).code).toBe('INTERNAL_ERROR');
  });

  it('nunca lança e preserva ApiError existente', () => {
    const original = new ApiError({ status: 409, code: 'CONFLICT', errors: [] });
    expect(toApiError(original)).toBe(original);
    expect(toApiError('qualquer coisa').code).toBe('UNKNOWN');
  });
});
