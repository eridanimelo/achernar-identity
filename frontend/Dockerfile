FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production
FROM nginxinc/nginx-unprivileged:alpine
COPY --from=build /app/dist/identity/browser /usr/share/nginx/html
RUN mkdir -p /etc/nginx/snippets
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
