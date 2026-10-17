/** Tipos transversais da API (RFC 9457 + paginação). Modelos de domínio ficam em cada feature. */
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}

export interface FieldIssue {
  field: string;
  code: string;
  message?: string;
}

export interface ApiProblem {
  status: number;
  code: string;
  title?: string;
  detail?: string;
  traceId?: string;
  errors: FieldIssue[];
  retryAfterSeconds?: number;
}
