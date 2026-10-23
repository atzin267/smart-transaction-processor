# Smart Transaction Event Processor

Servicio que recibe eventos de actualización de transacciones y mantiene el estado de cada una, tolerando eventos repetidos, incompletos o fuera de orden.

## Requisitos

Java 21. No necesitas Maven instalado: el repo incluye el Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Ejecutar

**Windows:**
```
mvnw.cmd spring-boot:run
```

**bash:**
```bash
./mvnw spring-boot:run
```

Levanta en `http://localhost:8080`.

## Endpoints

### POST /events

Campos obligatorios: `eventId`, `transactionId`, `type`, `status` (`CREATED`/`PROCESSING`/`COMPLETED`/`FAILED`), `timestamp` (ISO-8601 UTC).

**Windows CMD:**
```
curl -X POST http://localhost:8080/events -H "Content-Type: application/json" -d "{\"eventId\":\"EVT-1001\",\"transactionId\":\"TXN-5001\",\"type\":\"STATUS_UPDATED\",\"status\":\"CREATED\",\"timestamp\":\"2026-10-21T14:30:00Z\"}"
```

**bash:**
```bash
curl -X POST http://localhost:8080/events \
  -H "Content-Type: application/json" \
  -d '{"eventId":"EVT-1001","transactionId":"TXN-5001","type":"STATUS_UPDATED","status":"CREATED","timestamp":"2026-10-21T14:30:00Z"}'
```

Respuesta (200):
```json
{"result":"APPLIED","eventId":"EVT-1001","transactionId":"TXN-5001"}
```
`result`: `APPLIED` (cambió el estado), `DUPLICATE` (eventId repetido) o `STALE` (timestamp más viejo que el último aplicado; se acepta pero no cambia el estado, aunque su status ya no fuera una transición válida).

### GET /transactions/{transactionId}

```
curl http://localhost:8080/transactions/TXN-5001
```

200 si existe (`transactionId`, `status`, `lastUpdated`, `lastEventId`), 404 si no.

## Errores

| Código | `error` | Causa |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Falta un campo obligatorio o tiene un valor inválido (`details` trae el campo exacto). |
| 400 | `MALFORMED_REQUEST` | JSON roto o `status` desconocido. |
| 409 | `INVALID_TRANSITION` | Transición de estado no permitida (incluye primer evento distinto de `CREATED`). |
| 404 | `NOT_FOUND` | La transacción no existe. |
| 404 / 405 / 415 | `HTTP_<código>` | Ruta inexistente, método no permitido o `Content-Type` no soportado. |
| 500 | `INTERNAL_ERROR` | Error no controlado (se registra en el log). |

## Pruebas

```
mvnw.cmd test
```
(`./mvnw test` en bash)

18 pruebas: unitarias de `TransactionService` y de integración con MockMvc.

## Más detalle

Arquitectura, supuestos y limitaciones en [DECISIONS.md](DECISIONS.md). Uso de IA en [AI_USAGE.md](AI_USAGE.md).
