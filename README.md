# Jackpot Service

[![CI](https://github.com/thothbot/sporty/actions/workflows/ci.yml/badge.svg)](https://github.com/thothbot/sporty/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/thothbot/sporty/branch/main/graph/badge.svg)](https://codecov.io/gh/thothbot/sporty)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Backend service for Jackpot contribution and reward management. Designed for high-performance with 10K+ RPS capability.

## Tech Stack

- **Java 21** with virtual threads
- **Spring Boot 3.5.5**
- **Spring Kafka** for async message processing
- **H2 Database** (in-memory for development)
- **Docker & Docker Compose**
- **JUnit 5, Mockito, MockMvc** for testing

## Features

- REST API for bet publishing and reward evaluation
- Kafka-based async processing with batch consumption
- Strategy pattern for contribution/reward calculations
- Docker Compose development environment
- Comprehensive code quality tooling

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

## Quick Start

```bash
# Clone repository
git clone <repo>
cd jackpot-service

# Start with Docker Compose
docker-compose up -d

# Or run locally (requires Kafka)
./mvnw spring-boot:run
```

## API Documentation

Once running, access interactive API documentation at:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **Kafka UI**: http://localhost:8090

## Environment Variables

### Kafka Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| KAFKA_BOOTSTRAP_SERVERS | localhost:9092 | Kafka broker addresses |
| KAFKA_TOPIC_BETS | jackpot-bets | Topic for bet messages |
| KAFKA_TOPIC_PARTITIONS | 10 | Number of partitions |
| KAFKA_CONSUMER_GROUP_ID | jackpot-service | Consumer group ID |
| KAFKA_CONSUMER_CONCURRENCY | 10 | Consumer thread count |
| KAFKA_CONSUMER_MAX_POLL_RECORDS | 500 | Batch size per poll |
| KAFKA_CONSUMER_USE_VIRTUAL_THREADS | true | Enable Java 21 virtual threads |
| KAFKA_PRODUCER_ACKS | 1 | Producer acknowledgment |
| KAFKA_TOPIC_BETS_DLQ | jackpot-bets-dlq | Dead letter queue topic |
| KAFKA_RETRY_MAX_ATTEMPTS | 3 | Max retry attempts before DLQ |
| KAFKA_RETRY_INITIAL_INTERVAL_MS | 1000 | Initial retry backoff (ms) |
| KAFKA_RETRY_MULTIPLIER | 2.0 | Exponential backoff multiplier |
| KAFKA_RETRY_MAX_INTERVAL_MS | 10000 | Max retry interval (ms) |

### Application Settings

| Variable | Default | Description |
|----------|---------|-------------|
| SERVER_PORT | 8080 | HTTP server port |
| SPRING_PROFILES_ACTIVE | dev | Active Spring profile |
| LOG_LEVEL | INFO | Root logging level |

## Performance Tuning

### For 10K+ RPS

```bash
# Increase consumer concurrency and batch size
export KAFKA_CONSUMER_CONCURRENCY=20
export KAFKA_CONSUMER_MAX_POLL_RECORDS=1000
export KAFKA_TOPIC_PARTITIONS=20

docker-compose up -d
```

### Key Optimizations

- **Batch consumption**: Process 500 messages per poll
- **Concurrent consumers**: 10 threads per instance
- **Virtual threads**: Java 21 for efficient I/O
- **Partition by jackpotId**: Ordered processing per jackpot
- **Bulk database writes**: Single insert for batch
- **Aggregate updates**: One pool update per jackpot per batch

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw verify
```

## Code Quality

```bash
# Checkstyle
./mvnw checkstyle:check

# SpotBugs + security scanning
./mvnw spotbugs:check

# OWASP dependency vulnerability scan (slow first run)
./mvnw dependency-check:check

# OWASP scan with Docker (official image, uses NVD API key for faster downloads)
# First, create .env.local with your NVD API key (see below)
source .env.local && docker run --rm \
  -v "$(pwd)":/src \
  -v "$(pwd)/.dependency-check-data":/usr/share/dependency-check/data \
  owasp/dependency-check:latest \
  --scan /src \
  --project "jackpot-service" \
  --format HTML --format JSON \
  --out /src/target \
  --nvdApiKey "$NVD_API_KEY"
```

### OWASP Dependency Check Setup

For faster NVD database downloads, get an API key from https://nvd.nist.gov/developers/request-an-api-key

Create `.env.local` (gitignored):
```bash
NVD_API_KEY=your-api-key-here
```

Reports are generated in `target/dependency-check-report.html` and `target/dependency-check-report.json`.

## Architecture

```
                         External Services
                    +--------------------------+
                    |          Kafka           |
                    +------------+-------------+
                          ^      |
                          |      v
+-------------------------+------+-------------------------+
|  Jackpot Service        |      |                         |
|                         |      |                         |
|  +----------+     +-----+------+-----+     +----------+  |
|  |   REST   |---->|     Kafka        |---->|  Batch   |  |
|  |   API    |     |    Producer      |     | Consumer |  |
|  +----+-----+     +------------------+     +----+-----+  |
|       |                                         |        |
|       |           +-----------------+           |        |
|       +---------->|     Service     |<----------+        |
|                   |      Layer      |                    |
|                   +--------+--------+                    |
|                            |                             |
|                   +--------v--------+                    |
|                   |    Database     |                    |
|                   +-----------------+                    |
+----------------------------------------------------------+
```

### Strategy Pattern

- **ContributionStrategy**: Fixed, Variable
- **RewardStrategy**: Fixed, Variable
- Easy to add new strategies (Open/Closed Principle)

## Docker Commands

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f app

# Scale app instances
docker-compose up -d --scale app=3

# Stop all
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Project Structure

```
src/
├── main/
│   ├── java/com/sporty/jackpot/
│   │   ├── config/           # Kafka and app configuration
│   │   ├── controller/       # REST controllers
│   │   ├── dto/              # Data transfer objects
│   │   ├── entity/           # JPA entities
│   │   ├── exception/        # Custom exceptions
│   │   ├── kafka/            # Kafka producer/consumer
│   │   ├── mapper/           # MapStruct mappers
│   │   ├── repository/       # JPA repositories
│   │   ├── service/          # Business logic
│   │   └── strategy/         # Strategy implementations
│   └── resources/
│       └── application.yml   # Configuration
└── test/
    └── java/com/sporty/jackpot/
        ├── controller/       # API tests
        ├── kafka/            # Integration tests
        ├── service/          # Service tests
        └── strategy/         # Strategy tests
```
