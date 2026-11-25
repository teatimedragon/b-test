export interface PageMeta {
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface Listing {
  listingId: string;
  sourceId: string;
  sourceCode: string;
  address: string;
  city: string;
  state: string;
  zip: string;
  price: number;
  currencyCode: string;
  bedrooms: number;
  bathrooms: number;
  sqft: number;
  latitude: number;
  longitude: number;
  listedDate: string;
  status: string;
  description: string;
  /** Returned by search for validating the ranking; not shown in the UI. */
  ranking?: Ranking;
}

export interface Ranking {
  score: number;
  distanceScore: number;
  priceScore: number;
  freshnessScore: number;
  distanceMiles: number;
  ageDays: number;
}

export interface SearchResponse {
  listings: Listing[];
  meta: PageMeta;
}

export interface SearchFilters {
  freeText?: string;
  minPrice?: number;
  maxPrice?: number;
  minBedrooms?: number;
  minBathrooms?: number;
}

export interface SearchRequest extends SearchFilters {
  lat: number;
  lng: number;
  page: number;
  size: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface UploadError {
  errorSource: string;
  errorDetails: string;
}

export interface UploadResult {
  received: number;
  accepted: number;
  rejected: number;
  results: { logId: string; listingId: string | null; accepted: boolean; errors: UploadError[] }[];
}

export interface LogEntry {
  logId: string;
  logTime: string;
  id: string | null;
  source: string | null;
  address: string | null;
  city: string | null;
  state: string | null;
  zip: string | null;
  price: string | null;
  currencyCode: string | null;
  bedrooms: string | null;
  bathrooms: string | null;
  sqft: string | null;
  latitude: string | null;
  longitude: string | null;
  listedDate: string | null;
  status: string | null;
  description: string | null;
  accepted: boolean;
  errors: (UploadError & { errorId: string })[];
}

export interface LogsResponse {
  logs: LogEntry[];
  meta: PageMeta;
}
