# Application log

How the app is built, the decisions made along the way, and where the build differs from or fills gaps in `build-spec.md`.

## 1. Architecture

```
frontend (React + Vite)  ──/marketplace, /marketplace-admin──▶  backend (Spring Boot)  ──▶  H2 in memory
   nginx :3000 in Docker                                           :8080
   Vite dev server :5173 locally (proxy)
```

### Backend packages (`backend/src/main/java/com/somecompany/marketplace`)

| Package | Contents | Spring? |
|---|---|---|
| `validation` | `RawListing`, `RawListingReader` (JSON → strings), `ListingField`, `ListingValidators` (one factory per rule), `ValidationPipeline`, `ListingMapper`, `Numbers`, `Messages` | No |
| `marketplace` | `SearchParamsValidator`, `ListingSearchFilter`, `ListingRanker`, `Haversine`, `GeoPoint` (plain), plus `MarketplaceSearchService` and `MarketplaceController` | Only the service and controller |
| `admin` | `ListingUploadService` (log → validate → errors → upsert), `AdminQueryService`, `AdminController`, DTOs | Yes |
| `domain` | JPA entities `ListingLogEntity`, `ListingErrorEntity`, `ListingEntity` and their repositories | JPA only |
| `common` | `AppConfig` (wires the plain classes and the `Clock`), pagination (`PageRequestParams`, `PageMeta`), error handling, `IdGenerator` / `UuidV7Generator` | Mixed |

- Constructor injection everywhere. The framework-free classes are registered as beans in `AppConfig`, so they have no Spring annotations and are unit tested with `new`.
- A `java.time.Clock` bean (UTC) drives "today" and `LogTime`. Integration tests replace it with a clock fixed at `2026-11-25T09:00:00Z` that moves forward 1 ms each time it is read, so log order is deterministic.
- UUIDv7 ids come from `java-uuid-generator` (`Generators.timeBasedEpochGenerator()`), behind an `IdGenerator` interface.
- Table and column names match the spec exactly (`ListingLog.LogId`, `Listing.SquareFeet`, …) by using Hibernate's `PhysicalNamingStrategyStandardImpl` and explicit `@Table`/`@Column` names. The schema is created on startup (`ddl-auto=create-drop`).

### Frontend (`frontend/src`)

- `context/DefaultLocationContext.tsx`: the default location in React state, shared by both pages, initial value `38.7893, -77.1873`. It is never stored, so a reload resets it.
- `pages/MarketplacePage.tsx`: filter bar, results, pagination, popup. One effect runs the search whenever filters, page or location change; any filter change resets to page 0.
- `pages/AdminPage.tsx` with `components/admin/LocationForm.tsx` and `components/admin/UploadAndLog.tsx`.
- `components/`: `SearchBar`, `PriceFilter`, `BedBathFilter`, `Dropdown`, `ListingCard`, `ListingModal`, `ListingMap` (react-leaflet with OpenStreetMap tiles), `Pagination`.
- `api/client.ts`: fetch wrappers. `searchListings` turns a 422 into a `SearchValidationError` carrying every message.
- Routing uses `react-router-dom`: `/` is the marketplace and `/admin` is the admin page. These paths don't overlap the API prefixes, so nginx and the Vite proxy can send everything under `/marketplace*` to the backend.

## 2. Upload and validation pipeline

`POST /marketplace-admin/listings` reads the body as text and parses it with Jackson itself (exact `BigDecimal`s, with trailing zeros kept), so malformed JSON and non-arrays get a clear `400 { "error": … }` before anything is logged.

For each array entry, in order, inside **its own transaction**:

1. **Read** – `RawListingReader` turns the entry into a `RawListing` of strings: strings as is, numbers as plain decimal text (`1E+5` → `100000`), JSON `null` or a missing field as `null`, and booleans, objects or arrays as their JSON text (so they are logged, then rejected as "not a valid number" and so on). An entry that is not an object becomes an empty `RawListing` flagged as such.
2. **Log** – the raw values go to `ListingLog`, each truncated to its column size, with a new UUIDv7 `LogId` and `LogTime = clock.instant()`. `CurrencyCode` is always `USD`.
3. **Validate** – `ValidationPipeline` runs every `ListingValidator` (a `Function<RawListing, List<ValidationError>>`) in order and concatenates all errors. It never stops early.
4. **Record errors** – one `ListingError` row per error, linked to the log entry. `ErrorDetails` is truncated to 100 characters.
5. **Upsert** – when there are no errors, `ListingMapper` builds the `Listing` row and `save()` merges it: a new `ListingId` is inserted, and an existing one has every column overwritten. Because entries run in array order, a repeated `ListingId` in one upload ends with the later values.

If anything unexpected throws while processing an entry (for example a database error), that entry's transaction rolls back. A new log entry is then written with one `general` error, "Unexpected error while saving the listing", and the remaining entries carry on.

### Validators (in pipeline order)

| Validator | Rule | ErrorSource |
|---|---|---|
| `required(field)`, for all 15 fields | Not null, not empty, not whitespace only | column name |
| `maxLength(field)`, for all fields except `state` | Raw length ≤ column size (the text columns are the same size in ListingLog and Listing; numeric fields use their ListingLog column size) | column name |
| `listingIdLength` | `source:id` ≤ 150 characters | `general` |
| `state` | Exactly 2 letters A–Z, any case | `State` |
| `status` | `active`, `pending` or `sold`, any case | `Status` |
| `listedDate` | Strict `yyyy-MM-dd` and a real calendar date (`2026-02-30` fails) | `ListedDate` |
| `price` | A number, > 0, ≤ 15 integer digits, ≤ 4 decimal places | `Price` |
| `bedrooms` | A whole number, ≥ 0, ≤ 32767 | `Bedrooms` |
| `bathrooms` | A number, > 0, ≤ 2 decimal places, < 10000 | `Bathrooms` |
| `sqft` | A whole number, > 0, ≤ 2147483647 | `SquareFeet` |
| `latitude` | A finite number from -90 to 90 inclusive | `Latitude` |
| `longitude` | A finite number from -180 to 180 inclusive | `Longitude` |

The format and number rules skip a value that is missing or blank, because `required` has already reported it. That way a missing price gives one error, not three. Every message quotes the failed value and gives the reason, for example `Price '-5' must be greater than 0` and `State 'Virginia' must be exactly 2 letters`. Values longer than 30 characters are shortened with `...` inside the message so the reason still fits in the 100-character `ErrorDetails` column.

### Mapping to Listing

`id` → `SourceId`, `source` → `SourceCode`, `sqft` → `SquareFeet`, and every other field goes to the column of the same name. Text is trimmed, `State` is uppercased and `Status` is lowercased. Price is stored with scale 4 and bathrooms with scale 2. Latitude and longitude are rounded HALF_UP to 6 decimal places.

## 3. Search and ranking

`GET /marketplace/listing`:

1. `SearchParamsValidator` parses every parameter from its raw string and collects all problems (non-numeric values, negatives, `min_price > max_price`, `lat` without `lng` or the reverse, out-of-range coordinates, page and size limits) into one `422 { "search": [...] }`.
2. The repository loads listings with `ListedDate <= today`. Dates are always stored as `yyyy-MM-dd`, so comparing the strings compares the dates.
3. `ListingSearchFilter` applies the price, bedroom, bathroom and free-text filters with AND (and checks the future-date rule again). Free text is trimmed and matched as one case-insensitive substring of city, state, zip or description.
4. `ListingRanker` ranks the **whole filtered set**, then the service slices out the requested page.

```
score          = 0.4 × distanceScore + 0.4 × priceScore + 0.2 × freshnessScore
distanceScore  = 10 − 9 × (distance / dMax)                  (10 for every listing when dMax = 0)
priceScore     = 10 − 9 × ((price − pMin) / (pMax − pMin))   (10 for every listing when pMax = pMin)
freshnessScore = max(1, 10 − floor(ageDays / 5))
```

- Distance is the haversine great-circle distance in miles, using an Earth radius of 3958.8 miles.
- `dMax`, `pMin` and `pMax` are taken from the whole filtered set across all statuses.
- The order is: status group (`active`, then `pending`, then `sold`), then score from highest to lowest, then newer `listedDate`, then `listingId` ascending. Paging is stable because of the last tie-breaker.

## 4. Deviations and decisions where the spec was silent

1. **The `422` body key is `search` on the admin endpoints too.** The spec only defines the 422 shape for search. Invalid `page` or `size` on `/marketplace-admin/listings` and `/logs` use the same `{ "search": [...] }` body so clients have one error format.
2. **`state` has no separate length error.** "Exactly 2 letters" already covers the length, so `Virginia` gives one error (`State 'Virginia' must be exactly 2 letters`) instead of two. The ListingLog value is still truncated to `Vi`.
3. **Length errors on numeric fields** use the ListingLog column size (Price 100, Bedrooms 50, Bathrooms 50, SquareFeet 100, Latitude 20, Longitude 20), since the spec says over-long raw values are truncated in ListingLog and get a length error.
4. **Bathrooms must be under 10000.** `Numeric(6,2)` can only hold 4 digits before the decimal point, so larger values are rejected with a clear error rather than failing in the database.
5. **Whole numbers may have a zero fraction.** `bedrooms: "3.0"` and `sqft: 1850.0` are accepted as 3 and 1850; `2.5` is rejected.
6. **Numeric strings are accepted.** `"price": "450000"` is valid, because the spec allows any field to arrive as a string or a number. Scientific notation (`4.5E+5`) is accepted too; `NaN` and `Infinity` are rejected as not valid numbers.
7. **A missing value's message** is `<Column> is required but value was null`, which names the failed value (`null`) as the spec asks.
8. **A single listing that is not at the default location gets a distance score of 1**, because by the formula it is the furthest listing (`distance = dMax`). Only `dMax = 0` gives everyone 10. The tests cover both cases.
9. **Filtering happens in memory, after a database pre-filter on date.** Ranking needs the whole filtered set anyway, and the filter rules then live in a plain class that is easy to test. For a much larger data set, the price, bedroom and bathroom filters could move into the query without changing the ranker.
10. **Log order ties** (the same `LogTime`) are broken by `LogId` descending. UUIDv7 ids are time-ordered, so the most recently created entry still comes first.
11. **Output number format.** Price is returned with at least 2 decimals (`450000.00`), and bathrooms without trailing zeros (`2.5`). Latitude and longitude keep their stored 6-decimal scale; in JSON, `38.789300` and `38.7893` are the same number.
12. **Upload transactions are per listing,** not per request, so a failure in one listing can't roll back the others.
13. **UI details.** Price inputs accept whole numbers ≥ 0 (matching the API), and an invalid value also disables Apply. The Bedrooms & Bathrooms buttons apply as soon as they are clicked, since the spec says any filter change runs a new search. The filter bar wraps onto two lines below 640 px wide so it stays usable on phones; on wider screens it stays on one line.
14. **Unknown routes and methods** keep their framework status (404, 405, …) with `{ "error": … }` and no stack trace. Anything else unexpected returns `500 { "error": "Internal server error" }`.
15. **Docker base images:** `eclipse-temurin:21-jdk` and `eclipse-temurin:21-jre` for the backend, `node:22-alpine` and `nginx:1.27-alpine` for the frontend. nginx also falls back to `index.html` so `/admin` works on reload, and it allows request bodies up to 20 MB.

## 5. Tests

`./mvnw test` (JUnit 5, AssertJ, Mockito, Spring Boot Test with MockMvc and H2):

- `ListingValidatorsTest`: each validator for a valid value, missing, empty, whitespace only, wrong type, and the boundaries (latitude ±90, longitude ±180, bedrooms 0, price 0, sizes at and over each column limit, the 150-character ListingId). Every failure message must contain the failed value.
- `ValidationPipelineTest`: collects errors from all validators, runs them in order, returns no errors for a clean listing, and gives a single `general` error for a non-object entry.
- `ListingMapperTest` and `RawListingReaderTest`: field mapping, the ListingId format, normalising state and status, rounding coordinates, truncating log values, and converting JSON types to strings.
- `ListingRankerTest`: status grouping, each score formula and its edge cases (single result, equal prices, zero distance), the freshness boundaries (day 4 → 10, day 5 → 9, day 45 → 1), the 40/40/20 weights, both tie-breakers, min and max taken across statuses, and the origin changing the order.
- `HaversineTest`: known distances (one degree of latitude, New York to Los Angeles, Washington to Baltimore, London to Paris, antipodes).
- `SearchParamsValidatorTest`, `ListingSearchFilterTest`, `PageRequestParamsTest`.
- `ListingUploadServiceTest` (Mockito): log first, one error row per failure, and an unexpected failure in one listing doesn't stop the next.
- `MarketplaceSearchServiceTest` (Mockito): ranks the whole set and then slices the page.
- `AdminApiIntegrationTest` and `MarketplaceApiIntegrationTest`: everything listed in spec section 9.2, end to end over HTTP.

`npm test` (Vitest with React Testing Library, jsdom):

- `PriceFilter.test.tsx`: min above max shows a message and disables Apply; equal bounds and empty bounds work.
- `MarketplacePage.test.tsx`: the default location is sent; Any omits a parameter and 1½+ sends `1.5`; free text on Enter and on the icon; price apply; a filter change resets to page 0; card content and the pipe-separated line; 422 messages; the empty state; the popup (Leaflet container, description, date, status, their order, and closing with ×, Escape and a backdrop click).
- `AdminPage.test.tsx`: location validation; a new location changes the next search; an unparsable file and a non-array file show "File not in correct format" without a request; a 500 and a network error show "Upload failed"; success shows the summary and reloads the log list with errors; log pagination.
- `format.test.ts`, `client.test.ts`: formatting and query building.

## 6. Verification done while building

- `./mvnw test` and `npm test` both pass. `tsc -b` and `vite build` succeed.
- The packaged jar and the built frontend were run in the runtime images (`eclipse-temurin:21-jre` and `nginx:1.27-alpine` with `frontend/nginx.conf`) on a shared Docker network. The sample file was uploaded through nginx on port 3000: 36 received, 15 accepted, 21 rejected, each invalid entry with the expected error. Search, 422 errors, `/admin` reload and the UI (filters, popup map, admin log) were checked in a headless browser.
- The full `docker compose up --build` could not be run end to end in the build sandbox. Docker Hub rate-limited image pulls, and the sandbox's network proxy blocked `npm ci` and Maven downloads inside build containers. The Dockerfiles use only standard steps (`./mvnw package` and `npm ci && npm run build`), and their runtime stages were checked as described above.
