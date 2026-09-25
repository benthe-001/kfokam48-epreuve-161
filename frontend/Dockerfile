# syntax=docker/dockerfile:1

# ---------- Étape 1 : build frontend ----------
FROM node:24-alpine AS build
WORKDIR /app

# npm ci : installation reproductible depuis package-lock.json
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---------- Étape 2 : serveur statique ----------
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80