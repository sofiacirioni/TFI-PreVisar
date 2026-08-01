# Ilustraciones

Set de [unDraw](https://undraw.co) (Katerina Limpitsouni). Su
[licencia](https://undraw.co/license) permite uso comercial y personal sin
atribución; se deja constancia acá igual, por trazabilidad del proyecto.

Restricciones relevantes: no se pueden redistribuir como pack ni usar para
entrenar modelos de IA. Acá se usan como decoración de pantalla, que es el uso
previsto.

## Procesamiento aplicado

1. Descarga individual desde `cdn.undraw.co` (solo las piezas en uso).
2. `npx svgo@3 --multipass` para sacar el markup sobrante.
3. Reemplazo del acento por defecto de unDraw (`#6C63FF`) por el de PreVisar
   (`#0E7FF2`), para que el set hable el mismo idioma cromático que la app.

El acento quedó **fijo** y no como variable CSS a propósito: se consumen vía
`<img src>`, y un SVG referenciado así es un documento aparte que no puede leer
las custom properties de la página. Si algún día se activa el tema oscuro
(los tokens ya existen pero todavía no hay forma de conmutarlo desde la UI),
habría que pasarlas a SVG inline para que sigan a `--accent`.

## Mapa de uso

| Archivo | Pantalla | Original en unDraw |
|---|---|---|
| `login.svg` | Login (columna izquierda) | `software-engineer` |
| `empty-expedientes.svg` | Listado de expedientes vacío + dashboard | `file-manager` |
| `empty-comitentes.svg` | Listado de comitentes vacío | `meet-the-team` |
| `empty-obras.svg` | Listado de obras vacío | `property-agreement` |
| `revisar.svg` | Revisar expediente (estado vacío) | `file-searching` |
| `error.svg` | Error genérico de carga | `document-warning` |
| `sin-conexion.svg` | Error de red / sin conexión | `connection-lost` |
| `legal-terminos.svg` | Términos y Condiciones (fondo) | `contract-signed` |
| `legal-privacidad.svg` | Política de Privacidad (fondo) | `private-data` |

Todas son decorativas: van con `alt=""` para que los lectores de pantalla las
ignoren, salvo que aporten información que el texto no dé.
