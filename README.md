# Rate Limiter API

A Spring Boot REST API that demonstrates **per-user API rate limiting** with the Token Bucket algorithm. Users select a subscription plan, authenticate with a JWT, and call protected APIs only within that plan's hourly quota.

## Features

- User registration and login with BCrypt password hashing
- JWT authentication for private endpoints
- Per-user rate limiting with [Bucket4j](https://github.com/bucket4j/bucket4j) and Redis
- MySQL persistence with Flyway migrations
- Swagger UI for interactive API testing
- Docker Compose setup for the API, MySQL, and Redis

## Architecture

```text
Client / Swagger UI
        |
        v
JWT Authentication Filter ----> validates token and identifies the user
        |
        v
Rate Limit Filter ------------> consumes a token from the user's Redis bucket
        |
        v
Controller -> Service -> Repository -> MySQL
```

MySQL stores permanent business data. Redis stores temporary rate-limit buckets. The JWT identifies the user on every private request.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Application language and runtime |
| Spring Boot 3 | REST API framework |
| Spring Security 6 | Security filter chain |
| MySQL 8 | Persistent users and plans |
| Redis | Distributed rate-limit storage |
| Bucket4j | Token Bucket implementation |
| Flyway | Database migrations |
| Docker Compose | Local multi-container setup |
| Swagger / OpenAPI | Interactive API documentation |

## Subscription Plans

| Plan | Limit per Hour |
|---|---:|
| FREE | 20 |
| BUSINESS | 40 |
| PROFESSIONAL | 100 |

The plans are inserted by [V002__adding_plans.sql](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/resources/db/migration/V002__adding_plans.sql).

## Request Flow

1. Call `GET /api/v1/plan` to get available plans.
2. Create an account through `POST /api/v1/user`, choosing a plan ID.
3. Log in through `POST /api/v1/auth/login` to receive a JWT access token.
4. [JwtAuthenticationFilter](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/filter/JwtAuthenticationFilter.java) validates the JWT and puts the user ID into Spring Security's context.
5. [RateLimitFilter](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/filter/RateLimitFilter.java) retrieves the user's Bucket4j bucket from Redis and consumes one token.
6. If a token remains, the request reaches the controller; otherwise the API returns `429 Too Many Requests`.

```text
Create user -> Login -> Receive JWT -> Call private API
                                      |
                                      v
                           JWT validation -> Redis quota check
                                      |
                         allowed -------+------- exhausted
                            |                       |
                         200 response            429 response
```

## API Endpoints

| Method | Endpoint | Authentication | Description |
|---|---|---|---|
| `GET` | `/api/v1/plan` | No | Lists subscription plans. |
| `POST` | `/api/v1/user` | No | Creates a user and associates their selected plan. |
| `POST` | `/api/v1/auth/login` | No | Validates credentials and returns a JWT token. |
| `GET` | `/api/v1/joke` | JWT | Sample protected endpoint used to demonstrate rate limiting. |
| `PUT` | `/api/v1/plan` | JWT | Changes the authenticated user's active plan. |

`PUT /api/v1/plan` uses [`@BypassRateLimit`](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/configuration/BypassRateLimit.java), allowing an exhausted user to change their plan.

## Local Setup with Docker

### Prerequisite

Install and start [Docker Desktop](https://www.docker.com/products/docker-desktop/).

### Start the application

From the project root:

```bash
docker compose up --build -d
```

Docker starts:

| Container | Purpose |
|---|---|
| `backend-application` | Spring Boot API on port `8080` |
| `mysql-datasource` | MySQL database |
| `redis-cache` | Redis rate-limit cache |

Open Swagger UI at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html).

### Stop and restart

```bash
# Stop containers but retain their data
docker compose stop

# Start previously built containers
docker compose start

# Rebuild after Java, configuration, or Dockerfile changes
docker compose up --build -d
```

## Using Swagger UI

1. Execute `GET /api/v1/plan` and copy a plan `Id`.
2. Execute `POST /api/v1/user` using an email, password, and that `PlanId`.
3. Execute `POST /api/v1/auth/login` with the same credentials.
4. Copy `AccessToken` from the response.
5. Click **Authorize** and paste the token value.
6. Execute `GET /api/v1/joke`.

Example create-user request:

```json
{
  "EmailId": "test@example.com",
  "Password": "MySecurePassword123",
  "PlanId": "paste-a-plan-id-here"
}
```

## Rate-Limit Responses

Successful protected requests include:

| Header | Meaning |
|---|---|
| `X-Rate-Limit-Remaining` | Requests remaining in the current bucket. |

An exhausted quota returns `429 Too Many Requests` with `X-Rate-Limit-Retry-After-Seconds`:

```json
{
  "Status": "429 TOO_MANY_REQUESTS",
  "Description": "API request limit linked to your current plan has been exhausted."
}
```

## Important Project Files

| File | Responsibility |
|---|---|
| [application.yml](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/resources/application.yml) | Port, database, Redis, JWT, and Swagger settings. |
| [SecurityConfiguration.java](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/configuration/SecurityConfiguration.java) | Public routes, stateless security, and custom filter order. |
| [JwtAuthenticationFilter.java](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/filter/JwtAuthenticationFilter.java) | JWT validation and request authentication. |
| [RateLimitFilter.java](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/filter/RateLimitFilter.java) | Per-user quota enforcement. |
| [RateLimitingService.java](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/service/RateLimitingService.java) | Bucket creation, retrieval, and reset. |
| [RedisConfiguration.java](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/configuration/RedisConfiguration.java) | Redis and Bucket4j integration. |
| [JwtUtility.java](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/java/com/behl/overseer/utility/JwtUtility.java) | JWT generation and validation. |
| [Flyway migrations](https://github.com/tejas123-creator/Rate-Limiter/tree/main/src/main/resources/db/migration) | Database schema and default plans. |
| [docker-compose.yml](https://github.com/tejas123-creator/Rate-Limiter/blob/main/docker-compose.yml) | Local API, MySQL, and Redis containers. |

## Database Model

The schema comes from [V001__creating_database_tables.sql](https://github.com/tejas123-creator/Rate-Limiter/blob/main/src/main/resources/db/migration/V001__creating_database_tables.sql).

```text
users 1 --- * user_plan_mappings * --- 1 plans
```

- `users`: email, password hash, and creation time.
- `plans`: plan name and hourly request limit.
- `user_plan_mappings`: plan-assignment history; one mapping is active per user.

## Testing

The project includes unit tests and Testcontainers integration tests.

```bash
mvn test
mvn integration-test
```

> Native Maven builds require Java 21. Docker is the easiest local option because it provides Java, MySQL, and Redis.
