import { HttpErrorResponse } from '@angular/common/http';
import { ApiProblem, FieldIssue } from '../models/api.types';

/** Erro normalizado de qualquer chamada à API (corpo problem+json ou falha de rede). */
export class ApiError extends Error implements ApiProblem {
  readonly status: number;
  readonly code: string;
  readonly title?: string;
  readonly detail?: string;
  readonly traceId?: string;
  readonly errors: FieldIssue[];
  readonly retryAfterSeconds?: number;

  constructor(problem: ApiProblem) {
    super(problem.code);
    this.name = 'ApiError';
    this.status = problem.status;
    this.code = problem.code;
    this.title = problem.title;
    this.detail = problem.detail;
    this.traceId = problem.traceId;
    this.errors = problem.errors;
    this.retryAfterSeconds = problem.retryAfterSeconds;
  }

  /** Código do primeiro erro do campo informado, se houver. */
  fieldCode(field: string): string | undefined {
    return this.errors.find((e) => e.field === field)?.code;
  }

  get isNetwork(): boolean {
    return this.status === 0;
  }
}

interface RawProblem {
  status?: number;
  code?: string;
  title?: string;
  detail?: string;
  trace_id?: string;
  errors?: { field?: string; code?: string; message?: string }[];
  retry_after_seconds?: number;
}

/** Converte qualquer falha em {@link ApiError}; nunca lança. */
export function toApiError(err: unknown): ApiError {
  if (err instanceof ApiError) {
    return err;
  }
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return new ApiError({ status: 0, code: 'NETWORK_ERROR', errors: [] });
    }
    const body = (typeof err.error === 'object' && err.error !== null ? err.error : {}) as RawProblem;
    const retryHeader = Number(err.headers?.get('Retry-After'));
    const retry = body.retry_after_seconds ?? (Number.isFinite(retryHeader) && retryHeader > 0 ? retryHeader : undefined);
    return new ApiError({
      status: err.status,
      code: body.code ?? defaultCode(err.status),
      title: body.title,
      detail: body.detail,
      traceId: body.trace_id,
      errors: (body.errors ?? []).map((e) => ({ field: e.field ?? '', code: e.code ?? '', message: e.message })),
      retryAfterSeconds: retry,
    });
  }
  return new ApiError({ status: -1, code: 'UNKNOWN', errors: [] });
}

function defaultCode(status: number): string {
  switch (status) {
    case 401:
      return 'UNAUTHENTICATED';
    case 403:
      return 'FORBIDDEN';
    case 404:
      return 'NOT_FOUND';
    case 429:
      return 'RATE_LIMITED';
    case 503:
      return 'SERVICE_UNAVAILABLE';
    default:
      return status >= 500 ? 'INTERNAL_ERROR' : 'UNKNOWN';
  }
}
