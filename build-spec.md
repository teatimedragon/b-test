# Build Spec: Some Company Listings Marketplace

This spec turns the original draft (`draft-instructions.txt`) into a buildable specification. Every original requirement is kept, and gaps and contradictions found in review are resolved in the sections below.

---

## 1. Overview

A web app for browsing property listings. It has two parts:

- **Marketplace (public landing page):** search, filter and view listings ranked by distance, price and freshness, with a map popup per listing.
- **Marketplace Admin:** upload listings as a JSON file, see the processing log with per-listing validation errors, and set the default location used for distance ranking.

---

## 2. Technology and project setup

| Area | Choice |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 3.x (Spring Web, Spring Data JPA, Bean Validation) |
| Build tool | Maven (with Maven Wrapper `mvnw`) |
| Database | H2, in-memory (`jdbc:h2:mem:marketplace`), schema created on startup |
| API style | RESTful, JSON, camelCase for all JSON field names |
| Frontend | React 18 + TypeScript, built with Vite |
| Map library | Leaflet via `react-leaflet`, using OpenStreetMap tiles (no API key needed) |
| Backend tests | JUnit 5, Mockito, AssertJ, Spring Boot Test with MockMvc |
| Frontend tests | Vitest + React Testing Library |
| UUIDs | UUIDv7 (time-ordered), e.g. via `com.fasterxml.uuid:java-uuid-generator` |
| Containers | Docker (multi-stage builds) + `docker-compose.yml` |

### 2.1 Design principles

- Use constructor-based **dependency injection** everywhere.
- Keep all business logic (validation pipeline, ranking, mapping, distance calculation) in plain Java classes with no framework dependencies, so each can be unit tested without Spring.
- Inject a `java.time.Clock` bean so "today" can be fixed in tests (freshness and future-date filtering depend on it).

### 2.2 Running the app

The app must run **both** in Docker and directly on a local machine.

- **Docker:** `docker compose up --build` starts two containers:
  - `backend`: Spring Boot on port 8080.
  - `frontend`: nginx serving the built React app on port 3000 and proxying `/marketplace` and `/marketplace-admin` to the backend.
- **Local:**
  - Backend: `./mvnw spring-boot:run` (port 8080).
  - Frontend: `npm install && npm run dev` (Vite on port 5173, with a dev proxy for `/marketplace` and `/marketplace-admin` to `localhost:8080`, so no CORS setup is needed).

### 2.3 Required documents

- `readme.md`: how to run the app with Docker and locally, how to run all tests, and a sample upload file.
- `applog.md`: the logic and decisions made while building the app (architecture, validation pipeline, ranking formula, and any deviations from this spec).
- `sample-data/listings.json`: a sample upload file with valid listings in all three statuses plus a few invalid ones that exercise each validation rule.

---

## 3. Data model (Listing domain)

All three tables live in the in-memory H2 database. Column names are as in the draft.

### 3.1 ListingLog

Records every listing exactly as received, before validation. All input fields are stored as strings so invalid values can still be logged.

| Column | Type | Notes |
|---|---|---|
| LogId | UUID (v7) | Primary key |
| LogTime | Timestamp | Server time when the record was received |
| SourceId | Varchar(50) | From request field `id` |
| SourceCode | Varchar(100) | From request field `source` |
| Address | Varchar(100) | |
| City | Varchar(100) | |
| State | Varchar(2) | |
| Zip | Varchar(50) | |
| Price | Varchar(100) | |
| CurrencyCode | Varchar(3) | Default `'USD'` |
| Bedrooms | Varchar(50) | |
| Bathrooms | Varchar(50) | |
| SquareFeet | Varchar(100) | From request field `sqft` |
| Latitude | Varchar(20) | |
| Longitude | Varchar(20) | |
| ListedDate | Varchar(100) | ISO date |
| Status | Varchar(50) | |
| Description | Varchar(250) | |

Raw values longer than a column's size are **truncated** to fit when stored in ListingLog, and the validation pipeline records a length error for that field (see 5.3).

### 3.2 ListingError

| Column | Type | Notes |
|---|---|---|
| ErrorId | UUID (v7) | Primary key |
| LogId | UUID (v7) | Foreign key to ListingLog.LogId |
| ErrorSource | Varchar(50) | The column name that failed (e.g. `Price`), or `general` |
| ErrorDetails | Varchar(100) | Message including the failed value; truncated to 100 chars |

### 3.3 Listing

| Column | Type | Notes |
|---|---|---|
| ListingId | Varchar(150) | Primary key: `SourceCode + ":" + SourceId` |
| SourceId | Varchar(50) | |
| SourceCode | Varchar(100) | |
| Address | Varchar(100) | |
| City | Varchar(100) | |
| State | Varchar(2) | Stored uppercase |
| Zip | Varchar(50) | |
| Price | Decimal(19,4) | |
| CurrencyCode | Varchar(3) | Default `'USD'` |
| Bedrooms | Smallint | |
| Bathrooms | Numeric(6,2) | |
| SquareFeet | Integer | |
| Latitude | Decimal(9,6) | |
| Longitude | Decimal(9,6) | |
| ListedDate | Varchar(100) | ISO date, `yyyy-MM-dd` |
| Status | Varchar(50) | Stored lowercase: `active`, `pending` or `sold` |
| Description | Varchar(250) | |

---

## 4. Common API conventions

- All bodies are JSON, field names in **camelCase**.
- Query parameter names keep the draft's spelling (`min_price`, `free_text`, etc.), plus the new `lat` and `lng`.
- **Pagination** applies to every GET list endpoint:
  - `page`: integer, **0-based**, default `0`, must be `>= 0`.
  - `size`: integer, default `20`, must be between `1` and `100`.
  - Every paginated response includes a `meta` object:
    ```json
    { "page": 0, "size": 20, "totalPages": 5, "totalElements": 93 }
    ```
  - A page past the end returns an empty list with correct `meta`.
- **Validation errors on query parameters** return `422` with this body:
  ```json
  {
    "search": [
      { "field": "min_price", "message": "Validation Failure, min price (500000) is greater than max price (300000)" }
    ]
  }
  ```
  All parameter errors are reported together, not just the first one.
- Malformed request bodies (not valid JSON, or not a JSON array where one is required) return `400` with `{ "error": "<message>" }`.

---

## 5. Marketplace Admin API

### 5.1 `GET /marketplace-admin/listings`

Returns the contents of the Listing table, paginated, ordered by `listingId` ascending.

Query parameters: `page`, `size`.

Response `200`:
```json
{
  "listings": [
    {
      "listingId": "zillow:12345",
      "id": "12345",
      "source": "zillow",
      "address": "123 Main St",
      "city": "Alexandria",
      "state": "VA",
      "zip": "22306",
      "price": 450000.00,
      "currencyCode": "USD",
      "bedrooms": 3,
      "bathrooms": 2.5,
      "sqft": 1850,
      "latitude": 38.789300,
      "longitude": -77.187300,
      "listedDate": "2026-11-20",
      "status": "active",
      "description": "Bright colonial near the park."
    }
  ],
  "meta": { "page": 0, "size": 20, "totalPages": 1, "totalElements": 1 }
}
```

### 5.2 `GET /marketplace-admin/logs` (new)

Returns the ListingLog table with each log entry's errors, paginated, ordered by `logTime` **descending** (most recent first). This backs the admin page.

Query parameters: `page`, `size`.

Response `200`:
```json
{
  "logs": [
    {
      "logId": "01937a2e-...",
      "logTime": "2026-11-25T09:15:02.123Z",
      "id": "12345",
      "source": "zillow",
      "address": "123 Main St",
      "city": "Alexandria",
      "state": "VA",
      "zip": "22306",
      "price": "abc",
      "currencyCode": "USD",
      "bedrooms": "3",
      "bathrooms": "2.5",
      "sqft": "1850",
      "latitude": "38.7893",
      "longitude": "-77.1873",
      "listedDate": "2026-11-20",
      "status": "active",
      "description": "Bright colonial near the park.",
      "accepted": false,
      "errors": [
        { "errorId": "01937a2e-...", "errorSource": "Price", "errorDetails": "Price 'abc' is not a valid number" }
      ]
    }
  ],
  "meta": { "page": 0, "size": 20, "totalPages": 1, "totalElements": 1 }
}
```

`accepted` is `true` when the entry has no errors (it was saved to Listing). All log values are returned as the raw strings that were stored.

### 5.3 `POST /marketplace-admin/listings`

Accepts a JSON array of listings, processes each one, and returns a summary.

Request body:
```json
[
  {
    "id": "12345",
    "source": "zillow",
    "address": "123 Main St",
    "city": "Alexandria",
    "state": "VA",
    "zip": "22306",
    "price": 450000,
    "bedrooms": 3,
    "bathrooms": 2.5,
    "sqft": 1850,
    "latitude": 38.7893,
    "longitude": -77.1873,
    "listedDate": "2026-11-20",
    "status": "active",
    "description": "Bright colonial near the park."
  }
]
```

**Field mapping:**

| Request field | ListingLog / Listing column |
|---|---|
| `id` | SourceId |
| `source` | SourceCode |
| `sqft` | SquareFeet |
| all other fields | the column with the same name |

Any field may arrive as a JSON string, number or null. The server converts each value to its string form for ListingLog and then validates it. `currencyCode` is not part of the payload; it is always `USD`.

Response `200` (even when some or all listings fail validation):
```json
{
  "received": 3,
  "accepted": 2,
  "rejected": 1,
  "results": [
    { "logId": "01937a2e-...", "listingId": "zillow:12345", "accepted": true, "errors": [] },
    { "logId": "01937a2f-...", "listingId": "zillow:999", "accepted": false,
      "errors": [ { "errorSource": "Price", "errorDetails": "Price 'abc' is not a valid number" } ] }
  ]
}
```
`listingId` is `null` in a result when `id` or `source` is missing.

Response `400`: the body is not valid JSON or is not an array.

#### Processing steps (per listing)

1. **Log first.** Save the raw listing to ListingLog (new UUIDv7 `LogId`, `LogTime` = now).
2. **Validate** with a functional pipeline (see below).
3. **Record errors.** Write one ListingError row per failure, linked by `LogId`.
4. **Save if clean.** If there are no errors, upsert into Listing:
   - `ListingId` = `SourceCode:SourceId`.
   - If that ListingId does not exist, insert it.
   - If it exists, update every column with the new values.

Each listing is processed on its own. An error in one listing never stops the others. Listings are processed in array order, so if the same ListingId appears twice in one upload, the later one wins.

#### Validation pipeline

Implement validation as an ordered list of small, independent validator functions (e.g. `Function<RawListing, List<ValidationError>>`). The pipeline passes the listing through every validator in turn and **collects all errors** from all of them (it does not stop at the first failure). Each validator is unit tested on its own.

Every error message must contain the failed value and the reason, e.g. `Price '-5' must be greater than 0`, `State 'Virginia' must be exactly 2 letters`.

| Field | Rules |
|---|---|
| All string fields (`id`, `source`, `address`, `city`, `state`, `zip`, `listedDate`, `status`, `description`) | Must exist and not be empty or whitespace only. Length must fit the Listing column size. |
| `state` | Exactly 2 letters (A–Z, case-insensitive). Stored uppercase. |
| `status` | One of `active`, `pending`, `sold` (case-insensitive). Stored lowercase. |
| `listedDate` | Valid ISO-8601 calendar date in `yyyy-MM-dd` form (e.g. `2026-11-20`). |
| `price` | Must exist, be a number, and be `> 0`. Max 15 digits before the decimal point and 4 after. |
| `bedrooms` | Must exist, be a whole number, and be `>= 0` (0 = studio). Max 32767. |
| `bathrooms` | Must exist, be a number, and be `> 0`. Max 2 decimal places. |
| `sqft` | Must exist, be a whole number, and be `> 0`. |
| `latitude` | Must exist and be a finite number between -90 and +90 inclusive. **Exempt from the `> 0` rule.** Max 6 decimal places (more are rounded). |
| `longitude` | Must exist and be a finite number between -180 and +180 inclusive. **Exempt from the `> 0` rule.** Max 6 decimal places (more are rounded). |
| `listingId` | `source + ":" + id` must be at most 150 characters (`ErrorSource` = `general`). |

A listing whose payload entry is not a JSON object (e.g. a number in the array) is logged with empty fields and one `general` error.

---

## 6. Marketplace API

### 6.1 `GET /marketplace/listing`

Query parameters:

| Parameter | Type | Rules |
|---|---|---|
| `min_price` | integer | Optional. `>= 0`. |
| `max_price` | integer | Optional. `>= 0`. If both are given, `min_price <= max_price`. |
| `min_bedrooms` | integer | Optional. `>= 0`. |
| `min_bathrooms` | **decimal** | Optional. `>= 0`. Allows `1.5` for the "1½+" button. |
| `free_text` | string | Optional. See below. |
| `lat` | decimal | Optional. Default location latitude, -90 to 90. Defaults to `38.7893`. |
| `lng` | decimal | Optional. Default location longitude, -180 to 180. Defaults to `-77.1873`. |
| `page` | integer | Optional. See Section 4. |
| `size` | integer | Optional. See Section 4. |

`lat` and `lng` must be sent together. Sending only one returns `422`.

Non-numeric values for numeric parameters return `422`.

**Filters** (all combined with AND):

- `price >= min_price` and `price <= max_price` (inclusive).
- `bedrooms >= min_bedrooms`.
- `bathrooms >= min_bathrooms`.
- `free_text`: trimmed. If it is not blank, a listing matches when the whole phrase appears as a **case-insensitive substring** of its city, state, zip **or** description. The phrase is not split into separate words.
- Listings with a `listedDate` **in the future** (after today, per the injected Clock, which uses UTC by default) are always excluded.

Response `200`:
```json
{
  "listings": [
    {
      "listingId": "zillow:12345",
      "sourceId": "12345",
      "sourceCode": "zillow",
      "address": "123 Main St",
      "city": "Alexandria",
      "state": "VA",
      "zip": "22306",
      "price": 450000.00,
      "currencyCode": "USD",
      "bedrooms": 3,
      "bathrooms": 2.5,
      "sqft": 1850,
      "latitude": 38.789300,
      "longitude": -77.187300,
      "listedDate": "2026-11-20",
      "status": "active",
      "description": "Bright colonial near the park."
    }
  ],
  "meta": { "page": 0, "size": 20, "totalPages": 1, "totalElements": 1 }
}
```

Response `422`: see Section 4. Example message: `Validation Failure, min price is greater than max price`.

### 6.2 Ordering and ranking

Ordering is computed over the **entire filtered result set first**, and pagination is applied **after** ordering.

**Step 1: group by status.** Results are grouped in this order: `active`, then `pending`, then `sold`.

**Step 2: rank within each group** by a weighted score, highest first.

```
score = 0.4 × distanceScore + 0.4 × priceScore + 0.2 × freshnessScore
```

Each part is a value from 1 to 10. The min/max values used to scale distance and price come from the **whole filtered result set** (all statuses together), so scores are comparable across groups.

**Distance score (40%)**

- Distance = great-circle (haversine) distance in **miles** between the default location (`lat`/`lng`) and the listing.
- `dMax` = the largest distance in the filtered result set.
- `distanceScore = 10 − 9 × (distance / dMax)`.
  - Distance 0 scores 10. The furthest listing scores 1.
  - If `dMax` is 0, every listing scores 10.

**Price score (40%)**

- `pMin` and `pMax` = the lowest and highest price in the filtered result set.
- `priceScore = 10 − 9 × ((price − pMin) / (pMax − pMin))`.
  - The cheapest listing scores 10. The most expensive scores 1.
  - If `pMax == pMin`, every listing scores 10.

**Freshness score (20%)**

- `ageDays` = whole days between `listedDate` and today.
- `freshnessScore = max(1, 10 − floor(ageDays / 5))`.

| Age (days) | 0–4 | 5–9 | 10–14 | … | 40–44 | 45+ |
|---|---|---|---|---|---|---|
| Score | 10 | 9 | 8 | … | 2 | 1 |

**Tie-breakers** (when scores are equal): newer `listedDate` first, then `listingId` ascending, so paging is stable.

Implement the ranking as a pure, framework-free `ListingRanker` class (input: listings, default location, today; output: ordered list) so it can be fully unit tested.

---

## 7. UI

### 7.1 General

- Page title and header: **Some Company**.
- A button in the **top right** of the landing page navigates to the admin page. The admin page has a way back to the landing page.

### 7.2 Default location (client state)

- The default location is held **in memory in React state**, in a shared React context (e.g. `DefaultLocationContext`) so both pages read the same value.
- On app load it is `latitude: 38.7893, longitude: -77.1873`.
- It is set from the admin page (Section 7.4). It is **not** saved to the server or the database, and it **resets to the default on a full page reload**.
- Every marketplace search sends it as the `lat` and `lng` query parameters.

### 7.3 Landing page (search and results)

**Filter bar** (all on one line):

- **Search box** with a search icon on its left, placed left of center. Pressing Enter or clicking the icon runs the search with `free_text`.
- **Price** dropdown, to the right of the search box. It opens a panel with **Minimum** and **Maximum** price inputs and an **Apply** button at the bottom.
  - The UI must not allow min price to be higher than max price: when min > max, show an inline message and disable Apply.
  - Empty inputs mean that bound is not sent.
- **Bedrooms & Bathrooms** dropdown, next to Price. It opens a panel with two rows of toggle buttons:
  - Bedrooms: `Any`, `1+`, `2+`, `3+`, `4+`.
  - Bathrooms: `Any`, `1+`, `1½+`, `2+`, `3+` (sent as `1`, `1.5`, `2`, `3`).
  - Both default to `Any`. When `Any` is selected, that parameter is **not sent**. Otherwise the value is sent as `min_bedrooms` / `min_bathrooms`.

Changing any filter resets to page 0 and runs a new search.

**Results:**

- Listings appear as cards in the order the API returns them.
- Each card shows:
  1. The price, formatted as currency (e.g. `$450,000`).
  2. One line: `3 bd | 2.5 ba | 1,850 sqft | Active` (bedrooms, bathrooms, square feet and status separated by pipes).
  3. The full address: `123 Main St, Alexandria, VA 22306`.
- Pagination controls (Previous / Next and "Page X of Y") under the results.
- An empty state message when there are no results.
- A `422` response shows its error messages above the results.

**Listing popup:** clicking a card opens a modal showing, in order:

1. A **Leaflet map** centered on the listing's latitude/longitude with a marker.
2. The description.
3. The listed date and status.

The modal closes with a close button, the Escape key, or a click outside it.

### 7.4 Admin page

- **Default location:** latitude and longitude input boxes, pre-filled with the current value, and a **Set default location** button. Inputs are validated (latitude -90 to 90, longitude -180 to 180) before saving to the React context. Show a confirmation after saving.
- **Upload:** a button to choose a `.json` file. The client reads the file and:
  - If it cannot be parsed as JSON, or is not a JSON array, show **"File not in correct format"** and do not call the server.
  - Otherwise POST the array to `/marketplace-admin/listings`.
  - If the request fails (network error or non-2xx response), show **"Upload failed"**.
  - On success, show the summary (received / accepted / rejected) and **refresh the log list** from `/marketplace-admin/logs`.
- **Listing log:** shows the ListingLog contents from `GET /marketplace-admin/logs`, most recent `LogTime` first, paginated. Each row shows the log time, source, id, address, status and whether it was accepted. Entries with errors show their errors (ErrorSource and ErrorDetails) with that entry.

---

## 8. Project structure (suggested)

```
/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/.../
│       ├── admin/        (controller, upload service, log queries)
│       ├── marketplace/  (controller, search service, ListingRanker)
│       ├── validation/   (validator functions, pipeline)
│       ├── domain/       (JPA entities, repositories)
│       └── common/       (pagination, error handling, Clock, UUIDv7)
├── frontend/
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/ (pages, components, DefaultLocationContext, api client)
├── sample-data/listings.json
├── docker-compose.yml
├── readme.md
└── applog.md
```

---

## 9. Testing

A full set of unit and integration tests must show that every business requirement is met. `./mvnw test` and `npm test` must both pass.

### 9.1 Backend unit tests (no Spring context)

- **Each validator** individually: valid value, missing, empty/blank, wrong type, boundary values (e.g. latitude exactly -90 and 90, bedrooms 0, price 0), and that messages contain the failed value.
- **Validation pipeline:** collects errors from all validators; a clean listing yields no errors.
- **Field mapping:** `id` → SourceId, `source` → SourceCode, `sqft` → SquareFeet; ListingId built as `source:id`.
- **ListingRanker:** status grouping order; each score formula including edge cases (single result, equal prices, zero distance); freshness table boundaries (day 4 → 10, day 5 → 9, day 45 → 1); weighted total; tie-breakers.
- **Haversine distance** against known values.
- **Search parameter validation:** min > max, negatives, lat without lng, page/size limits.

### 9.2 Backend integration tests (Spring Boot + MockMvc + H2)

- POST a mixed batch: every listing lands in ListingLog; invalid ones produce ListingError rows; valid ones land in Listing; one bad listing doesn't stop the rest; summary counts are correct.
- POST an existing ListingId updates the record rather than duplicating it.
- POST a non-array or malformed body returns `400`.
- `GET /marketplace-admin/logs` is ordered by LogTime descending and includes errors.
- `GET /marketplace-admin/listings` and `/logs` paginate.
- `GET /marketplace/listing`: each filter, free text case-insensitivity across city/state/zip/description, future-dated listings excluded, full ranking order end to end, ranking applied before pagination (page 1 continues page 0's order), `422` cases, and `lat`/`lng` changing the order.

### 9.3 Frontend tests (Vitest + React Testing Library)

- Price dropdown blocks min > max.
- Bed/bath buttons: `Any` omits the parameter; `1½+` sends `1.5`.
- Search sends the default location from context; changing it on the admin page changes the next search.
- Card content and pipe-separated line formatting.
- Popup shows map container, description, date and status.
- Upload: unparsable file shows "File not in correct format"; failed request shows "Upload failed"; success refreshes the log list.

---

## 10. Error handling summary

| Situation | Response |
|---|---|
| Invalid search parameters | `422` with `{ "search": [ { field, message } ] }` |
| Malformed POST body | `400` with `{ "error": ... }` |
| Some or all uploaded listings invalid | `200` with summary; errors stored in ListingError |
| Unexpected server error | `500` with `{ "error": "Internal server error" }` (no stack traces) |

---

## 11. Out of scope

- Authentication or authorization for the admin page.
- Persisting data or the default location across restarts or page reloads.
- Currencies other than USD.

