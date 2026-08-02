# =============================================================================
# Backend PreVisar — imagen multi-stage
# =============================================================================
# Etapa 1 (build): compila el jar con Maven y un JDK completo.
# Etapa 2 (runtime): solo el jar sobre un JRE, que pesa bastante menos y no lleva
# compilador ni fuentes.
#
# Se usa la imagen oficial de Maven en vez del wrapper (./mvnw) a propósito: el
# wrapper es un script de shell y en un repo trabajado desde Windows puede quedar
# con finales de línea CRLF, que en Linux fallan con "bad interpreter".
# =============================================================================

# ----------------------------- Etapa 1: build -------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# El pom va primero y solo: si no cambió, Docker reutiliza la capa de
# dependencias y no vuelve a bajar medio Maven Central en cada build.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src/ ./src/
# Los tests ya corren en el pipeline de desarrollo; acá solo se empaqueta.
RUN mvn -B clean package -DskipTests

# ---------------------------- Etapa 2: runtime ------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl es para el healthcheck del compose. Se limpia el índice de apt para no
# dejar ~40 MB de listas dentro de la imagen.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios: si alguien escapa del proceso, no es root.
RUN useradd --system --create-home --uid 1001 previsar

# Carpeta de los archivos subidos. Es el punto de montaje del volumen del
# compose, y tiene que pertenecer al usuario que corre la app o los uploads
# fallan con permission denied.
RUN mkdir -p /data/expedientes && chown -R previsar:previsar /data/expedientes

COPY --from=build /build/target/*.jar app.jar

USER previsar
EXPOSE 8080

# MaxRAMPercentage en vez de un -Xmx fijo: la JVM se ajusta al límite de memoria
# que le ponga el contenedor, sin tener que tocar la imagen.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
