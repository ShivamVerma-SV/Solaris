# syntax=docker/dockerfile:1

# This matches the Node/npm generation used by the lockfile and is only present in the build stage.
FROM node:26.5-alpine AS build
WORKDIR /workspace

RUN npm install --global npm@12.0.2
COPY package.json package-lock.json ./
RUN npm ci

COPY angular.json tsconfig*.json ./
COPY public ./public
COPY src ./src
RUN npm run build

FROM nginx:1.28-alpine AS runtime
COPY nginx.conf /etc/nginx/nginx.conf
COPY --from=build --chown=nginx:nginx /workspace/dist/frontend/browser /usr/share/nginx/html

USER nginx
EXPOSE 8080
ENTRYPOINT []
CMD ["nginx", "-g", "daemon off;"]
