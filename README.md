# Solaris

Solaris is a web-based Solar Energy Monitoring System for tracking solar energy production, consumption, battery storage, devices, and system alerts.

The project is being developed as a full-stack application with a Java/Spring Boot backend and a separate frontend planned for the next stage.

## Project Status

**Backend:** API foundation complete

**Frontend:** Planned

The backend provides stateless JWT authentication with rotating Redis-backed refresh tokens, role and ownership authorization, admin management APIs, homeowner telemetry APIs, alert monitoring, and JDBC-based energy reporting.

## Planned Features

- User registration and authentication
- Admin and homeowner roles
- Solar device management
- Energy production monitoring
- Energy consumption tracking
- Battery/storage monitoring
- Energy and storage history
- System alerts and notifications
- Admin system settings
- User and device management
- Dashboard with energy statistics and trends

## Tech Stack

### Backend

- Java 25
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT
- Hibernate
- Lombok
- Gradle

### Frontend

Planned separately. The frontend will consume the backend REST APIs and provide dashboards for homeowners and administrators.

## Architecture

The backend follows a layered structure:

```text
Client
   │
   ▼
REST API
   │
   ▼
Controller
   │
   ▼
Service
   │
   ▼
Repository
   │
   ▼
PostgreSQL
```

## Local backend

Copy `.env.example` to `.env`, replace the placeholder values, and start the data services:

```bash
docker compose up -d postgres redis
```

Run the backend locally with Gradle:

```bash
cd backend
set -a
source ../.env
set +a
./gradlew bootRun
```

The API uses `/api/auth/**`, `/api/admin/**`, and `/api/homeowner/**`. Access tokens are sent as `Authorization: Bearer <accessToken>`.

Run verification with:

```bash
cd backend
./gradlew clean test
./gradlew clean build
```

## License

This project is currently being developed as a personal/academic project.
