# Proximity Service Design Spec

## Context

Build a proximity service that lets users discover nearby businesses (restaurants, hotels, theaters, museums, etc.) by location and radius. The system must handle high read traffic for location searches with low latency while supporting business CRUD operations from owners.

## Technology Stack

- **Language:** Java 21 (virtual threads)
- **Framework:** Spring Boot 3.4
- **Build:** Gradle with Groovy DSL (multi-project)
- **Database:** PostgreSQL (primary + replica with streaming replication)
- **Cache:** Redis
- **Load Balancer:** Nginx
- **Monitoring:** Prometheus + Grafana
- **Containerization:** Docker Compose

## Architecture

Two separate Spring Boot microservices behind an Nginx load balancer:

- **LBS Service** — read-only, handles nearby business searches using geohash algorithm
- **Business Service** — handles business CRUD, manages cache invalidation

Both services are stateless and horizontally scalable (2 replicas each behind Nginx).

## Project Structure

```
proximity-service/
├── settings.gradle
├── build.gradle                    # root: shared deps, Java 21, Spring Boot plugin
├── common/                         # shared DTOs, geohash utils, constants
│   ├── build.gradle
│   └── src/main/java/com/proximityservice/common/
│       ├── dto/
│       │   ├── BusinessDto.java
│       │   ├── NearbySearchRequest.java
│       │   └── NearbySearchResponse.java
│       ├── geohash/
│       │   ├── GeoHashUtil.java       # encode/decode, neighbors, precision mapping
│       │   └── HaversineUtil.java     # distance calculation
│       └── model/
│           └── Business.java          # shared entity
├── lbs-service/
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/proximityservice/lbs/
│       ├── LbsApplication.java
│       ├── controller/
│       │   └── NearbySearchController.java
│       ├── service/
│       │   └── NearbySearchService.java
│       └── repository/
│           └── GeospatialIndexRepository.java
├── business-service/
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/proximityservice/business/
│       ├── BusinessApplication.java
│       ├── controller/
│       │   └── BusinessController.java
│       ├── service/
│       │   └── BusinessService.java
│       ├── repository/
│       │   ├── BusinessRepository.java
│       │   └── GeospatialIndexRepository.java
│       └── cache/
│           └── CacheInvalidationService.java
├── load-test-client/
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/proximityservice/loadtest/
│       ├── LoadTestApplication.java
│       ├── scenario/
│       │   ├── NearbySearchScenario.java
│       │   └── BusinessCrudScenario.java
│       └── metrics/
│           └── MetricsCollector.java
├── docker/
│   ├── nginx/
│   │   └── nginx.conf
│   ├── postgres/
│   │   ├── primary/
│   │   │   └── init.sql
│   │   └── replica/
│   │       └── setup-replica.sh
│   └── prometheus/
│       └── prometheus.yml
├── monitoring/
│   └── grafana/
│       └── dashboards/
│           └── proximity-service.json
├── docker-compose.yml
├── docs/
│   └── proximity-service.drawio
└── README.md
```

## Data Schema

### Business Table

```sql
CREATE TABLE business (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    address         VARCHAR(500),
    city            VARCHAR(100),
    state           VARCHAR(100),
    country         VARCHAR(50),
    zip_code        VARCHAR(20),
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    category        VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    website         VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_business_category ON business(category);
```

### Geospatial Index Table

```sql
CREATE TABLE geospatial_index (
    geohash       VARCHAR(12) NOT NULL,
    business_id   BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    PRIMARY KEY (geohash, business_id)
);

CREATE INDEX idx_geospatial_geohash ON geospatial_index(geohash);
```

Each business is stored with geohash entries at three precisions:
- Precision 4 (~39km cells) — used for 5km and 20km radius searches
- Precision 5 (~4.9km cells) — used for 1km and 2km radius searches
- Precision 6 (~1.2km cells) — used for 0.5km radius searches

This means each business has 3 rows in the geospatial_index table. When a search comes in, only the rows at the matching precision are queried.

## Geohash Algorithm

### Precision Mapping

| Radius (km) | Geohash Precision | Approx Cell Size |
|-------------|-------------------|-------------------|
| 0.5          | 6                 | ~1.2km x 0.6km   |
| 1            | 5                 | ~4.9km x 4.9km   |
| 2            | 5                 | ~4.9km x 4.9km   |
| 5            | 4                 | ~39km x 19.5km   |
| 20           | 4                 | ~39km x 19.5km   |

### Search Algorithm

1. Validate radius is one of: 0.5, 1, 2, 5, 20 km
2. Compute the geohash of the user's location at the appropriate precision
3. Get the 8 neighboring geohash cells (to handle boundary cases)
4. Query Redis for business IDs in all 9 cells (`geo:{geohash}` keys)
5. On cache miss, fall back to PostgreSQL `geospatial_index` table
6. Fetch business details for each ID from Redis (`biz:{id}` keys), fallback to DB
7. Filter results by Haversine distance (exact radius check)
8. Sort by distance ascending, return results

### Haversine Formula

Standard implementation for calculating great-circle distance between two lat/lng points. Used as the final filter after the coarse geohash lookup to ensure precise radius matching.

## API Design

### LBS Service (port 8080)

#### GET /v1/search/nearby

**Parameters:**
- `latitude` (required) — double, range [-90, 90]
- `longitude` (required) — double, range [-180, 180]
- `radius` (optional) — double, default 5.0, must be one of: 0.5, 1, 2, 5, 20

**Response (200):**
```json
{
  "businesses": [
    {
      "id": 1,
      "name": "Joe's Pizza",
      "address": "123 Main St",
      "city": "New York",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "category": "restaurant",
      "distance": 0.35
    }
  ],
  "total": 15
}
```

**Error Response (400):**
```json
{
  "error": "Invalid radius. Must be one of: 0.5, 1, 2, 5, 20"
}
```

### Business Service (port 8081)

#### GET /v1/businesses/{id}

**Response (200):** Full business object
**Response (404):** `{"error": "Business not found"}`

#### POST /v1/businesses

**Request body:**
```json
{
  "name": "Joe's Pizza",
  "description": "Best pizza in town",
  "address": "123 Main St",
  "city": "New York",
  "state": "NY",
  "country": "US",
  "zipCode": "10001",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "category": "restaurant",
  "phone": "+1-555-0123",
  "website": "https://joespizza.example.com"
}
```

**Response (201):** Created business object with `id`

#### PUT /v1/businesses/{id}

**Request body:** Same as POST (partial updates supported)
**Response (200):** Updated business object

#### DELETE /v1/businesses/{id}

**Response (204):** No content

### Side Effects on Write Operations

When a business is created, updated, or deleted:
1. PostgreSQL `business` table is updated
2. `geospatial_index` entries are updated (recalculated on location change)
3. Redis cache is invalidated:
   - Remove/update `geo:{geohash}` entries at all stored precisions
   - Remove/update `biz:{id}` entry

## Caching Strategy

### Redis Key Structure

- `geo:{geohash}` → JSON array of business IDs in that geohash cell
  - Example: `geo:dr5ru7` → `[1, 5, 23, 89]`
  - TTL: 1 hour (configurable)

- `biz:{business_id}` → JSON serialized business object
  - Example: `biz:42` → `{"id":42,"name":"Joe's Pizza",...}`
  - TTL: 1 hour (configurable)

### Cache Flow (Read — LBS Service)

```
Request → Compute geohash + neighbors
       → Check Redis for geo:{hash} keys
       → Cache HIT: get business IDs
       → Cache MISS: query PostgreSQL, populate cache
       → For each business ID, check Redis biz:{id}
       → Cache HIT: return business
       → Cache MISS: query PostgreSQL, populate cache
       → Filter by Haversine distance
       → Return sorted results
```

### Cache Invalidation (Write — Business Service)

```
Business created  → INSERT geo entries → SET geo:{hash} and biz:{id} in Redis
Business updated  → UPDATE geo entries → DEL old geo:{hash}, SET new geo:{hash} and biz:{id}
Business deleted  → DELETE geo entries → DEL geo:{hash} and biz:{id}
```

## Infrastructure

### Docker Compose Services

| Service | Image | Port | Notes |
|---------|-------|------|-------|
| nginx | nginx:alpine | 80 | Routes /v1/search/* → lbs, /v1/businesses/* → business |
| lbs-service | custom | 8080 (internal) | 2 replicas |
| business-service | custom | 8081 (internal) | 2 replicas |
| postgres-primary | postgres:16 | 5432 | WAL streaming, init.sql for schema |
| postgres-replica | postgres:16 | 5433 | Streaming replication from primary |
| redis | redis:7-alpine | 6379 | Default config |
| prometheus | prom/prometheus | 9090 | Scrapes both services |
| grafana | grafana/grafana | 3000 | Pre-provisioned dashboards |
| load-client | custom | — | Profile: test |

### Nginx Configuration

```nginx
upstream lbs_backend {
    server lbs-service:8080;
}

upstream business_backend {
    server business-service:8081;
}

server {
    listen 80;
    resolver 127.0.0.11 valid=10s;

    location /v1/search/ {
        proxy_pass http://lbs_backend;
    }

    location /v1/businesses {
        proxy_pass http://business_backend;
    }
}
```

Docker's internal DNS (127.0.0.11) resolves service names to all replica IPs for round-robin load balancing.

### PostgreSQL Replication

- Primary: configured with `wal_level=replica`, `max_wal_senders=3`
- Replica: uses `pg_basebackup` to initialize, then streams WAL from primary
- Init scripts handle replication user creation and `pg_hba.conf` configuration
- LBS Service connects to replica (read-only), Business Service connects to primary (read-write)

## Monitoring

### Prometheus

Scrape configuration targets both services at `/actuator/prometheus` (Micrometer).

Metrics collected:
- `http_server_requests_seconds` — request latency per endpoint
- `cache_gets_total`, `cache_puts_total` — cache hit/miss tracking
- `db_query_duration_seconds` — database query timing (custom)
- Standard JVM metrics (memory, GC, threads)

### Grafana Dashboards

Pre-provisioned dashboard with panels:
- Request rate and latency (p50/p95/p99) per service and endpoint
- Cache hit ratio (geo cache and business cache separately)
- Database query latency
- JVM memory usage and GC activity
- Active HTTP connections
- Load test throughput (when load client is running)

## Load Test Client

Custom Java 21 application using `java.net.http.HttpClient` with virtual threads.

### Configuration (environment variables)

- `TARGET_URL` — Nginx URL (default: `http://nginx:80`)
- `CONCURRENCY` — number of virtual threads (default: 50)
- `DURATION_SECONDS` — test duration (default: 300)
- `SEARCH_RATIO` — percentage of search vs CRUD requests (default: 0.8)

### Scenarios

1. **Nearby Search** — random lat/lng within a configured bounding box, random valid radius
2. **Business CRUD** — create a business, read it, update it, delete it

### Output

- Console: live stats every 5 seconds (RPS, latency p50/p95/p99, error rate)
- Prometheus endpoint (`/metrics` on port 8082) for Grafana visualization

### Startup

```bash
docker compose --profile test up load-client
```

## Testing Strategy

### Unit Tests (JUnit 5)

- `GeoHashUtilTest` — encode/decode, neighbor calculation, precision mapping
- `HaversineUtilTest` — distance calculations with known coordinate pairs
- `NearbySearchServiceTest` — search logic with mocked repositories/cache
- `BusinessServiceTest` — CRUD logic with mocked repositories
- `CacheInvalidationServiceTest` — cache key management

### Integration Tests (Testcontainers)

- `GeospatialIndexRepositoryIT` — geohash queries against real PostgreSQL
- `BusinessRepositoryIT` — CRUD against real PostgreSQL
- `RedisCacheIT` — cache operations against real Redis
- `NearbySearchIntegrationIT` — full search flow with real DB + Redis

### API Tests (@WebMvcTest)

- `NearbySearchControllerTest` — request validation, response format
- `BusinessControllerTest` — CRUD endpoint validation, error responses

## Verification

1. `./gradlew build` — compiles all modules, runs unit tests
2. `docker compose up -d` — starts all infrastructure + services
3. Verify services are healthy:
   - `curl http://localhost/v1/search/nearby?latitude=40.7128&longitude=-74.0060`
   - `curl -X POST http://localhost/v1/businesses -H 'Content-Type: application/json' -d '{"name":"Test","latitude":40.7,"longitude":-74.0,"category":"test"}'`
4. Check Grafana at `http://localhost:3000` — dashboards show metrics
5. Run load test: `docker compose --profile test up load-client`
6. Verify load test metrics appear in Grafana
7. Check PostgreSQL replication: write via business-service, read via lbs-service
