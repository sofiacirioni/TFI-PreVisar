# Diagramas — PreVisar

Documentación visual del sistema. Refleja el estado actual del código en
`backend/src/main/java/ar/edu/utn/frc/previsar/` y `frontend/src/app/`.

> Los diagramas usan **Mermaid**. Se previsualizan en GitHub y en VS Code
> (vista previa de Markdown `Ctrl+Shift+V` + extensión _Markdown Preview Mermaid Support_).
> El esquema de base de datos (sección 7) se ve directamente en **dbdiagram.io**.

Contenido:

1. [Diagrama de clases (modelo de dominio)](#1-diagrama-de-clases--modelo-de-dominio)
2. [Arquitectura por capas](#2-arquitectura-por-capas)
3. [Mapa de componentes (cableado real)](#3-mapa-de-componentes--cableado-real)
4. [Flujos principales](#4-flujos-principales)
5. [Arquitectura del frontend](#5-arquitectura-del-frontend)
6. [Despliegue (Docker Compose)](#6-despliegue--docker-compose)
7. [Esquema de base de datos (DBML)](#7-esquema-de-base-de-datos-dbml)

---

## 1. Diagrama de clases — modelo de dominio

Entidades JPA y enums persistentes. Se divide en dos vistas para que sea legible:
el **núcleo** (cuenta, cartera y expediente) y los **satélites** del expediente
(estructura documental, validación, pagos y revisión externa).

### 1.1 Núcleo: cuenta, cartera y expediente

```mermaid
classDiagram
    direction LR

    %% ===================== Autenticación y profesional =====================
    class Usuario {
        +Long id
        +String email
        +String passwordHash
        +Rol rol
        +Boolean activo
        +String terminosVersion
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Profesional {
        +Long id
        +String nombre
        +String apellido
        +String dni
        +String cuit
        +String matricula
        +String numeroOrden
        +String tituloOtroDescripcion
        +String domicilio
        +String telefono
        +Boolean afiliadoCaja8470
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +String tituloDescripcion()
    }

    class RolRevisor {
        +Long id
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    %% ===================== Cartera del profesional =====================
    class Comitente {
        +Long id
        +TipoPersona tipoPersona
        +String nombreRazonSocial
        +String dniCuit
        +String domicilio
        +String email
        +String telefono
        +LocalDateTime deletedAt
        +boolean isDeleted()
        +void softDelete()
    }

    class Obra {
        +Long id
        +String designacion
        +String calle
        +String numero
        +String barrio
        +String localidad
        +String codigoPostal
        +String circunscripcion
        +String seccion
        +String manzana
        +String parcela
        +LocalDateTime deletedAt
        +String getNomenclaturaCatastral()
        +boolean isDeleted()
        +void softDelete()
    }

    class Expediente {
        +Long id
        +String nombre
        +EstadoExpediente estado
        +BigDecimal honorariosReferenciales
        +boolean activo
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +DatosContrato getDatosContrato()
    }

    class DatosContrato {
        <<embeddable>>
        +BigDecimal honorariosPactados
        +String documentacionConfeccion
        +String tareasEspeciales
        +String formaPago
        +String plazoEntrega
        +String gastosEspeciales
    }

    %% ===================== Catálogos =====================
    class Especialidad {
        +Long id
        +String codigo
        +String nombre
        +boolean activo
    }

    class TipoTarea {
        +Long id
        +String codigo
        +String nombre
        +int orden
        +boolean activo
    }

    class Titulo {
        +Long id
        +String nombre
        +Boolean permiteTextoLibre
        +Boolean activo
    }

    class CondicionIva {
        +Long id
        +String codigo
        +String descripcion
        +boolean activo
    }

    class Provincia {
        +Long id
        +String nombre
        +String codigo
    }

    class Regional {
        +Long id
        +String nombre
    }

    %% ===================== Aportes (parametrizados) =====================
    class ConceptoAporte {
        +Long id
        +String codigo
        +String nombre
        +GrupoAporte grupo
        +boolean activo
    }

    class TipoTareaAporte {
        +Long id
        +boolean activo
    }

    class ParametroAporte {
        +Long id
        +TipoValor tipoValor
        +BigDecimal valor
        +BaseCalculo baseCalculo
        +LocalDate vigenciaDesde
        +LocalDate vigenciaHasta
        +boolean activo
    }

    %% ===================== Enums =====================
    class Rol {
        <<enumeration>>
        PROFESIONAL
        ADMIN
    }
    class TipoPersona {
        <<enumeration>>
        FISICA
        JURIDICA
    }
    class EstadoExpediente {
        <<enumeration>>
        BORRADOR
        EN_PROCESO
    }
    class GrupoAporte {
        <<enumeration>>
        CIEC
        CAJA
    }
    class TipoValor {
        <<enumeration>>
        PORCENTAJE
        FIJO
    }
    class BaseCalculo {
        <<enumeration>>
        HONORARIOS
        MONTO_OBRA
    }

    %% ===================== Relaciones =====================
    Profesional "1" *-- "1" Usuario : autentica
    RolRevisor "1" --> "1" Profesional : habilita
    RolRevisor "*" --> "1" Provincia
    Profesional "*" --> "1" Titulo
    Profesional "*" --> "1" Regional
    Profesional "*" --> "1" CondicionIva
    Regional "*" --> "1" Provincia

    Comitente "*" --> "1" Profesional
    Obra "*" --> "1" Comitente
    Obra "*" --> "1" Provincia

    Expediente "*" --> "1" Profesional
    Expediente "*" --> "0..1" Obra
    Expediente "*" --> "0..1" TipoTarea
    Expediente "1" *-- "1" DatosContrato : embebido
    TipoTarea "*" --> "1" Especialidad

    TipoTareaAporte "*" --> "1" TipoTarea
    TipoTareaAporte "*" --> "1" ConceptoAporte
    ParametroAporte "*" --> "1" ConceptoAporte

    %% ===================== Dependencias con enums =====================
    Usuario ..> Rol
    Comitente ..> TipoPersona
    Expediente ..> EstadoExpediente
    ConceptoAporte ..> GrupoAporte
    ParametroAporte ..> TipoValor
    ParametroAporte ..> BaseCalculo
```

### 1.2 Satélites del expediente: estructura, documentos, validación y pagos

```mermaid
classDiagram
    direction LR

    class Expediente {
        +Long id
        +EstadoExpediente estado
    }
    class TipoTarea
    class Provincia
    class Usuario

    %% ===================== Estructura documental (configurable) =====================
    class Seccion {
        +Long id
        +String codigo
        +String nombre
        +Integer orden
        +boolean activo
    }

    class DocumentoRequerido {
        +Long id
        +String codigo
        +String nombre
        +boolean obligatorio
        +Integer orden
        +boolean permiteMultiples
        +boolean generable
        +boolean validaA4
        +boolean activo
    }

    %% ===================== Archivos y validación =====================
    class DocumentoCargado {
        +Long id
        +String nombreOriginal
        +String rutaRelativa
        +String tipoMime
        +Long tamanoBytes
        +boolean activo
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class ValidacionVisual {
        +Long id
        +String hashDocumento
        +String resultado
        +Instant createdAt
    }

    %% ===================== Pago del arancel =====================
    class Pago {
        +Long id
        +String mpPaymentId
        +String mpPreferenceId
        +EstadoPago estado
        +BigDecimal monto
        +Instant createdAt
        +Instant updatedAt
    }

    %% ===================== Revisión externa (rol revisor) =====================
    class RevisionExterna {
        +Long id
        +String nombreArchivo
        +String rutaRelativa
        +EstadoRevision estado
        +String resultado
        +String detalle
        +Instant createdAt
    }

    %% ===================== Enums =====================
    class EstadoPago {
        <<enumeration>>
        APROBADO
        PENDIENTE
        RECHAZADO
    }
    class EstadoArancel {
        <<enumeration>>
        NINGUNO
        PENDIENTE
        APROBADO
    }
    class EstadoRevision {
        <<enumeration>>
        EN_PROGRESO
        COMPLETADO
        ERROR
    }
    class NivelObservacion {
        <<enumeration>>
        INFO
        ADVERTENCIA
    }
    class OrigenObservacion {
        <<enumeration>>
        DETERMINISTICO
        COHERENCIA
        IA_VISUAL
    }

    %% ===================== Relaciones =====================
    Seccion "*" --> "1" TipoTarea
    Seccion "*" --> "1" Provincia
    DocumentoRequerido "*" --> "1" Seccion

    DocumentoCargado "*" --> "1" Expediente
    DocumentoCargado "*" --> "1" DocumentoRequerido
    ValidacionVisual "*" --> "1" DocumentoCargado

    Pago "*" --> "1" Expediente
    RevisionExterna "*" --> "1" Usuario

    Pago ..> EstadoPago
    Expediente ..> EstadoArancel : derivado de sus Pago
    RevisionExterna ..> EstadoRevision
```

### Notas del modelo

- **Usuario ⇄ Profesional**: composición. Un `Profesional` _tiene_ un `Usuario`
  que lo autentica (`@OneToOne`). Los datos de login (email, hash, rol, versión de
  T&C aceptada) viven en `Usuario`; los datos personales/profesionales en
  `Profesional`.
- **RolRevisor**: rol secundario opcional. Si un `Profesional` tiene un
  `RolRevisor`, queda habilitado a administrar la estructura documental y el
  arancel de la `Provincia` indicada, y a usar el análisis de expedientes
  externos. _Revisor no es un valor del enum `Rol`._
- **Soft delete**: `Comitente` y `Obra` usan `deletedAt`; `Expediente` y
  `DocumentoCargado` usan un booleano `activo`. En ambos casos se preserva la
  integridad de los expedientes históricos.
- **Expediente**: en `BORRADOR` puede no tener `obra` ni `tipoTarea` todavía
  (ambas relaciones son nullable). `DatosContrato` es un `@Embeddable`: sus
  columnas viven en la tabla `expediente`, y el getter nunca devuelve `null`.
- **Aportes**: `ConceptoAporte` es el catálogo (grupo CIEC o CAJA),
  `TipoTareaAporte` declara qué conceptos aplican a cada tarea y
  `ParametroAporte` guarda el valor vigente de cada concepto. El resultado **no se
  persiste** en el expediente: se calcula on-demand.
- **EstadoArancel** no es una columna: se deriva de las filas de `Pago` con la
  prioridad `APROBADO > PENDIENTE > NINGUNO`.
- **Estructura documental**: `Seccion` + `DocumentoRequerido` se configuran por
  *(provincia, tipo de tarea)*. Los flags `generable`, `permiteMultiples` y
  `validaA4` son **dato**, no `if` en el código.
- **ValidacionVisual**: cachea el resultado de la IA por
  `(documento_cargado, hash de contenido)`. Si el archivo cambia, cambia el hash y
  se recalcula.
- **RevisionExterna** cuelga de `Usuario` (no de `Expediente`): es un PDF de un
  tercero que el revisor sube, ajeno a la cartera propia. Su estado vive en la
  base para sobrevivir un reinicio.

### Leyenda

| Notación               | Significado                                          |
| ---------------------- | ---------------------------------------------------- |
| `A *-- B`              | Composición (A contiene a B)                         |
| `A --> B`              | Asociación / referencia (`@ManyToOne` / `@OneToOne`) |
| `A ..> B`              | Dependencia (uso de un `enum`)                       |
| `"1"`, `"*"`, `"0..1"` | Multiplicidad de la relación                         |

---

## 2. Arquitectura por capas

El backend sigue una arquitectura clásica en capas: `Controller → Service → Repository`,
con DTOs en los bordes, mappers para la conversión entidad↔DTO, una capa de
seguridad transversal (JWT), integraciones con servicios externos y manejo
centralizado de excepciones.

```mermaid
flowchart TB
    Client["Cliente · Frontend Angular"]

    subgraph SEC["Seguridad (transversal)"]
        direction TB
        Filter["JwtAuthenticationFilter"]
        JwtSvc["JwtService"]
        UDS["CustomUserDetailsService"]
        Entry["JwtAuthenticationEntryPoint · 401"]
        Utils["SecurityUtils · usuario/profesional actual"]
    end

    subgraph WEB["Capa Web · @RestController"]
        Ctrl["Controllers"]
    end

    subgraph APP["Capa de Negocio · @Service"]
        Svc["Services + Impl"]
        Map["Mappers (entidad ↔ DTO)"]
        Val["Validadores nivel 1 / 2 / 3"]
        Pdf["PDF: templates + compilación"]
    end

    subgraph DATA["Capa de Acceso a Datos"]
        Repo["Repositories · Spring Data JPA"]
        Store["FileStorageService · filesystem"]
    end

    DB[("PostgreSQL")]
    FS[("Filesystem · previsar.storage.base-path")]

    subgraph EXT["Servicios externos"]
        direction TB
        Gem["Gemini · google-genai"]
        MP["Mercado Pago · SDK"]
        Mail["SMTP · Spring Mail"]
    end

    subgraph CROSS["Transversal"]
        direction TB
        DTO["DTOs request / response"]
        Exc["GlobalExceptionHandler → ErrorResponseDto"]
        Cfg["SecurityConfig · CorsProperties · JwtProperties<br/>GeminiProperties · MercadoPagoProperties<br/>StorageProperties · PrevisarMailProperties"]
    end

    Client -->|HTTP + Bearer JWT| Filter
    Filter --> JwtSvc
    Filter --> UDS
    Filter -. token inválido .-> Entry
    Filter -->|autenticado| Ctrl
    Ctrl --> Svc
    Svc --> Utils
    Svc --> Map
    Svc --> Val
    Svc --> Pdf
    Svc --> Repo
    Svc --> Store
    Repo --> DB
    Store --> FS
    Val --> Gem
    Svc --> MP
    Svc --> Mail

    Ctrl -. valida .-> DTO
    Svc -. lanza .-> Exc
    Exc -. responde .-> Client
    Cfg -. configura .-> Filter
    Cfg -. configura .-> EXT
```

### Responsabilidades por capa

| Capa            | Componentes                                                                                                                                                          | Rol                                                                                                                          |
| --------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| **Web**         | `AuthController`, `CatalogoController`, `ComitenteController`, `ObraController`, `ExpedienteController`, `DocumentoController`, `EstructuraController`, `ParametroAporteController`, `PagoController`, `RevisorController`, `ProfesionalController` | Exponen los endpoints REST, reciben/validan DTOs request y devuelven DTOs response.                                          |
| **Negocio**     | `*Service` (interfaz) + `*ServiceImpl` (`@Service`, `@Transactional`)                                                                                                | Lógica de negocio, validaciones y orquestación.                                                                              |
| **Validación**  | `ValidadorExpediente` ← `ValidacionNivel1Service`, `ValidacionNivel2Service`, `ValidacionNivel3Service`; orquesta `ValidacionServiceImpl`                             | Pre-validación por niveles. `ValidacionServiceImpl` inyecta la **lista** de validadores y fusiona sus resultados.             |
| **PDF**         | `PdfTemplate` ← `CaratulaTemplate`, `ContratoTemplate`; `PdfGenerationServiceImpl`, `CompilacionServiceImpl`, `PdfRasterizerService`, `PdfResponseFactory`, `NumeroALetras` | Generación de los PDFs propios, compilado del expediente y rasterizado para la IA.                                           |
| **Mappers**     | `ComitenteMapper`, `DocumentoCargadoMapper`, `EspecialidadMapper`, `EstructuraMapper`, `ExpedienteMapper`, `ObraMapper`, `ProfesionalMapper`, `TipoTareaMapper`      | Conversión entidad ↔ DTO (MapStruct, `componentModel = "spring"`).                                                            |
| **Datos**       | `*Repository` (Spring Data JPA), `FileStorageService`                                                                                                                | Persistencia sobre PostgreSQL; los archivos van al filesystem con validación anti *path traversal*.                          |
| **Integración** | `GeminiVisionClient`, `PagoMpService`, `EmailServiceImpl`                                                                                                            | Clientes de servicios externos, con timeouts, reintentos con backoff y degradación controlada.                               |
| **Seguridad**   | `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `JwtAuthenticationEntryPoint`, `CustomUserDetailsService`, `SecurityUtils`                                 | Autenticación stateless con JWT en cada request. La autorización de negocio (dueño del recurso, rol revisor) vive en los services. |
| **Transversal** | `GlobalExceptionHandler` + jerarquía de excepciones, DTOs, `CuitValidator`, `HashUtil`, `*Properties`                                                                 | Manejo de errores, validación y configuración externalizada (12-factor).                                                     |

### Jerarquía de excepciones → HTTP

| Excepción | HTTP | Cuándo |
|-----------|------|--------|
| `MethodArgumentNotValidException` | 400 | Falla `@Valid` en un DTO. |
| `BusinessException` | 400 | Regla de negocio violada. |
| `ForbiddenException` | 403 | Autenticado pero sin permiso (ej. no es revisor de esa provincia). |
| `ResourceNotFoundException` | 404 | Recurso inexistente **o ajeno**. |
| `BadCredentialsException` | 401 | Login incorrecto. |
| `DataIntegrityViolationException` | 409 | Choca con un `UNIQUE`/`CHECK`/FK de la base. |
| `GeminiException`, `PagoException` | 502 | Falla de un servicio externo. |
| `PdfGenerationException`, `StorageException` | 500 | Falla al generar un PDF o al leer/escribir un archivo. |
| `Exception` (catch-all) | 500 | Bug: se loguea el stacktrace. |

---

## 3. Mapa de componentes — cableado real

Dependencias inyectadas tal como están en el código. Los mappers se omiten del
grafo por claridad (cada service usa el suyo; se listan en la tabla de arriba).

```mermaid
flowchart LR
    subgraph C["Controllers"]
        direction TB
        AuthC["AuthController"]
        CatC["CatalogoController"]
        ComC["ComitenteController"]
        ObrC["ObraController"]
        ExpC["ExpedienteController"]
        DocC["DocumentoController"]
        EstC["EstructuraController"]
        ParC["ParametroAporteController"]
        PagC["PagoController"]
        RevC["RevisorController"]
        ProfC["ProfesionalController"]
    end

    subgraph S["Services"]
        direction TB
        AuthS["AuthService"]
        CatS["CatalogoService"]
        ComS["ComitenteService"]
        ObrS["ObraService"]
        ExpS["ExpedienteService"]
        DocS["DocumentoCargadoService"]
        EstS["EstructuraService"]
        ParS["ParametroAporteService"]
        PagS["PagoService"]
        RevS["RevisionExternaService"]
        ProfS["ProfesionalService"]
        AporteS["AporteCalculatorService"]
        ValS["ValidacionService"]
        CompS["CompilacionService"]
        PdfS["PdfGenerationService"]
        MailS["EmailService"]
        JwtS["JwtService"]
    end

    subgraph H["Colaboradores internos"]
        direction TB
        N1["ValidacionNivel1Service"]
        N2["ValidacionNivel2Service"]
        N3["ValidacionNivel3Service · @Async"]
        Track["AnalisisEstadoTracker"]
        Gem["GeminiVisionClient"]
        Rast["PdfRasterizerService"]
        Store["FileStorageService"]
        MpS["PagoMpService"]
    end

    subgraph R["Repositories"]
        direction TB
        UsuarioR["UsuarioRepository"]
        ProfR["ProfesionalRepository"]
        RolRevR["RolRevisorRepository"]
        ComR["ComitenteRepository"]
        ObrR["ObraRepository"]
        ExpR["ExpedienteRepository"]
        SecR["SeccionRepository"]
        DocReqR["DocumentoRequeridoRepository"]
        DocCarR["DocumentoCargadoRepository"]
        ValVisR["ValidacionVisualRepository"]
        PagoR["PagoRepository"]
        RevExtR["RevisionExternaRepository"]
        TipoTR["TipoTareaRepository"]
        TTAR["TipoTareaAporteRepository"]
        ConcR["ConceptoAporteRepository"]
        ParamR["ParametroAporteRepository"]
        EspR["EspecialidadRepository"]
        ProvR["ProvinciaRepository"]
        RegR["RegionalRepository"]
        CondR["CondicionIvaRepository"]
        TitR["TituloRepository"]
    end

    %% Controllers -> Services
    AuthC --> AuthS
    CatC --> CatS
    ComC --> ComS
    ObrC --> ObrS
    ExpC --> ExpS
    ExpC --> MpS
    ExpC --> PagS
    DocC --> DocS
    DocC --> ValS
    DocC --> CompS
    DocC --> ExpS
    DocC --> N3
    DocC --> Track
    EstC --> EstS
    ParC --> ParS
    PagC --> PagS
    RevC --> RevS
    ProfC --> ProfS

    %% AuthService
    AuthS --> UsuarioR
    AuthS --> ProfR
    AuthS --> RegR
    AuthS --> CondR
    AuthS --> TitR
    AuthS --> JwtS

    %% CatalogoService
    CatS --> ProvR
    CatS --> RegR
    CatS --> CondR
    CatS --> TitR
    CatS --> TipoTR
    CatS --> EspR

    %% Cartera
    ComS --> ComR
    ObrS --> ObrR
    ObrS --> ProvR
    ObrS --> ComR

    %% ExpedienteService
    ExpS --> ExpR
    ExpS --> TipoTR
    ExpS --> ObrR
    ExpS --> PagoR
    ExpS --> AporteS
    ExpS --> PdfS

    %% Documentos
    DocS --> ExpR
    DocS --> DocReqR
    DocS --> DocCarR
    DocS --> Store

    %% Estructura
    EstS --> SecR
    EstS --> DocReqR
    EstS --> TipoTR
    EstS --> RolRevR
    EstS --> ExpS

    %% Validación
    ValS --> N1
    ValS --> N2
    ValS --> N3
    N1 --> ExpS
    N1 --> DocCarR
    N1 --> Store
    N2 --> ExpS
    N2 --> DocCarR
    N2 --> Store
    N3 --> DocCarR
    N3 --> ValVisR
    N3 --> Store
    N3 --> Rast
    N3 --> Gem
    N3 --> Track

    %% Compilación
    CompS --> EstS
    CompS --> DocCarR
    CompS --> Store
    CompS --> ExpS

    %% Aportes
    AporteS --> TTAR
    AporteS --> ParamR
    ParS --> ParamR
    ParS --> ConcR
    ParS --> RolRevR

    %% Pagos
    MpS --> ExpS
    PagS --> PagoR
    PagS --> ExpR
    PagS --> ExpS
    PagS --> MailS

    %% Revisión externa
    RevS --> RevExtR
    RevS --> RolRevR
    RevS --> Store
    RevS --> Gem

    %% Profesional
    ProfS --> ProfR
    ProfS --> UsuarioR
    ProfS --> RegR
    ProfS --> CondR
    ProfS --> TitR
    ProfS --> RolRevR
    ProfS --> MailS
```

---

## 4. Flujos principales

### 4.1 Ciclo de vida del expediente

```mermaid
stateDiagram-v2
    [*] --> BORRADOR : POST /api/expedientes
    BORRADOR --> BORRADOR : PATCH (guardado parcial del wizard)
    BORRADOR --> EN_PROCESO : POST /{id}/completar<br/>(exige obra + tipo de tarea + honorarios)
    EN_PROCESO --> EN_PROCESO : armado documental<br/>(subir, validar, generar, pagar, compilar)
    BORRADOR --> [*] : DELETE (soft delete: activo = false)
    EN_PROCESO --> [*] : DELETE (soft delete: activo = false)

    note right of EN_PROCESO
        No hay estado terminal:
        la entrega y el visado formal
        ocurren en miCIEC, fuera de
        esta aplicación.
    end note
```

### 4.2 Pre-validación en 3 niveles

```mermaid
flowchart TB
    subgraph OnDemand["GET /documentos/validacion · barato, siempre"]
        direction TB
        VS["ValidacionServiceImpl<br/>inyecta List&lt;ValidadorExpediente&gt;"]
        N1["Nivel 1 · Formato<br/>A4 · 32 MB · duplicados · PDF ilegible"]
        N2["Nivel 2 · Coherencia<br/>CUIT en contrato · honorarios ±5%"]
        N3L["Nivel 3 · lectura<br/>lee validacion_visual persistida"]
        VS --> N1
        VS --> N2
        VS --> N3L
        N1 --> Merge["Fusiona por documento<br/>ValidacionResultadoDto"]
        N2 --> Merge
        N3L --> Merge
    end

    subgraph Async["POST /documentos/validacion/ia?documentoRequeridoId= · caro, a pedido"]
        direction TB
        Gate["AnalisisEstadoTracker.iniciarSiLibre()<br/>gate atómico por ranura → 409 si ya corre"]
        Job["ValidacionNivel3Service.analizarAsync() · @Async"]
        Rast["PdfRasterizerService<br/>1 PNG por página @110 DPI"]
        Gem["GeminiVisionClient<br/>timeout + 3 reintentos con backoff"]
        Persist["ValidacionVisual<br/>único por (documento, hash)"]
        Gate --> Job --> Rast --> Gem --> Persist
    end

    Persist -. la próxima lectura ya lo ve .-> N3L
    Poll["GET …/validacion/ia/estado"] -.consulta.-> Gate
```

Observaciones: cada una lleva `codigo`, `NivelObservacion` (INFO / ADVERTENCIA) y
`OrigenObservacion` (DETERMINISTICO / COHERENCIA / IA_VISUAL). **Nada es
bloqueante**: PreVisar informa, no rechaza.

### 4.3 Pago del arancel (Mercado Pago)

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant API as ExpedienteController
    participant MPS as PagoMpService
    participant MP as Mercado Pago
    participant PG as Pagante (profesional o comitente)
    participant WH as PagoController /api/pagos/webhook
    participant PS as PagoServiceImpl
    participant Mail as EmailService

    F->>API: POST /api/expedientes/{id}/pago
    API->>MPS: crearPreferenciaArancel(id)
    MPS->>MPS: total del grupo CIEC (ROD + arancel admin)
    MPS->>MP: crear preferencia<br/>externalReference = expedienteId<br/>notificationUrl = webhookUrlBase
    MP-->>MPS: preferenceId + initPoint
    MPS-->>F: PreferenciaPagoDto (link compartible)

    F->>PG: comparte el link
    PG->>MP: paga en Checkout Pro
    MP-->>PG: redirige a backUrlBase/pago/{exito|pendiente|error}

    par Notificación server-to-server
        MP->>WH: POST webhook (type=payment, data.id)
        WH->>PS: procesarNotificacion(...)
        PS->>PS: valida firma HMAC (x-signature)
        PS->>MP: GET payment (fuente de verdad)
        PS->>PS: upsert por mp_payment_id (idempotente)
        PS->>Mail: notificarPagoAcreditado (solo al pasar a APROBADO)
    and Red de seguridad
        F->>API: POST /api/expedientes/{id}/pago/sync
        API->>PS: sincronizarConMp(id)
        PS->>MP: search por external_reference
        PS-->>F: EstadoArancel (APROBADO / PENDIENTE / NINGUNO)
    end
```

El `sync` existe porque el webhook puede perderse (túnel caído, URL vieja atada a
una preferencia anterior). Ambos caminos hacen el **mismo upsert idempotente**, así
que ejecutarlos N veces no duplica filas ni re-notifica.

### 4.4 Compilado del expediente

```mermaid
flowchart LR
    Start["GET /documentos/compilado"] --> Est["EstructuraService<br/>estructura del expediente<br/>(incluye ranuras retiradas con archivo)"]
    Est --> Loop{"Por cada sección → ranura<br/>en orden"}
    Loop -->|tiene archivos| Anexar["Anexa PDF tal cual<br/>o encuadra la imagen en A4"]
    Loop -->|sin archivo y generable| Gen["Genera carátula / contrato<br/>con OpenPDF"]
    Anexar --> Next
    Gen --> Next
    Next{"¿Falló uno?"} -->|sí| Skip["Se omite y se loguea<br/>(no aborta el compilado)"]
    Next -->|no| Cont["Continúa"]
    Skip --> Fin
    Cont --> Fin{"¿0 páginas?"}
    Fin -->|sí| Err["PdfGenerationException"]
    Fin -->|no| Ok["PDF único descargable"]
```

### 4.5 Revisión externa (rol revisor)

```mermaid
sequenceDiagram
    autonumber
    participant R as Revisor (frontend)
    participant C as RevisorController
    participant S as RevisionExternaServiceImpl
    participant FS as FileStorageService
    participant G as Gemini

    R->>C: POST /api/revisor/revisiones (PDF ≤ 15 MB)
    C->>S: crear(archivo) — sync, con SecurityContext
    S->>S: exige RolRevisor · valida PDF y tamaño
    S->>FS: guarda en revisiones/{usuarioId}
    S-->>C: id (estado EN_PROGRESO)
    C->>S: analizarAsync(id) — cross-bean, aplica el proxy @Async
    C-->>R: 202 Accepted { id }

    S->>G: analizarPdf (visión nativa, sin rasterizar)
    alt resumen válido
        G-->>S: JSON con tipo, profesional, comitente, documentos, problemas
        S->>S: estado COMPLETADO + resultado JSONB
    else falla o PDF sin texto legible
        S->>S: estado ERROR + detalle legible
    end

    loop hasta que deje de estar EN_PROGRESO
        R->>C: GET /api/revisor/revisiones/{id}
    end
```

A diferencia del flujo del profesional (que arma ranura por ranura contra una
estructura conocida), acá la IA **solo describe lo que encuentra**: no sabe qué
debería haber, así que no juzga qué falta.

---

## 5. Arquitectura del frontend

```mermaid
flowchart TB
    subgraph Boot["Bootstrap"]
        Cfg["app.config.ts<br/>provideRouter · provideHttpClient<br/>LOCALE_ID es-AR · Lucide icons"]
    end

    subgraph Core["core/"]
        direction TB
        Guards["guards<br/>authGuard · guestGuard · revisorGuard"]
        Inter["interceptors<br/>jwtInterceptor · errorInterceptor"]
        Svcs["services<br/>Auth · Token · Catalogo · Comitente · Obra<br/>Expediente · Documento · Estructura<br/>ParametroAporte · Profesional · RevisionExterna"]
        Models["models (interfaces del contrato de la API)"]
        Const["constants/api.constants.ts<br/>única fuente de URLs"]
    end

    subgraph Layouts["layouts/"]
        AuthL["AuthLayout (público)"]
        MainL["MainLayout (privado)"]
    end

    subgraph Features["features/ · rutas lazy"]
        direction TB
        Auth["auth · login · register"]
        Dash["dashboard"]
        Prof["profile"]
        Com["comitentes"]
        Obr["obras"]
        Exp["expedientes<br/>wizard · armado · listado"]
        Pago["pago/pago-retorno (público)"]
        Legal["legal · terminos · privacidad (públicas)"]
        Ayuda["ayuda (FAQ)"]
        Rev["revisor · revision-list · revision-detalle"]
        Admin["admin · gestion-estructura · gestion-parametros"]
    end

    subgraph Shared["shared/"]
        Comps["components<br/>confirm-dialog · error-state<br/>cambiar-password · dar-de-baja · pagar-arancel"]
        Vals["validators<br/>dni-cuit · numero-orden · password-match"]
    end

    Cfg --> Inter
    Cfg --> Guards
    Guards --> Layouts
    Layouts --> Features
    Features --> Svcs
    Features --> Shared
    Svcs --> Const
    Svcs --> Models
    Inter -->|Bearer JWT / 401 → login| Svcs
    Rev -.revisorGuard.-> Guards
    Admin -.revisorGuard.-> Guards
```

Decisiones: componentes **standalone**, **signals** para el estado local,
**interceptores funcionales**, rutas cargadas de forma **lazy** por feature y
**alias de path** (`@core`, `@shared`, `@features`, `@layouts`, `@env`) en lugar
de rutas relativas profundas.

---

## 6. Despliegue — Docker Compose

Las secciones anteriores describen cómo está organizado el código; esta describe
**cómo corre**. Todo el sistema se levanta con `docker compose up -d --build` y
queda en `http://localhost:4200`.

```mermaid
flowchart TB
    Browser["Navegador"]

    subgraph Compose["docker compose · red previsar"]
        direction TB

        subgraph FE["previsar-frontend · nginx:1.27-alpine"]
            Nginx["nginx :80"]
            Static["SPA compilada<br/>/usr/share/nginx/html"]
        end

        subgraph BE["previsar-backend · eclipse-temurin:21-jre"]
            Api["Spring Boot :8080<br/>perfil docker · usuario no-root"]
        end

        subgraph DB["previsar-postgres · postgres:16-alpine"]
            Pg[("PostgreSQL :5432")]
        end

        VolPg[("volumen<br/>postgres_data")]
        VolFiles[("volumen<br/>previsar_storage<br/>/data/expedientes")]
    end

    subgraph Ext["Servicios externos"]
        direction TB
        Gemini["Google Gemini"]
        Mp["Mercado Pago"]
        Smtp["SMTP"]
    end

    Browser -->|":4200"| Nginx
    Nginx -->|"/ · assets"| Static
    Nginx -->|"proxy /api y /auth"| Api
    Api -->|"JDBC"| Pg
    Pg --- VolPg
    Api -->|"documentos subidos"| VolFiles

    Api --> Gemini
    Api --> Mp
    Api --> Smtp
    Mp -.->|"webhook a MP_WEBHOOK_URL_BASE"| Api

    Browser -.->|":8080 · solo debug/Swagger"| Api
```

### Servicios

| Servicio | Imagen base | Puerto en el host | Estado persistente |
| -------- | ----------- | ----------------- | ------------------ |
| `frontend` | `nginx:1.27-alpine` | `4200 → 80` | — (estáticos en la imagen) |
| `backend` | build sobre `eclipse-temurin:21-jre` | `8080 → 8080` | volumen `previsar_storage` |
| `postgres` | `postgres:16-alpine` | `5432 → 5432` | volumen `postgres_data` |

Ambas imágenes propias son **multi-stage**: las herramientas de build (Maven+JDK,
Node) quedan en la primera etapa y no viajan a la imagen final, que lleva solo un
JRE o nginx.

### Decisiones del despliegue

- **nginx hace de reverse proxy, no solo de servidor de estáticos.** Redirige
  `/api` y `/auth` al backend, así que navegador y API quedan en el **mismo
  origen**. Es lo que permite que el build de producción use rutas relativas
  (`environment.ts`) y hace que **CORS no entre en juego** en este stack. El
  `:8080` se publica solo por comodidad (Swagger, pegarle directo a la API).
- **Orden de arranque encadenado por healthchecks**, no por `depends_on` a secas:
  `postgres` sano (`pg_isready`) → `backend` sano (`GET /api-docs`, que solo
  responde con el contexto de Spring levantado y las migraciones aplicadas) →
  `frontend`. Sin esto, Flyway podía arrancar contra una base que todavía no
  aceptaba conexiones, y nginx devolvía 502 en el primer ingreso.
- **Perfil `docker` separado del perfil `dev`.** `dev` apunta a
  `localhost:5432`, que dentro de un contenedor es el propio contenedor; el
  perfil `docker` resuelve la base por el nombre del servicio (`postgres`) y baja
  el logging de SQL.
- **Dos volúmenes con responsabilidades distintas**: `postgres_data` (la base) y
  `previsar_storage` (los documentos subidos). Sobreviven a `down` y a que se
  reconstruyan las imágenes; se borran solo con `down -v`. Es coherente con la
  decisión de que los archivos no van en la base: si el volumen se pierde, las
  rutas guardadas en `documento_cargado` quedan apuntando a la nada.
- **El backend corre como usuario sin privilegios** (`previsar`, uid 1001), y la
  carpeta del volumen le pertenece: si no, los uploads fallan con *permission
  denied*.
- **La falta de una credencial externa no impide arrancar.** Lo único
  obligatorio es `JWT_SECRET`. Si faltan las de Gemini, Mercado Pago o SMTP la
  aplicación levanta igual y falla solo esa función al invocarla, en vez de no
  arrancar: así se puede trabajar sin conexión o sin cuota. Con el `.env`
  completo —el **mismo** que usa el desarrollo local— todas las integraciones
  funcionan dentro de los contenedores como fuera.

> El **webhook de Mercado Pago** es la única entrada que no pasa por nginx: es
> una llamada server-to-server desde MP hacia `MP_WEBHOOK_URL_BASE`, que en local
> requiere un túnel público (`ngrok http 8080`). Ver el flujo en la
> [sección 4.3](#43-pago-del-arancel-mercado-pago).

---

## 7. Esquema de base de datos (DBML)

Esquema físico de la base, generado a partir de las migraciones Flyway
`V001`–`V030` (refleja el estado final).

**▶ Ver el diagrama en dbdiagram.io:**
https://dbdiagram.io/d/PreVisar-Esquema-de-base-de-datos-6a0db795b62396d22c2afed1

> ⚠️ El diagrama enlazado fue generado sobre `V001`–`V015`. Actualizarlo con las
> tablas incorporadas después: `concepto_aporte`, `tipo_tarea_aporte`, `seccion`,
> `documento_requerido`, `documento_cargado`, `validacion_visual`, `pago` y
> `revision_externa`.

### Notas del esquema

- **Estado final tras los `ALTER`**: `rol_revisor.provincia_id` se eliminó en
  `V008` y se restauró en `V010`; `profesional.titulo` (texto libre) se reemplazó
  por `titulo_id` (FK) + `titulo_otro_descripcion` en `V012`; `obra.provincia_id`
  se agregó en `V010`; `parametro_aporte.concepto` (VARCHAR) se reemplazó por
  `concepto_id` (FK) en `V017`; las 4 columnas `expediente.aporte_*` se eliminaron
  en `V018`.
- **Enums = `VARCHAR + CHECK`**: en Postgres no son tipos enum nativos; en el
  diagrama se representan como enumeraciones solo para documentar los valores
  válidos (`rol`, `tipo_persona`, `estado`, `grupo`, `tipo_valor`, `base_calculo`).
- **Índices parciales** (cláusula `WHERE`):
  - `comitente (profesional_id, dni_cuit)` solo entre activos (`WHERE deleted_at IS NULL`);
  - un único vigente por concepto en `parametro_aporte` (`WHERE vigencia_hasta IS NULL AND activo`);
  - `expediente` por `profesional_id` `WHERE activo`;
  - `seccion (provincia_id, tipo_tarea_id, codigo)` y
    `documento_requerido (seccion_id, codigo)` `WHERE activo` — así, al dar de baja
    lógica una sección o documento, su código queda libre para recrearse.
- **Índices únicos de idempotencia**: `pago (mp_payment_id)` evita que reprocesar
  un webhook duplique el pago; `validacion_visual (documento_cargado_id,
  hash_documento)` evita re-guardar el análisis del mismo contenido.
- **JSONB**: `validacion_visual.resultado` y `revision_externa.resultado` guardan
  la respuesta cruda de Gemini. Se persiste el JSON tal cual (no columnas) porque
  el esquema del modelo puede cambiar y el consumo es de lectura completa.
- **Relaciones 1:1** (`usuario`↔`profesional`, `profesional`↔`rol_revisor`):
  garantizadas por el `UNIQUE` en la FK.
- **`parametro_aporte`** no tiene FK a `tipo_tarea` a propósito: es configuración
  con vigencia temporal. Qué conceptos aplican a cada tarea lo declara
  `tipo_tarea_aporte`.
- **Los archivos no viven en la base**: `documento_cargado.ruta_relativa` y
  `revision_externa.ruta_relativa` apuntan al filesystem
  (`previsar.storage.base-path`). La baja de un documento es lógica en la base y
  *best-effort* en disco: no son transaccionales entre sí.
