import type { FieldError, LogsResponse, SearchRequest, SearchResponse, UploadResult } from './types';

/** A 422 from the search endpoint, carrying every parameter error. */
export class SearchValidationError extends Error {
  constructor(public readonly errors: FieldError[]) {
    super(errors.map((e) => e.message).join('; '));
    this.name = 'SearchValidationError';
  }
}

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

/** Builds the query string; filters left undefined are not sent. */
export function buildSearchQuery(request: SearchRequest): URLSearchParams {
  const params = new URLSearchParams();
  const freeText = request.freeText?.trim();
  if (freeText) params.set('free_text', freeText);
  if (request.minPrice !== undefined) params.set('min_price', String(request.minPrice));
  if (request.maxPrice !== undefined) params.set('max_price', String(request.maxPrice));
  if (request.minBedrooms !== undefined) params.set('min_bedrooms', String(request.minBedrooms));
  if (request.minBathrooms !== undefined) params.set('min_bathrooms', String(request.minBathrooms));
  params.set('lat', String(request.lat));
  params.set('lng', String(request.lng));
  params.set('page', String(request.page));
  params.set('size', String(request.size));
  return params;
}

export async function searchListings(request: SearchRequest): Promise<SearchResponse> {
  const response = await fetch(`/marketplace/listing?${buildSearchQuery(request).toString()}`);
  if (response.status === 422) {
    const body = await response.json().catch(() => ({}));
    throw new SearchValidationError(Array.isArray(body?.search) ? body.search : []);
  }
  if (!response.ok) {
    throw new ApiError(response.status, 'Search failed');
  }
  return response.json();
}

export async function uploadListings(listings: unknown[]): Promise<UploadResult> {
  const response = await fetch('/marketplace-admin/listings', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(listings),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Upload failed');
  }
  return response.json();
}

export async function fetchLogs(page: number, size: number): Promise<LogsResponse> {
  const response = await fetch(`/marketplace-admin/logs?page=${page}&size=${size}`);
  if (!response.ok) {
    throw new ApiError(response.status, 'Could not load the listing log');
  }
  return response.json();
}
