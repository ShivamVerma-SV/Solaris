# Solaris

Solaris is a web-based Solar Energy Monitoring System for tracking solar energy production, consumption, battery storage, devices, and system alerts.

The project is being developed as a full-stack application with a Java/Spring Boot backend and a separate frontend planned for the next stage.

## Project Status

**Backend:** Under development  
**Frontend:** Planned

The backend is currently being built around the core API, authentication, database structure, security, and monitoring functionality. Frontend development will be started after the backend foundation is in place.

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

## License

This project is currently being developed as a personal/academic project.

