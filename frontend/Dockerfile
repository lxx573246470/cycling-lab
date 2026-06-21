# syntax=docker/dockerfile:1.7
FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json* ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

COPY . .
ARG VITE_API_BASE=/api/v1
ARG VITE_AUTH_DISABLED=true
ENV VITE_API_BASE=$VITE_API_BASE
ENV VITE_AUTH_DISABLED=$VITE_AUTH_DISABLED
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
