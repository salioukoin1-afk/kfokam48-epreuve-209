# Frontend KFOKAM48 — build Vite puis service statique
FROM node:20-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci --no-fund --no-audit
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
# Le conteneur frontend expose l'UI ; les appels /api sont proxifiés par nginx vers le backend.
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
