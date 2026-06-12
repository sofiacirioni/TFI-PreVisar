# PreVisar

Aplicación web que asiste a profesionales matriculados del **CIEC Córdoba** en el
armado y la **pre-validación de expedientes técnicos** antes de presentarlos en
*miCIEC*. PreVisar es una **capa previa** a miCIEC: no se integra con el sistema
oficial ni lo reemplaza, sino que ayuda a que el expediente llegue completo y
correcto.

> **Trabajo Final Integrador** — Tecnicatura Universitaria en Programación, UTN FRC.
> Estudiante: **Sofía Cirioni** (legajo 415591).

---

## Alcance del MVP

- **Subtipo de trámite:** PR-DT-RT (Proyecto, Dirección Técnica y Representación Técnica).
- **Especialidad:** Eléctrica (ELEC).

El sistema cubre el alta del profesional y su cartera (comitentes y obras), el
armado guiado del expediente mediante un asistente paso a paso, y el cálculo
referencial de aportes.

---

## Stack tecnológico

### Backend
- **Java 21** (Zulu / compatible) + **Spring Boot 3.5.14** + **Maven**
- Arquitectura en capas: `controller → service → repository → entity`, con servicios definidos en **interfaz + implementación**
- **Lombok** (boilerplate) y **MapStruct 1.6.3** (mapeo entidad ↔ DTO)
- **Spring Security + JWT** (jjwt 0.12.6), autenticación *stateless*
- **springdoc-openapi 2.7.0** (Swagger UI)
- Paquete base: `ar.edu.utn.frc.previsar`

### Frontend
- **Angular 20** + **Angular Material 20**
- Componentes *standalone*, **signals** e interceptores funcionales
- URLs de la API centralizadas en `core/constants/api.constants.ts`

### Base de datos
- **PostgreSQL 16** (Docker — contenedor `previsar-postgres`, base/usuario `previsar`)
- **Flyway** para versionado del esquema (migraciones `V001`–`V015`)

---

## Modelo de datos

Cadena de dominio:

```
usuario (1:1) profesional → comitente → obra → expediente → tipo_tarea → especialidad
```

- **`usuario`** es la cuenta de acceso (autenticación); **`profesional`** es la
  identidad de dominio de la que cuelga todo el negocio.
- El **expediente se ancla en la `obra`**, no en el comitente: tiene
  `obra_id`, `tipo_tarea_id` y `profesional_id` (este último desnormalizado a
  propósito, para el aislamiento por profesional y el listado). Al comitente se
  llega vía `obra.comitente`.
- **`parametro_aporte`** no tiene FK: es configuración (con vigencia temporal)
  que consume el cálculo de aportes.

### Migraciones Flyway

| Migración | Contenido |
|-----------|-----------|
| `V001`–`V012` | Jurisdicción (provincia/regional), usuario, condición IVA, profesional, rol revisor, comitente, obra, título y ajustes asociados. |
| `V013` | `especialidad` + `tipo_tarea` (con flags `aplica_rod`, `aplica_arancel_admin`, `aplica_caja`, `orden`). Seeds: especialidad **ELEC** + tarea **PR-DT-RT**. |
| `V014` | `parametro_aporte`: `concepto` (ROD / ARANCEL_ADMIN / CAJA_PROFESIONAL / CAJA_COMITENTE), `tipo_valor` (PORCENTAJE / FIJO), `valor`, `base_calculo` (HONORARIOS / MONTO_OBRA, nullable), vigencia y `activo`. Índice parcial único: un valor vigente por concepto. Seeds iniciales de aportes. |
| `V015` | `expediente`: casi todos los campos nullable a propósito (guardado parcial); `profesional_id` es la única FK obligatoria. Columnas: `estado`, `nombre` (editable), `honorarios_referenciales`, los 4 aportes (snapshot), `activo` (soft delete) y timestamps. |

> El diagrama de clases del dominio y los diagramas de arquitectura están en
> **[docs/diagramas.md](docs/diagramas.md)**.

---

## Estructura del repositorio

Monorepo:

```
TFI-PreVisar/
├── backend/            # Spring Boot (API REST)
├── frontend/           # Angular (SPA)
├── docs/               # Diagramas y documentación
├── docker-compose.yml  # PostgreSQL 16
└── .env.example        # Plantilla de variables de entorno
```

---

## Puesta en marcha

### Requisitos
- **JDK 21** (el backend compila con `release 21`)
- **Node.js** + **npm** (para Angular 20)
- **Docker** (para PostgreSQL)

### 1. Base de datos

```bash
docker compose up -d
```

Levanta PostgreSQL 16 en `localhost:5432` (base/usuario `previsar`).

### 2. Variables de entorno

Copiá la plantilla y completá el secreto JWT:

```bash
cp .env.example .env
```

```ini
# Generar el secreto, por ejemplo: openssl rand -base64 64
JWT_SECRET=<tu-clave-secreta>
JWT_EXPIRATION_MS=3600000
JWT_ISSUER=previsar-api
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

> El backend **no arranca sin `JWT_SECRET`** (es intencional). Exportá las
> variables en tu terminal antes de levantarlo (o configúralas en tu IDE).

### 3. Backend

```bash
cd backend
./mvnw spring-boot:run
```

API en `http://localhost:8080`. Flyway aplica las migraciones al iniciar.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

### 4. Frontend

```bash
cd frontend
npm install
npm start
```

App en `http://localhost:4200`.

---

## API — endpoints principales

| Recurso | Endpoints |
|---------|-----------|
| **Auth** (públicos) | `POST /auth/register`, `POST /auth/login` |
| **Catálogos** | `GET /api/catalogos/{provincias, regionales, condiciones-iva, titulos, especialidades, tipos-tarea}` |
| **Profesional** | `GET/PUT /api/profesional/me`, `POST /api/profesional/me/cambiar-password` |
| **Comitentes** | `GET/POST /api/comitentes`, `GET/PUT/DELETE /api/comitentes/{id}`, `GET /api/comitentes/buscar?dniCuit=` |
| **Obras** | `GET/POST /api/comitentes/{id}/obras`, `GET /api/obras`, `GET/PUT/DELETE /api/obras/{id}` |
| **Expedientes** | `GET/POST /api/expedientes`, `GET/PATCH/DELETE /api/expedientes/{id}`, `POST /api/expedientes/{id}/completar` |

El borrado de comitentes y obras es **lógico** (soft delete) para preservar la
integridad de expedientes históricos.

---

## Funcionalidades implementadas (frontend)

- **Autenticación:** login y registro de profesional.
- **Dashboard** y **perfil** del profesional (incluye cambio de contraseña).
- **Comitentes:** listado y alta/edición.
- **Obras:** listado y alta/edición.
- **Expedientes:** listado *(Mis expedientes)* y **asistente paso a paso**
  (wizard) para el armado, con guardado parcial.

---

## Estado del proyecto

Cronograma de **10 semanas** (12/05/2026 – 20/07/2026), organizado en 6 sprints.

- ✅ **Sprint 0, 1 y 2 cerrados.** Sprint 2 finalizado antes de su fecha límite.
- Gestión: **Jira** (proyecto `415591_PreVisar`, prefijo `SCRUM-`) y **Confluence**.

---

## Autoría

**Sofía Cirioni** — Tecnicatura Universitaria en Programación, UTN FRC · 2026.
Trabajo Final Integrador. Licencia: ver [LICENSE](LICENSE).
