# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app

# Copiar archivos de dependencias
COPY package*.json ./
RUN npm ci

# Copiar código fuente
COPY . .

# Build de la aplicación
RUN npm run build

# Stage 2: Production
FROM nginx:1.26-alpine AS runtime

# Copiar configuración de nginx
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# Copiar el build desde la etapa de build
COPY --from=build /app/dist/mortal-kombat-app/browser/ /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

