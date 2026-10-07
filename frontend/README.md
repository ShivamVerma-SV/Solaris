# Solaris frontend

Angular 22 frontend for the Solaris solar-energy monitoring backend.

## Local development

Start the Spring Boot backend on port `8080`, then run:

```bash
npm install
npm start
```

The Angular development server opens on `http://localhost:4200`. Requests to `/api` are forwarded to `http://localhost:8080` by `proxy.conf.json`, so no backend CORS change is required for local development.

The production API base is also `/api`, configured in `src/environments/environment.production.ts`. Deploy the frontend behind the same origin as the backend, or adjust that environment value for the target deployment.

## Commands

```bash
npm run build
npm test -- --watch=false
npx tsc --noEmit -p tsconfig.app.json
```

## Authentication

The application uses the backend's `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, and `/api/auth/logout` endpoints. Access tokens are attached by a functional HTTP interceptor. A single shared refresh operation coordinates concurrent `401` responses and retries requests with the rotated token pair.

Routes are guarded for the backend roles `ADMIN` and `HOMEOWNER`. These client-side restrictions improve navigation only; the Spring Security configuration remains authoritative.

## Themes

Solaris includes purpose-built light and dark themes using semantic design tokens. The user's selection is stored locally, with the operating-system preference used when no selection exists.
