# Some Company Listings Marketplace

A web app for browsing property listings.

- **Marketplace** (landing page, `/`): search and filter listings, ranked by distance, price and freshness, with a map popup for each listing.
- **Marketplace Admin** (`/admin`): upload listings as a JSON file, see the processing log with per-listing validation errors, and set the default location used for distance ranking.

| Part | Stack |
|---|---|
| Backend (`backend/`) | Java 21, Spring Boot 3.5, Spring Data JPA, H2 in-memory database, Maven Wrapper |
| Frontend (`frontend/`) | React 18, TypeScript, Vite, Leaflet via react-leaflet (OpenStreetMap tiles) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc; Vitest and React Testing Library |

How the app was built and why is in [applog.md](applog.md).

## Run with Docker

Requires Docker with Compose v2.

```bash
docker compose up --build
```

- App: <http://localhost:3000> (nginx serves the React build and proxies `/marketplace` and `/marketplace-admin` to the backend)
- API: <http://localhost:8080>

The database is in memory, so all data is lost when the backend container stops.

## Run locally

Requires Java 21 and Node.js 20 or newer.

**Backend** (port 8080):

```bash
cd backend
./mvnw spring-boot:run
```

**Frontend** (port 5173), in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. The Vite dev server proxies `/marketplace` and `/marketplace-admin` to `localhost:8080`, so no CORS setup is needed. To point it at another backend, set `BACKEND_URL`, for example `BACKEND_URL=http://localhost:9090 npm run dev`.

## Run the tests

```bash
cd backend && ./mvnw test     # unit tests and Spring Boot + MockMvc + H2 integration tests
cd frontend && npm test       # Vitest + React Testing Library
```

## Try it with the sample file

1. Start the app (Docker or locally).
2. Open the app and click **Marketplace Admin** in the top right.
3. Click **Choose JSON file** and pick [`sample-data/listings.json`](sample-data/listings.json).
4. The summary shows 36 received, 15 accepted and 21 rejected, and the listing log shows each entry with its errors.
5. Click **Back to marketplace** to browse the accepted listings.

The sample file has:

- 15 valid listings around Alexandria, VA in all three statuses (`active`, `pending`, `sold`). One of them is dated in the future (`2026-12-15`), so it is saved but hidden from search until that date.
- 21 invalid entries, one per validation rule (missing id, blank city, `Virginia` as a state, unknown status, impossible date, wrong date format, non-numeric / zero / negative price, missing price, fractional or negative bedrooms, zero bathrooms, three decimal places of bathrooms, zero square feet, latitude and longitude out of range, a ListingId over 150 characters, a description over 250 characters, an entry with several problems at once, and a bare number instead of an object). Each one's `description` says what it tests.

You can also upload from the command line:

```bash
curl -X POST http://localhost:8080/marketplace-admin/listings \
  -H 'Content-Type: application/json' \
  --data @sample-data/listings.json
```

### Upload file format

A JSON array of objects. Values can be JSON strings or numbers. `currencyCode` is always `USD` and is not part of the file.

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

## API summary

| Method and path | Purpose |
|---|---|
| `GET /marketplace/listing` | Search. Query: `min_price`, `max_price`, `min_bedrooms`, `min_bathrooms`, `free_text`, `lat`, `lng`, `page`, `size`. Invalid parameters return `422` with `{ "search": [ { "field", "message" } ] }`. |
| `GET /marketplace-admin/listings` | The Listing table, by `listingId`, paginated. |
| `GET /marketplace-admin/logs` | The ListingLog with each entry's errors, most recent first, paginated. |
| `POST /marketplace-admin/listings` | Upload a JSON array. Returns `200` with a summary even when listings fail validation; `400` if the body is not a JSON array. |

Each search result also has a `ranking` object, so the order can be checked by hand. It is not shown in the UI.

```json
"ranking": {
  "score": 2.6,
  "distanceScore": 1.0,
  "priceScore": 1.0,
  "freshnessScore": 9.0,
  "distanceMiles": 69.0941,
  "ageDays": 7
}
```

`score = 0.4 × distanceScore + 0.4 × priceScore + 0.2 × freshnessScore`. The values are rounded to 4 decimal places for display, and the ordering uses the unrounded values.

Pagination: `page` is 0-based (default 0), `size` is 1 to 100 (default 20), and every list response has `meta: { page, size, totalPages, totalElements }`.

The H2 console is not enabled. To inspect the data, use the two admin `GET` endpoints.
