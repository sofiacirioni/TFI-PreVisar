# =============================================================================
# Frontend PreVisar — imagen multi-stage
# =============================================================================
# Etapa 1 (build): compila la SPA con Node.
# Etapa 2 (runtime): nginx sirviendo los estáticos. Node no viaja a la imagen
# final: una vez compilada, la app es HTML/CSS/JS y no necesita runtime.
# =============================================================================

# ----------------------------- Etapa 1: build -------------------------------
FROM node:22-alpine AS build
WORKDIR /build

# package.json + lock primero: si no cambiaron, Docker reutiliza la capa de
# node_modules y se saltea el npm ci en cada build.
COPY package.json package-lock.json ./
# npm ci (no npm install): instala exactamente lo que fija el lock, que es de lo
# que depende que el build sea reproducible.
RUN npm ci

COPY . .
# Configuración por defecto = production (ver angular.json), que es la que aplica
# environment.ts con las rutas relativas /api y /auth.
RUN npm run build

# ---------------------------- Etapa 2: runtime ------------------------------
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

# El builder de Angular deja los estáticos en dist/<proyecto>/browser.
COPY --from=build /build/dist/frontend/browser /usr/share/nginx/html

EXPOSE 80

# La imagen de nginx ya trae su propio ENTRYPOINT/CMD.
