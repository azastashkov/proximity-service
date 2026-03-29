# Proximity Service

A microservice-based system for discovering nearby businesses (restaurants, hotels, theaters, museums) by location and radius. Uses geohash-based spatial indexing for efficient location queries.

## Architecture

Two stateless Spring Boot microservices behind an Nginx load balancer:

- **LBS Service** — handles nearby business searches (read-only)
- **Business Service** — handles business CRUD operations

See `docs/proximity-service.drawio` for the full architecture diagram.

### Tech Stack

- Java 21, Spring Boot 3.4
- PostgreSQL 16 (primary + streaming replica)
- Redis 7 (caching)
- Nginx (load balancer)
- Prometheus + Grafana (monitoring)
- Docker Compose

## Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 21 JDK
- Gradle 8.11+

### Build & Run

```bash
# Build all modules
./gradlew build

# Build fat JAR for load test client
./gradlew :load-test-client:fatJar

# Start all services
docker compose up --build -d

# Verify services are running
curl http://localhost/v1/search/nearby?latitude=40.7580&longitude=-73.9855
curl http://localhost/v1/businesses/1
```

### Run Load Tests

```bash
docker compose --profile test up load-client
```

## API

### Nearby Search (LBS Service)

```
GET /v1/search/nearby?latitude={lat}&longitude={lng}&radius={r}
```

- `latitude` (required): -90 to 90
- `longitude` (required): -180 to 180
- `radius` (optional): 0.5, 1, 2, 5, 20 km (default: 5)

### Business CRUD (Business Service)

```
GET    /v1/businesses/{id}     # Get business details
POST   /v1/businesses          # Create business
PUT    /v1/businesses/{id}     # Update business
DELETE /v1/businesses/{id}     # Delete business
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| Nginx | 80 | API gateway / load balancer |
| LBS Service | 8080 (internal) | Nearby search (2 replicas) |
| Business Service | 8081 (internal) | Business CRUD (2 replicas) |
| PostgreSQL Primary | 5432 | Write database |
| PostgreSQL Replica | 5433 | Read database |
| Redis | 6379 | Cache |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Dashboards (admin/admin) |

## Geohash Algorithm

The service uses geohash-based spatial indexing:

1. Each business location is encoded as geohashes at precisions 4, 5, and 6
2. Nearby search computes the target geohash + 8 neighbors at the appropriate precision
3. Results are filtered by exact Haversine distance

| Radius | Precision | Cell Size |
|--------|-----------|-----------|
| 0.5 km | 6 | ~1.2km x 0.6km |
| 1-2 km | 5 | ~4.9km x 4.9km |
| 5-20 km | 4 | ~39km x 19.5km |

## Monitoring

Access Grafana at http://localhost:3000 (admin/admin). Pre-provisioned dashboard includes:

- Request rate and latency (p95/p99) per service
- Error rates
- JVM memory and GC metrics
- Load test throughput and latency

## Project Structure

```
proximity-service/
├── common/              # Shared entities, DTOs, geohash utilities
├── lbs-service/         # Location-Based Service
├── business-service/    # Business CRUD Service
├── load-test-client/    # Custom Java load test client
├── docker/              # Nginx, PostgreSQL, Prometheus configs
├── monitoring/          # Grafana dashboards and provisioning
├── docs/                # Architecture diagram
└── docker-compose.yml
```
