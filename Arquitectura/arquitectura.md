# MIGRACIÓN DE NGINX — Guía para migrar desde `tpi-compose`

⚠️ Objetivo: copiar y adaptar la configuración funcional de `nginx` desde el repo `tpi-compose` (`nginx/nginx.conf` + `compose.yml`) a otro repo destino (o a Kubernetes) y dejarlo listo para ejecutarse con `docker-compose` (o adaptar a K8s). 

---

## 1. Resumen 🔎

- Origen: `c:\Users\dario\source\repos\tpi\tpi-compose`
  - Archivo de Nginx: `nginx/nginx.conf`
  - Docker Compose: `compose.yml`
- Entregable esperado: `nginx` operativo en el repo destino, con la misma lógica de proxying, CORS y headers, documentado y con tests automatizados básicos.

---

## 2. Qué hace la configuración original ✅

- Puerto 80: reenvía a `webapp:4200` (página front).
- Puerto 3000: reenvía a `api-proxy:3000` y aplica cabeceras CORS y `Access-Control-Allow-*`.
- Varias cabeceras se agregan: `Upgrade`, `Connection`, `Host`, `X-Real-IP`, `X-Forwarded-*`, `Origin`, `Authorization`.
- Compose monta el archivo `./nginx/nginx.conf` en `/etc/nginx/conf.d/default.conf` dentro del contenedor.
- En `compose.yml` otros servicios esperan al `nginx` (o config usan `http://nginx:8080` internamente).

---

## 3. Consideraciones previas 🔧

- Comprueba si en el repo destino ya existe un servicio `nginx`; si existe, decide si lo actualizarás o lo reemplazarás.
- Identifica si el destino utiliza Docker Compose o Kubernetes. Sugerencias:
  - Docker Compose: copiar `nginx/nginx.conf`, añadir `service` `nginx` al `docker-compose.yml` del destino, montar volúmenes, redes y puertos.
  - Kubernetes: convertir `nginx.conf` a `ConfigMap`, crear `Deployment` y `Service`, o usar `Ingress` y reglas equivalentes.
 - Asegura que redes y nombres de servicios coincidan (p.ej. `webapp`, `api-proxy`). Si varían, usar `envsubst` o placeholders en la configuración.
- Usa imagen de `nginx` con tag estable (recomendado: `nginx:1.26-alpine` o similar) — evita `latest` para reproducibilidad.

---

## 4. Pasos para migrar (Docker Compose) 🚀

1. Inspección:
   - Detectar si existe `nginx` en el repo destino y confirmar si hay conflictos de puerto/red.
2. Copiar archivo:
   - Copia `nginx/nginx.conf` → `TARGET_REPO/nginx/nginx.conf`.
3. Añadir servicio `nginx` en el `docker-compose.yml` del repo destino:

```yaml
nginx:
  image: nginx:latest
  container_name: reverse_proxy
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/conf.d/default.conf:ro
  ports:
    - "80:80"
  networks:
    - back-end-network
    - front-end-network
  depends_on:
    - webapp
```

> Ajusta `ports` si el destino requiere otros host ports.

4. Redes y nombres de servicio:
  - Verifica la red y el nombre del servicio `webapp`, `api-proxy` en el destino. Si los nombres difieren, adapta `nginx.conf` o utiliza `envsubst` para sustituir en runtime.

5. Variables de entorno: si alguna configuración apuntaba anteriormente a `http://nginx:8080`, actualiza o elimina esas variables para que apunten al servicio correcto (por ejemplo, directamente a `api-proxy` o al backend).

6. Templatización (recomendado para flexibilidad):
  - Convierte el `nginx.conf` a `default.conf.template` con placeholders `${API_PROXY_HOST}`, `${WEBAPP_HOST}`.
   - Usa un `Dockerfile` y un `entrypoint.sh` para `envsubst`:

```dockerfile
FROM nginx:alpine
COPY default.conf.template /etc/nginx/conf.d/default.conf.template
COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
```

`entrypoint.sh`:
```bash
#!/bin/sh
envsubst '$$API_PROXY_HOST $$WEBAPP_HOST' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf
exec "$@"
```

7. Documentación: incluir en README del repo destino sección de Nginx y pasos para ejecutar.

---

## 5. Pruebas / verificación ✔️

Crea un script `scripts/check-nginx-proxy.sh` con estos pasos (bash/powershell):

 - Levantar contenedores mínimos (nginx, webapp, api-proxy):
```powershell
# En PowerShell
docker compose -f docker-compose.yml up -d nginx webapp api-proxy
```

- Pruebas HTTP / CORS:

```powershell
curl -i http://localhost/  # 200 (webapp)
curl -i http://localhost:3000/ | grep -i 'Access-Control-Allow-Origin'  # debe contener '*'
// Nota: se omite la comprobación de `:8080/actuator/health`.
```

- Comprobación desde contenedor:

```powershell
docker exec -it api-proxy curl -I http://localhost:3000/  # health check del proxy desde dentro del contenedor
```

- Verificar logs:

```powershell
docker logs nginx
```

---

## 6. Kubernetes (si aplica) ☸️

- Crea `ConfigMap` para la `nginx.conf`.
- Crea un `Deployment` con la imagen `nginx` y monta el `ConfigMap` al path `/etc/nginx/conf.d/default.conf`.
  - Crea `Service` y `Ingress` con rutas equivalentes:
  - `/` → `webapp`
  - puerto 3000 → `api-proxy`
- Reemplaza directivas de `add_header` y `proxy_set_header` según política de seguridad del cluster.

---

## 7. Branch y PR (gestión de cambios) 🧾

- Crea branch: `chore/nginx-migration-YYYYMMDD`
- Commits: dividir por pasos lógicos, ejemplo:
  1. `feat(nginx): add nginx config and service to compose`
  2. `chore(nginx): add templating and entrypoint` (si aplica)
  3. `test(nginx): add integration script and CI job`
- PR description: objetivo, archivos cambiados, pruebas ejecutadas y resultados.

---

## 8. Lista de verificación (QA) ✅

- [ ] `nginx/nginx.conf` copiado al repo destino
- [ ] `nginx` service agregado al `docker-compose.yml` o manifests k8s, con los ports y redes necesarias
 - [ ] `API_PROXY` resuelve correctamente desde `nginx` (internal DNS)
- [ ] `scripts/check-nginx-proxy.sh` incluido y pasa localmente
- [ ] README/documentación actualizada con pasos para levantar servicios y variables
- [ ] PR creado con descripción y checklist completado
- [ ] CI job agregado (opcional) que ejecuta las comprobaciones automáticamente

---

## 9. Puntos importantes / Preguntas para poner en el PR ❓

 - ¿Alguna variable de entorno apunta a `http://nginx:8080`? Si es así, actualízala para apuntar al servicio y puerto correctos (por ejemplo, al `api-proxy` o al backend directo).
 - ¿Los nombres de los servicios (`webapp`, `api-proxy`) coinciden en el destino? Si no, detallar los nombres nuevos.
- ¿Se aceptan plantillas con `envsubst` para flexibilidad o prefieres usar `sed`/`awk` o reemplazo en CI?
- ¿Se debe exponer el puerto 3000 en el host? (Original: mapeado en `patient-portal` pero no en `nginx` de `compose.yml`)

---

## 10. Referencias y comandos útiles 🧪

- Levantar nginx y servicios mínimos:

```powershell
# En PowerShell
docker compose -f compose.yml up -d nginx webapp api-proxy
```

- Limpiar:

```powershell
docker compose -f compose.yml down -v
```

- Logs:

```powershell
docker logs -f nginx
```

- Comprobación rápida:

```powershell
curl -i http://localhost/
curl -i http://localhost:3000/ | grep -i Access-Control-Allow-Origin
// Nota: se omitió la comprobación de `:8080/actuator/health`.
```

---

