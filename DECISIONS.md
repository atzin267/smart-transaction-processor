# DECISIONS (borrador)

## Supuestos y reglas de negocio
- Campos obligatorios: eventId, transactionId, type, status, timestamp (ISO-8601 UTC).
- Estados válidos: CREATED, PROCESSING, COMPLETED, FAILED.
- Evento duplicado (mismo eventId): no se vuelve a procesar; se responde 200 con resultado DUPLICATE (idempotencia).
- Evento más antiguo que el último aplicado a la transacción: se acepta pero no modifica el estado actual; resultado STALE.
- Transiciones permitidas:
  - CREATED -> PROCESSING | COMPLETED | FAILED
  - PROCESSING -> PROCESSING | COMPLETED | FAILED
  - COMPLETED y FAILED son estados finales.
  - Transición inválida -> 409 Conflict.
- Datos inválidos o JSON mal formado -> 400 con lista de errores.

## Persistencia
- En memoria (ConcurrentHashMap). Limitación: se pierde al reiniciar y no escala a varias instancias.

## Tecnología nueva
- Bean Validation (Jakarta Validation / Hibernate Validator). (Completar: qué sabía, cómo investigué, qué apliqué, cómo lo comprobé, qué falta aprender.)

## Pendientes / siguientes pasos
- (Completar al final)
