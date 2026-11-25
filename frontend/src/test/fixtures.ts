import { vi } from 'vitest';
import type { Listing, LogEntry, PageMeta } from '../api/types';

export function makeListing(overrides: Partial<Listing> = {}): Listing {
  return {
    listingId: 'zillow:12345',
    sourceId: '12345',
    sourceCode: 'zillow',
    address: '123 Main St',
    city: 'Alexandria',
    state: 'VA',
    zip: '22306',
    price: 450000,
    currencyCode: 'USD',
    bedrooms: 3,
    bathrooms: 2.5,
    sqft: 1850,
    latitude: 38.7893,
    longitude: -77.1873,
    listedDate: '2026-11-20',
    status: 'active',
    description: 'Bright colonial near the park.',
    ...overrides,
  };
}

export function makeLog(overrides: Partial<LogEntry> = {}): LogEntry {
  return {
    logId: 'log-1',
    logTime: '2026-11-25T09:15:02.123Z',
    id: '12345',
    source: 'zillow',
    address: '123 Main St',
    city: 'Alexandria',
    state: 'VA',
    zip: '22306',
    price: '450000',
    currencyCode: 'USD',
    bedrooms: '3',
    bathrooms: '2.5',
    sqft: '1850',
    latitude: '38.7893',
    longitude: '-77.1873',
    listedDate: '2026-11-20',
    status: 'active',
    description: 'Bright colonial near the park.',
    accepted: true,
    errors: [],
    ...overrides,
  };
}

export function meta(totalElements: number, page = 0, size = 20): PageMeta {
  return { page, size, totalElements, totalPages: Math.ceil(totalElements / size) };
}

export function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

type Handler = (url: URL, init?: RequestInit) => Response | Promise<Response>;

/** Stubs global fetch, routing by path. Returns the mock so tests can inspect calls. */
export function mockFetch(routes: Record<string, Handler>) {
  const fn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), 'http://localhost');
    const key = `${(init?.method ?? 'GET').toUpperCase()} ${url.pathname}`;
    const handler = routes[key];
    if (!handler) throw new Error(`Unexpected request: ${key}`);
    return handler(url, init);
  });
  vi.stubGlobal('fetch', fn);
  return fn;
}

/** The query parameters of every call to a path. */
export function callsTo(fetchMock: ReturnType<typeof mockFetch>, path: string): URLSearchParams[] {
  return fetchMock.mock.calls
    .map(([input]) => new URL(String(input), 'http://localhost'))
    .filter((url) => url.pathname === path)
    .map((url) => url.searchParams);
}
