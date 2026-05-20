# Script de verificación de infraestructura Docker
# Mortal Kombat Match System

Write-Host "=== Verificación de Infraestructura Docker ===" -ForegroundColor Cyan
Write-Host ""

# Función para verificar si un puerto está escuchando
function Test-Port {
    param($Port)
    try {
        $connection = New-Object System.Net.Sockets.TcpClient("localhost", $Port)
        $connection.Close()
        return $true
    } catch {
        return $false
    }
}

# 1. Verificar que Docker está corriendo
Write-Host "1. Verificando Docker..." -ForegroundColor Yellow
try {
    docker info | Out-Null
    Write-Host "   ✓ Docker está corriendo" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Docker no está corriendo o no está instalado" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 2. Verificar contenedores
Write-Host "2. Verificando contenedores..." -ForegroundColor Yellow
$containers = @(
    "characters-db",
    "match-db",
    "characters-be",
    "random-be",
    "match-be",
    "frontend",
    "reverse-proxy"
)

foreach ($container in $containers) {
    $status = docker ps --filter "name=$container" --format "{{.Status}}"
    if ($status) {
        Write-Host "   ✓ $container : $status" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $container : No está corriendo" -ForegroundColor Red
    }
}

Write-Host ""

# 3. Verificar puertos
Write-Host "3. Verificando puertos expuestos..." -ForegroundColor Yellow
$ports = @{
    "1583" = "Frontend"
    "5712" = "API Gateway"
    "3307" = "Characters DB"
    "3308" = "Match DB"
}

foreach ($port in $ports.Keys) {
    if (Test-Port -Port $port) {
        Write-Host "   ✓ Puerto $port ($($ports[$port])) está escuchando" -ForegroundColor Green
    } else {
        Write-Host "   ✗ Puerto $port ($($ports[$port])) NO está escuchando" -ForegroundColor Red
    }
}

Write-Host ""

# 4. Verificar health checks
Write-Host "4. Verificando health checks..." -ForegroundColor Yellow

Write-Host "   Verificando API Gateway health..." -ForegroundColor Gray
try {
    $response = Invoke-WebRequest -Uri "http://localhost:5712/health" -Method GET -TimeoutSec 5 -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✓ API Gateway health check: OK" -ForegroundColor Green
    }
} catch {
    Write-Host "   ✗ API Gateway health check: FAILED" -ForegroundColor Red
}

Write-Host ""

# 5. Verificar conectividad de microservicios
Write-Host "5. Verificando microservicios a través del proxy..." -ForegroundColor Yellow

$endpoints = @{
    "http://localhost:5712/api/matches/history" = "Match Service"
    "http://localhost:5712/api/characters" = "Characters Service"
    "http://localhost:5712/api/v1/dice" = "Random Dice Service"
}

foreach ($endpoint in $endpoints.Keys) {
    Write-Host "   Probando $($endpoints[$endpoint])..." -ForegroundColor Gray
    try {
        $response = Invoke-WebRequest -Uri $endpoint -Method GET -TimeoutSec 5 -UseBasicParsing
        Write-Host "   ✓ $($endpoints[$endpoint]): Status $($response.StatusCode)" -ForegroundColor Green
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.Value__
        if ($statusCode) {
            Write-Host "   ⚠ $($endpoints[$endpoint]): Status $statusCode (puede ser esperado)" -ForegroundColor Yellow
        } else {
            Write-Host "   ✗ $($endpoints[$endpoint]): Sin respuesta" -ForegroundColor Red
        }
    }
}

Write-Host ""

# 6. Verificar frontend
Write-Host "6. Verificando Frontend..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:1583/" -Method GET -TimeoutSec 5 -UseBasicParsing
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✓ Frontend accesible en http://localhost:1583" -ForegroundColor Green
    }
} catch {
    Write-Host "   ✗ Frontend no accesible" -ForegroundColor Red
}

Write-Host ""

# 7. Resumen de redes
Write-Host "7. Redes Docker..." -ForegroundColor Yellow
docker network ls | Select-String -Pattern "backend-network|frontend-network"

Write-Host ""

# 8. Resumen de volúmenes
Write-Host "8. Volúmenes Docker..." -ForegroundColor Yellow
docker volume ls | Select-String -Pattern "characters_db_data|match_db_data"

Write-Host ""
Write-Host "=== Verificación Completada ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para ver logs en tiempo real:" -ForegroundColor White
Write-Host "  docker compose logs -f" -ForegroundColor Gray
Write-Host ""
Write-Host "Para ver logs de un servicio específico:" -ForegroundColor White
Write-Host "  docker compose logs -f <service-name>" -ForegroundColor Gray
Write-Host ""
Write-Host "Accesos rápidos:" -ForegroundColor White
Write-Host "  Frontend:     http://localhost:1583" -ForegroundColor Gray
Write-Host "  API Gateway:  http://localhost:5712" -ForegroundColor Gray
Write-Host "  API Health:   http://localhost:5712/health" -ForegroundColor Gray
