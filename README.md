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
- **Jurisdicción sembrada:** Córdoba (el modelo ya es multi-provincia).

El sistema cubre:

1. **Alta del profesional y su cartera** (comitentes y obras).
2. **Armado guiado del expediente** — asistente paso a paso (wizard) con guardado
   parcial, y luego una pantalla de armado documental con la estructura de
   secciones y ranuras (*slots*) de documentos.
3. **Cálculo referencial de aportes**, parametrizado por concepto y tipo de tarea.
4. **Generación de PDFs** por el propio sistema (carátula y contrato de locación) y
   **compilado** del expediente completo en un único PDF.
5. **Pre-validación en 3 niveles** (formato, coherencia de datos y análisis visual
   con IA).
6. **Pago del arancel CIEC** vía Mercado Pago (Checkout Pro + webhook).
7. **Rol revisor**: configuración de la estructura documental de su provincia,
   actualización del arancel y análisis con IA de expedientes completos de terceros.

---

## Stack tecnológico

### Backend
- **Java 21** + **Spring Boot 3.5.14** + **Maven**
- Arquitectura en capas: `controller → service → repository → entity`, con la
  mayoría de los servicios definidos en **interfaz + implementación**
- **Lombok** (boilerplate) y **MapStruct 1.6.3** (mapeo entidad ↔ DTO)
- **Spring Security + JWT** (jjwt 0.12.6), autenticación *stateless*
- **springdoc-openapi 2.7.0** (Swagger UI)
- **Apache PDFBox 3.0.3** — lectura, rasterizado y compilado de PDFs
- **OpenPDF 3.0.5** — generación de los PDFs propios (carátula, contrato)
- **Google GenAI (Gemini) 1.60.0** — validación visual y resumen de expedientes
- **Mercado Pago SDK 2.9.2** — cobro del arancel (Checkout Pro)
- **Spring Mail** — notificaciones (pago acreditado, solicitud de rol revisor)
- Paquete base: `ar.edu.utn.frc.previsar`

### Frontend
- **Angular 20** + **Angular Material 20**
- Componentes *standalone*, **signals**, rutas *lazy* e interceptores funcionales
- `ngx-extended-pdf-viewer` (previsualización de documentos) y
  `ngx-mat-select-search` (búsqueda en selects)
- URLs de la API centralizadas en `core/constants/api.constants.ts`

### Base de datos y almacenamiento
- **PostgreSQL 16** (contenedor `previsar-postgres`, base/usuario `previsar`)
- **Flyway** para versionado del esquema (migraciones `V001`–`V031`)
- Los archivos subidos **no** van a la base: se guardan en el filesystem y en la
  BD queda solo la ruta relativa (`previsar.storage.base-path`)

### Infraestructura
- **Docker Compose** — el stack completo (base + API + SPA) en un comando
- Imágenes propias **multi-stage**: las herramientas de build (Maven+JDK, Node)
  no viajan a la imagen final, que lleva solo un JRE o **nginx**
- **nginx** sirve la SPA compilada y hace de *reverse proxy* hacia la API, con lo
  que frontend y backend quedan en el mismo origen
- Estado persistente en volúmenes: `postgres_data` y `previsar_storage`

---

## Modelo de datos

Cadena de dominio principal:

```
usuario (1:1) profesional → comitente → obra → expediente → tipo_tarea → especialidad
```

Y alrededor del expediente:

```
expediente → documento_cargado → documento_requerido → seccion → (tipo_tarea, provincia)
expediente → pago                     (arancel CIEC vía Mercado Pago)
documento_cargado → validacion_visual (resultado de IA, cacheado por hash)
```

- **`usuario`** es la cuenta de acceso (autenticación); **`profesional`** es la
  identidad de dominio de la que cuelga todo el negocio.
- El **expediente se ancla en la `obra`**: tiene `obra_id`, `tipo_tarea_id` y
  `profesional_id` (este último desnormalizado a propósito, para el aislamiento
  por profesional y el listado). Al comitente se llega vía `obra.comitente`; la
  provincia que define la estructura documental, vía `obra.provincia`.
- **La estructura documental es dato, no código**: `seccion` +
  `documento_requerido` se configuran por *(provincia, tipo de tarea)* y las
  administra el revisor de esa provincia.
- **Los aportes no se persisten en el expediente**: son un cálculo determinístico
  a partir de `honorarios_referenciales` + los `parametro_aporte` vigentes.
  `tipo_tarea_aporte` declara qué conceptos aplican a cada tarea.
- **El estado del arancel tampoco se persiste**: se deriva de las filas de `pago`
  (`APROBADO > PENDIENTE > NINGUNO`).
- **`revision_externa`** es el flujo del revisor: un PDF completo de un tercero
  que la IA resume. Su estado sí vive en la base, para sobrevivir un reinicio.

### Migraciones Flyway

| Migración | Contenido |
|-----------|-----------|
| `V001`–`V012` | Jurisdicción (provincia/regional), usuario, condición IVA, profesional, rol revisor, comitente, obra, título y ajustes asociados. |
| `V013` | `especialidad` + `tipo_tarea`. Seeds: especialidad **ELEC** + tarea **PR-DT-RT**. |
| `V014` | `parametro_aporte`: valores con vigencia temporal. |
| `V015` | `expediente`: casi todos los campos nullable a propósito (guardado parcial); `profesional_id` es la única FK obligatoria. |
| `V016` | `profesional.numero_orden` (complementa la matrícula). |
| `V017` | **Rediseño de aportes**: `concepto_aporte` (catálogo, grupo CIEC/CAJA) + `tipo_tarea_aporte` (qué conceptos aplican a cada tarea). Reemplaza los flags `aplica_*` de `tipo_tarea`. |
| `V018` | Se quitan las 4 columnas `aporte_*` del expediente: son cálculo, no dato. |
| `V019` | `base_calculo` queda solo en `parametro_aporte` (era redundante). |
| `V020` | `seccion` + `documento_requerido` (estructura documental) y seeds de PR-DT-RT. |
| `V021` | La estructura pasa a ser **por provincia**; unicidad de código parcial (`WHERE activo`) para poder recrear un código dado de baja. |
| `V022` | Estado del expediente: `BORRADOR → EN_PROCESO`. Se elimina `COMPLETO` (no hay estado terminal: la entrega/visado vive en otro sistema). |
| `V023` | `documento_cargado` (archivo en filesystem, ruta en BD) + `documento_requerido.permite_multiples`. |
| `V024` | `documento_requerido.generable`: el sistema produce ese PDF (carátula, contrato). |
| `V025` | `documento_requerido.valida_a4`: los planos (gran formato) quedan exentos del chequeo A4. |
| `V026` | `validacion_visual`: resultado JSONB de la IA, único por `(documento, hash de contenido)`. |
| `V027` | `pago`: registro de pagos de Mercado Pago, único por `mp_payment_id` (idempotencia). |
| `V028` | `usuario.terminos_version`: versión de T&C aceptada al registrarse. |
| `V029` | `revision_externa`: PDF completo que el revisor analiza con IA; estado + resultado JSONB. |
| `V030` | Datos editables del contrato de locación embebidos en `expediente`. |
| `V031` | Restaura el índice parcial único de `parametro_aporte`: **un solo valor vigente por concepto**. Lo creaba la `V014` sobre la columna `concepto`, y el `DROP COLUMN` de la `V017` se lo llevó. |

> El diagrama de clases del dominio, los de arquitectura y los flujos principales
> están en **[docs/diagramas.md](docs/diagramas.md)**.

---

## Estructura del repositorio

Monorepo:

```
TFI-PreVisar/
├── backend/
│   ├── Dockerfile          # build del jar + runtime sobre JRE
│   └── src/…               # Spring Boot (API REST)
├── frontend/
│   ├── Dockerfile          # build de la SPA + runtime sobre nginx
│   ├── nginx.conf          # sirve la SPA y hace de proxy a la API
│   └── src/…               # Angular (SPA)
├── docs/                   # Diagramas y documentación
├── docker-compose.yml      # stack completo: PostgreSQL + API + SPA
└── .env.example            # Plantilla de variables de entorno
```

---

## Puesta en marcha

### Opción A — Docker (recomendada)

Levanta la aplicación entera —base, API y SPA— con un comando. Lo único que
hace falta instalado es **Docker**.

```bash
cp .env.example .env      # completá JWT_SECRET (openssl rand -base64 64)
docker compose up -d --build
```

Listo: **http://localhost:4200**

| Servicio | Qué es | Dónde queda |
|----------|--------|-------------|
| `frontend` | SPA compilada, servida por nginx (y proxy hacia la API) | http://localhost:4200 |
| `backend` | API Spring Boot. Flyway migra al arrancar | http://localhost:8080 · [Swagger](http://localhost:8080/swagger-ui.html) |
| `postgres` | PostgreSQL 16 | `localhost:5432` (base/usuario `previsar`) |

El arranque está encadenado por *healthchecks*: el backend espera a que Postgres
acepte conexiones y nginx espera a que la API responda, así que no hay 502 al
entrar por primera vez.

**Comandos útiles**

```bash
docker compose logs -f backend   # seguir los logs de la API
docker compose ps                # estado y salud de cada servicio
docker compose down              # bajar todo (los datos se conservan)
docker compose down -v           # bajar y BORRAR base y archivos subidos
docker compose up -d --build     # reconstruir tras cambiar código
```

Los datos viven en dos volúmenes y sobreviven a `down`, a `--build` y a que se
recreen las imágenes: `postgres_data` (la base) y `previsar_storage` (los
documentos subidos, montados en `/data/expedientes`).

> **El frontend y la API quedan en el mismo origen.** nginx redirige `/api` y
> `/auth` al backend, que es justamente por lo que el build de producción usa
> rutas relativas. Por eso en este stack CORS no entra en juego.

### Opción B — Desarrollo local

Con recarga en caliente, para trabajar sobre el código. Requiere **JDK 21**,
**Node.js + npm** y **Docker** (solo para la base).

```bash
docker compose up -d postgres     # solo la base
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm start
```

El backend levanta con el perfil `dev` (apunta a `localhost:5432` y loguea el
SQL); el de los contenedores es el perfil `docker`. La app queda en
`http://localhost:4200` y pega directo al `:8080`.

### Variables de entorno

```bash
cp .env.example .env
```

**Docker y el desarrollo local usan el mismo `.env`**: no hay dos
configuraciones. Con las credenciales cargadas, todas las funciones andan igual
en los dos modos.

Lo único **obligatorio** para arrancar es `JWT_SECRET`. Las credenciales de
Gemini, Mercado Pago y correo son necesarias para *usar* esas funciones, pero su
ausencia **no impide que la aplicación levante**: en vez de morir al arrancar,
falla solo la función puntual cuando se la invoca. Eso permite trabajar sin
conexión o sin cuota, no significa que esas features estén deshabilitadas.

| Variable | Obligatoria | Para qué |
|----------|-------------|----------|
| `JWT_SECRET` | **Sí** | Firma de los tokens. Sin ella la app **no arranca** (intencional). |
| `JWT_EXPIRATION_MS`, `JWT_ISSUER` | No | Duración y emisor del token. |
| `FRONTEND_PORT`, `BACKEND_PORT`, `POSTGRES_PORT` | No | Puertos publicados en el host (`4200`, `8080`, `5432`). Cambialos si alguno está ocupado. |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | No | Credenciales de la base. |
| `LOG_LEVEL_APP` | No | `INFO` (default) o `DEBUG` para la app. |
| `CORS_ALLOWED_ORIGINS` | No | Orígenes permitidos. No aplica en el stack de compose (mismo origen). |
| `PREVISAR_STORAGE_PATH` | No | Carpeta base de los archivos subidos. En Docker la fija el compose en `/data/expedientes`. |
| `GOOGLE_API_KEY` | Para la IA | La lee el SDK de Gemini directamente del entorno. |
| `MP_ACCESS_TOKEN` | Para el pago | Credencial de prueba de Mercado Pago (`TEST-…`). |
| `MP_BACK_URL_BASE` | No | A dónde MP redirige el **navegador** → **frontend** (`:4200`). |
| `MP_WEBHOOK_URL_BASE` | Para el pago | A dónde MP hace la llamada **server-to-server** → **backend** público (`ngrok http 8080`). Si queda vacía, el pago nunca se confirma. |
| `MP_WEBHOOK_SECRET` | Recomendada | Valida la firma del webhook. Vacía ⇒ se procesa igual con un `WARN`. |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Para correos | Gmail con contraseña de aplicación. |
| `MAIL_FROM`, `MAIL_COLEGIO`, `MAIL_HABILITADO` | No | Remitente, casilla de la institución y *kill-switch* de envíos. |

> **`MP_BACK_URL_BASE` y `MP_WEBHOOK_URL_BASE` no son la misma URL**: la primera
> apunta al frontend y la segunda al backend público. Compartirlas hace que la
> notificación de pago se pierda en silencio.

---

## API — endpoints principales

| Recurso | Endpoints |
|---------|-----------|
| **Auth** (públicos) | `POST /auth/register`, `POST /auth/login` |
| **Catálogos** (públicos) | `GET /api/catalogos/{provincias, regionales, condiciones-iva, titulos, especialidades, tipos-tarea}` |
| **Profesional** | `GET/PUT /api/profesional/me`, `POST /api/profesional/me/cambiar-password`, `POST /api/profesional/me/baja`, `POST /api/profesional/me/solicitar-rol-revisor` |
| **Comitentes** | `GET/POST /api/comitentes`, `GET/PUT/DELETE /api/comitentes/{id}`, `GET /api/comitentes/buscar?dniCuit=`, `GET /api/comitentes/buscar-incremental?q=` |
| **Obras** | `GET/POST /api/comitentes/{id}/obras`, `GET /api/obras`, `GET/PUT/DELETE /api/obras/{id}` |
| **Expedientes** | `GET/POST /api/expedientes`, `GET/PATCH/DELETE /api/expedientes/{id}`, `POST /api/expedientes/{id}/completar`, `POST /api/expedientes/calcular-aportes` |
| **PDFs del expediente** | `GET /api/expedientes/{id}/caratula`, `GET /api/expedientes/{id}/contrato`, `PUT /api/expedientes/{id}/contrato/datos` |
| **Documentos** | `POST/GET /api/expedientes/{id}/documentos`, `GET/DELETE /api/expedientes/{id}/documentos/{docId}`, `GET …/compilado` |
| **Validación** | `GET /api/expedientes/{id}/documentos/validacion` (niveles 1+2+3 persistido), `POST …/validacion/ia?documentoRequeridoId=` (dispara IA, 202), `GET …/validacion/ia/estado?documentoRequeridoId=` |
| **Pago (arancel)** | `POST /api/expedientes/{id}/pago`, `POST /api/expedientes/{id}/pago/sync`, `POST /api/pagos/webhook` (público, lo llama MP) |
| **Estructura documental** | `GET /api/estructura?tipoTareaId=&provinciaId=`, `GET /api/estructura/expediente/{id}`, `GET /api/estructura/mia?tipoTareaId=`; CRUD de `secciones` y `documentos` + `POST /api/estructura/clonar` (solo revisor) |
| **Aportes (gestión)** | `GET /api/aportes/arancel`, `PUT /api/aportes/arancel` (solo revisor) |
| **Revisor** | `POST/GET /api/revisor/revisiones`, `GET /api/revisor/revisiones/metricas`, `GET/DELETE /api/revisor/revisiones/{id}` |

**Convenciones de la API**

- El borrado de comitentes, obras, expedientes y documentos es **lógico**
  (soft delete), para preservar la integridad de los expedientes históricos.
- Ante un recurso ajeno se devuelve **404, no 403**: no se filtra la existencia
  de datos de otro profesional.
- Los permisos de revisor se validan **en el service** (no hay *method security*):
  cada operación exige `RolRevisor` y, cuando aplica, que la provincia coincida.
- Los errores usan un formato único (`ErrorResponseDto`) producido por
  `GlobalExceptionHandler`.

---

## Pre-validación en 3 niveles

La validación **no se persiste como veredicto**: se calcula on-demand y devuelve
observaciones con `nivel` (INFO / ADVERTENCIA) y `origen`.

| Nivel | Qué revisa | Costo |
|-------|-----------|-------|
| **1 — Formato** (`ValidacionNivel1Service`) | Páginas A4 (salvo ranuras marcadas `valida_a4 = false`), peso total < 32 MB (límite de miCIEC), archivos duplicados por hash, PDFs ilegibles. | Barato, siempre corre. |
| **2 — Coherencia** (`ValidacionNivel2Service`) | Extrae texto del contrato y la planilla y lo cruza con el expediente: CUIT del comitente presente, honorarios dentro de ±5 % del referencial. | Barato, siempre corre. |
| **3 — Visual con IA** (`ValidacionNivel3Service`) | Rasteriza el PDF y lo manda a Gemini para detectar problemas de presentación (firmas faltantes, sellos, legibilidad). | **Caro**: lo dispara el profesional por ranura, corre `@Async` y el resultado se persiste en `validacion_visual` cacheado por hash de contenido. |

Los tres implementan `ValidadorExpediente`; `ValidacionServiceImpl` los descubre
por inyección de lista y fusiona sus resultados. En la lectura general, el nivel 3
solo **lee** lo ya persistido, nunca llama a Gemini.

---

## Funcionalidades implementadas (frontend)

- **Autenticación:** login y registro de profesional (con aceptación de T&C).
- **Dashboard** diferenciado para profesional y revisor.
- **Perfil:** edición, cambio de contraseña, solicitud de rol revisor y baja de cuenta.
- **Comitentes:** listado, detalle y alta/edición, con búsqueda incremental por DNI/CUIT.
- **Obras:** listado y alta/edición.
- **Expedientes:**
  - *Mis expedientes* (listado con estado y estado del arancel).
  - **Wizard** de armado inicial, con guardado parcial.
  - **Pantalla de armado documental**: subida por ranura, previsualización,
    generación de carátula/contrato, panel de validación, análisis con IA y
    descarga del compilado.
- **Pago del arancel:** diálogo de pago, link compartible y pantalla pública de
  retorno (`/pago/:estado`).
- **Revisor:** análisis de expedientes completos con IA (listado, detalle y métricas),
  configuración de la estructura documental y actualización del arancel.
- **Legales y ayuda:** Términos y Condiciones y Política de Privacidad (públicas),
  FAQ (privada).

---

## Testing

Tests unitarios con JUnit 5 + Mockito en `backend/src/test`:

```bash
cd backend
./mvnw test
```

El testing automatizado se acotó al **backend**; el frontend se valida
manualmente.

**Qué está cubierto** — la capa de negocio, seguridad y utilidades:

| Área | Clases con tests |
|------|------------------|
| Cartera y perfil | `ComitenteServiceImpl`, `ObraServiceImpl`, `ProfesionalServiceImpl`, `AuthServiceImpl` |
| Expediente | `ExpedienteServiceImpl`, `DocumentoCargadoServiceImpl`, `EstructuraServiceImpl`, `CatalogoServiceImpl` |
| Aportes y pagos | `AportesCalculatorServiceImpl`, `ParametroAporteServiceImpl`, `PagoServiceImpl` |
| Pre-validación | `ValidacionServiceImpl`, `ValidacionNivel1Service`, `ValidacionNivel2Service`, `AnalisisEstadoTracker` |
| Revisor | `RevisionExternaServiceImpl` |
| Seguridad y soporte | `JwtService`, `SecurityUtils`, `CustomUserDetailsService`, `GlobalExceptionHandler`, `FileStorageService`, `CuitValidator`, `HashUtil` |

**Qué queda fuera a propósito**: las clases cuyo trabajo es hablar con algo
externo, donde un test unitario solo verificaría el mock —
`GeminiVisionClient`, `PagoMpService`, `EmailServiceImpl`,
`CompilacionServiceImpl`, `PdfGenerationServiceImpl`, `PdfRasterizerService`,
`ValidacionNivel3Service`, los prompts (constantes) y el filtro/entry point de
JWT (cadena de servlets). Cubrirlas requeriría tests de integración, fuera del
alcance planificado.

`PrevisarApplicationTests.contextLoads` levanta el contexto completo, así que
necesita PostgreSQL arriba y las variables de entorno cargadas. Para correr solo
los tests unitarios:

```bash
./mvnw test -Dtest='!PrevisarApplicationTests'
```

---

## Estado del proyecto

Desarrollo iniciado el **19/05/2026**; última actividad registrada en el
repositorio: **01/08/2026**. Gestión en **Jira** (proyecto `415591_PreVisar`,
prefijo `SCRUM-`) y **Confluence**; los commits referencian los tickets cerrados.

Entregado a la fecha: cartera del profesional, armado guiado y documental del
expediente, cálculo de aportes, generación y compilado de PDFs, pre-validación en
3 niveles, cobro del arancel por Mercado Pago, notificaciones por correo y las
funciones del rol revisor.

---

## Autoría

**Sofía Cirioni** — Tecnicatura Universitaria en Programación, UTN FRC · 2026.
Trabajo Final Integrador. Licencia: ver [LICENSE](LICENSE).
