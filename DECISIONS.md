# DECISIONS.md

## Arquitectura y estructura

Spring Boot estándar en capas (`model`, `service`, `controller`, `exception`, `dto`), sin dependencias innecesarias — nada de base de datos, Lombok o seguridad.

En memoria: `ConcurrentHashMap` para guardar las transacciones y un `Set` independiente para la idempotencia por `eventId`. El método `process()` usa `synchronized` para evitar condiciones de carrera, suficiente para el volumen de este reto.

## Decisiones clave de negocio

- **Idempotencia primero:** si el `eventId` ya fue visto, responde `DUPLICATE` inmediato, sin evaluar nada más.
- **Orden de validación:** duplicado ➔ ¿existe la transacción? ➔ ¿timestamp obsoleto (`STALE`)? ➔ ¿transición válida? ➔ aplicar.
- **Por qué `STALE` va antes que la transición:** en mi primera versión validaba la transición primero, y al revisar un caso fuera de orden encontré un error: `CREATED` (10:00) → `COMPLETED` (10:05) y después llega tarde `PROCESSING` (10:02). Ese evento es legítimo —pasó antes, solo llegó tarde—, pero como `COMPLETED → PROCESSING` no es una transición válida, respondía `409`. Lo correcto es `STALE`: un evento más viejo que el último aplicado ya fue superado, así que no tiene sentido compararlo contra el estado actual. Lo corregí y agregué una prueba con exactamente ese caso.
- **Excepción a la regla:** las transiciones inválidas no se guardan como procesadas, permitiendo corregir y reenviar el mismo `eventId`. En cambio, los eventos `STALE` sí se registran, porque son válidos — solo que obsoletos.
- **Regla del primer evento:** como el contrato no definía cómo nace una transacción, decidí por criterio propio que el primer evento debe iniciar en `CREATED`. Cualquier otro estado inicial se rechaza con `409 Conflict`.

## Prioridades

- Primero, que la máquina de estados funcionara bien (duplicado/stale/transición/aplicado) — es el núcleo real del ejercicio.
- Después, pruebas que cubrieran cada regla de negocio, no solo los 3 casos mínimos que pedía el reto.
- Luego, mensajes de error claros y consistentes entre sí.
- La documentación, al final, una vez verificado todo con pruebas automatizadas y con el servicio corriendo de verdad.
- Lo que dejé sin tocar a propósito: formato estricto de `transactionId`/`eventId`, un catálogo cerrado para `type`, cualquier persistencia real — nada de eso pedía el reto.

## Stack y validación

- **Jakarta Bean Validation:** en un *record*, las anotaciones puestas en sus componentes (`@NotBlank`, `@NotNull`) se aplican también a los campos y accessors generados; Hibernate Validator valida esos campos, y `@Valid` en el `@RequestBody` hace que Spring valide el objeto antes de entrar al método del controller. Así no hay validaciones manuales en el código.
- **Manejo de errores:** un único `@RestControllerAdvice` centraliza todas las excepciones de dominio y validación. Los errores HTTP propios de Spring (ruta inexistente, método no permitido, `Content-Type` no soportado) conservan su código (404/405/415) en vez de caer en el 500 genérico, y los errores realmente inesperados se registran en el log.

## Aprendizaje de tecnología nueva: Jakarta Bean Validation

- **Qué sabía antes:** había visto `@NotNull` de pasada en algún tutorial, nunca integrado en un proyecto propio.
- **Cómo investigué:** documentación de Jakarta Validation / Hibernate Validator, probando cada comportamiento con código real en vez de quedarme solo leyendo.
- **Qué aplicé:** `@NotBlank`/`@NotNull` en los parámetros del record `Event`, y `@Valid` en el `@RequestBody` del controller.
- **Cómo comprobé:** escribí una prueba que manda un evento sin `transactionId` y verifica que ese campo exacto aparece en la respuesta de error — no basta con que falle, tiene que señalar el campo correcto.
- **Qué me falta:** validación cruzada entre varios campos (`@AssertTrue` en un método, o un `@Constraint` propio) — no la necesité aquí porque ninguna regla depende de más de un campo a la vez.

## Limitaciones conocidas

- **Sin persistencia real:** el estado vive en RAM y se pierde al reiniciar.
- **Una sola instancia:** si se levantaran varias réplicas, cada una tendría su propia memoria; para escalar, el estado tendría que vivir fuera del proceso (por ejemplo, una base de datos con índice único en `eventId`).
- **Sin historial:** solo guardo el estado actual y qué `eventId` ya se procesaron, no la lista de eventos por transacción.
- **Primer evento obligatorio `CREATED`:** si un evento de otro estado llega antes que el `CREATED`, se rechaza con `409`; como no queda registrado, el sistema externo puede reenviarlo después. La alternativa sería aceptar cualquier estado inicial, pero preferí no crear transacciones que "aparezcan" ya terminadas.
- **Concurrencia simple:** bloqueo de grano grueso con `synchronized`. Para alta concurrencia real se requeriría un lock por clave o `ConcurrentHashMap.compute()`.
