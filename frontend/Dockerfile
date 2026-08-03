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

# Renueva la fecha de modificación de todo lo publicado.
#
# nginx deriva el ETag del mtime + tamaño. Los assets del visor de PDF conservan
# el nombre entre versiones de la imagen (viewer-6.0.1165.mjs), así que sin esto
# un navegador con el archivo cacheado revalida, recibe 304 y se queda con la
# copia vieja Y con sus headers viejos: un 304 no reenvía Content-Type. Fue
# exactamente el problema al corregir el MIME de los .mjs — el arreglo no llegaba
# a quien ya había abierto la app. Tocando los archivos, cada build produce un
# ETag nuevo y la primera revalidación devuelve un 200 completo.
RUN find /usr/share/nginx/html -type f -exec touch {} +

EXPOSE 80

# La imagen de nginx ya trae su propio ENTRYPOINT/CMD.
