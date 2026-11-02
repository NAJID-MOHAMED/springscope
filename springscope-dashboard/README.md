# SpringScope Dashboard

Standalone React dashboard for the SpringScope Spring Boot inspection API.

## Requirements and setup

Node.js 20+ and npm. From this directory:

```sh
npm install
Copy-Item .env.example .env
npm run dev
```

By default Vite proxies `/springscope` to `http://localhost:8080`, so local development needs no backend CORS changes. Set `VITE_API_BASE_URL` to the API base (default shown in `.env.example`) when using a different backend. Restart Vite after changing environment values.

## Commands

```sh
npm run build
npm run test
npm run lint
```

## Architecture

Feature pages live under `src/features`; Axios API services and actual backend response interfaces live under `src/lib/api` and `src/types`; TanStack Query handles caching and health polling. React Flow displays the backend graph. Edges show recorded Spring dependency metadata and do not prove runtime calls. The backend currently does not provide dependency lists in individual bean details or runtime traces; the dashboard does not fabricate either.

Start the backend from repository root using `./mvnw -pl springscope-demo spring-boot:run` (Windows: `./mvnw.cmd -pl springscope-demo spring-boot:run`).
