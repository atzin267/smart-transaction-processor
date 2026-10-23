# AI_USAGE.md — Uso de IA en este proyecto

**Herramienta:** Claude (Claude Code), como asistente de implementación integrado en el editor.

## Dirección Arquitectónica y Criterio de Negocio (Mi Rol)

El diseño conceptual y las reglas de negocio fueron definidos íntegramente por mí antes y durante el desarrollo: la matriz de transiciones de estado, la idempotencia por `eventId`, el criterio lógico para clasificar eventos `STALE` y la elección de Jakarta Bean Validation. 

La implementación se realizó de forma colaborativa e iterativa, la lógica se discutió, se prototipó, se auditó línea por línea y se corrigió activamente. **Cada decisión de negocio, cada detección de bugs y cada aceptación final dependieron estrictamente de mi revisión crítica.**

---

## 1. Historial de Commits (Estructura de Trabajo)

El desarrollo se ejecutó y verificó de forma atómica en el siguiente orden secuencial (verificable en `git log --oneline`):

* `Agregar Maven Wrapper para build reproducible`
* `Agregar modelo de datos: Event, Transaction, Status, ProcessResult`
* `Implementar TransactionService con las reglas de negocio`
* `Agregar manejo global de errores HTTP`
* `Agregar endpoints POST /events y GET /transactions/{id}`
* `Agregar pruebas unitarias de TransactionService`
* `Agregar pruebas de integración de la API con MockMvc`
* `Completar README.md, DECISIONS.md y AI_USAGE.md`
* `Corregir orden STALE/transición y códigos HTTP de rutas inválidas`
* `Actualizar documentación con las correcciones de STALE y HTTP`

---

## 2. Puntos Clave de Control y Auditoría Técnica

Mi intervención activa como ingeniero se reflejó en momentos críticos donde detecté y corregí fallas que la herramienta no resolvió por sí sola:

* **Auditoría de estado (`git log`):** Al iniciar la revisión, detecté que el código asumido no existía y que solo estaba el esqueleto del commit inicial, verificándolo de forma independiente antes de avanzar.
* **Resolución de vacíos de negocio:** Ante la falta de especificación para el primer evento de una transacción nueva, determiné por criterio propio que el primer estado debe ser estrictamente `CREATED` (rechazando cualquier otro con `409`), implementando pruebas específicas.
* **Detección y corrección de un bug lógico crítico:** Durante la revisión, detecté que un evento con timestamp viejo pero con una transición inválida contra el estado actual respondía `409` en lugar de `STALE`. Corregí el orden en `process()` para evaluar el timestamp antes que la transición y añadí la prueba `eventoViejoConTransicionYaSuperadaEsStaleNoConflicto`.
* **Corrección de manejo de errores HTTP (500 vs 409/404):** Detecté que rutas inexistentes o métodos no soportados caían en el `@ExceptionHandler` genérico arrojando un error `500`. Ajusté el diseño para que las excepciones nativas de Spring conserven sus códigos reales (404/405/415), respaldado con la prueba `rutaInexistente_devuelve404_yMetodoNoPermitido_devuelve405`.
* **Claridad conceptual (`STALE`):** Validé línea por línea la condición `event.timestamp().isBefore(current.lastUpdated())` usando los casos `EVT-1001` a `EVT-1003` para separar el orden de llegada HTTP del orden lógico por `timestamp`.

---

## 3. Verificación Forense de Resultados

Ningún resultado se dio por bueno sin validación empírica:
1. **Compilación incremental:** Ejecución de `mvnw.cmd compile` tras cada bloque de código.
2. **Auditoría de pruebas en XML:** Inspección directa de los reportes Surefire (`target/surefire-reports/*.xml`), confirmando `tests=18, failures=0, errors=0` más allá del resumen de consola.
3. **Pruebas funcionales en vivo:** Levantamiento del servidor real (`spring-boot:run`) interactuando mediante peticiones `curl` para validar códigos HTTP, cuerpos de respuesta y comprobación de persistencia vía `GET`.

---

## 4. Aprendizajes Técnicos y Próximos Pasos

* **Jakarta Bean Validation:** Se investigó la propagación automática de anotaciones (`@NotBlank`, `@NotNull`) en parámetros de *record* hacia los constructores generados, activándose mediante `@Valid` en los controllers sin validación manual.
* **Áreas de mejora identificadas:** Implementar validación cruzada a nivel de clase (`@AssertTrue` / `@Constraint`) para reglas multi-campo, y evaluar alternativas de menor contención al `synchronized` actual (como `ConcurrentHashMap.compute()`) para escenarios de alta concurrencia.

---

## Ejemplos de prompts

1. **Revisión antes de cambiar nada**
   > "Revisa el código existente: dime si compila, si ves bugs, casos borde que falten o algo que contradiga mis reglas de negocio (duplicados → DUPLICATE, evento más viejo → STALE, transiciones inválidas → 409). Explícame cada hallazgo antes de cambiar nada."

   Resultado: en esa revisión salió que el código que creía tener (`TransactionService`, los controllers, etc.) todavía no existía en el repositorio — solo el esqueleto del primer commit. Lo confirmé con `git log --oneline --stat` antes de seguir.

2. **Búsqueda de bugs y casos borde**
   > **Contexto:** Servicio Spring Boot que recibe eventos de transacciones que pueden llegar duplicados, incompletos o fuera de orden. Reglas: mismo `eventId` → DUPLICATE; timestamp anterior al último aplicado → STALE; primer evento debe ser CREATED; COMPLETED y FAILED son finales; transición inválida → 409.
   > **Tarea:** Revisa `TransactionService`, los controllers y `GlobalExceptionHandler` buscando bugs, condiciones de carrera y casos borde que no cumplan esas reglas.
   > **Restricciones:** No modifiques código. Para cada hallazgo dame: el escenario exacto (eventos y orden en que llegan), el resultado actual, el esperado y su severidad.
   > **Formato:** Una tabla ordenada de mayor a menor severidad.

   Resultado: de ahí salieron los dos bugs ya documentados en la sección 2 — el orden STALE/transición y el 500 en rutas inválidas — que terminé corrigiendo e implementando en este repositorio.

3. **Ampliar la cobertura de pruebas**
   > **Contexto:** Mismas reglas de negocio. Ya tengo varias pruebas (unitarias del servicio y de integración con MockMvc) que cubren: evento válido, campo faltante, JSON roto, status desconocido, consulta inexistente, duplicado, STALE, transición inválida, primer evento distinto de CREATED, ruta inexistente y método no permitido.
   > **Tarea:** Propón los casos de prueba que faltan, priorizados por riesgo. Usa técnicas como partición de equivalencia, valores límite y transición de estados (todas las combinaciones de estado actual → nuevo).
   > **Restricciones:** Primero solo la lista de casos con el resultado esperado de cada uno; yo elijo cuáles implementar. Nada de dependencias nuevas. Cada prueba debe ser independiente.
   > **Formato:** Tabla con caso, entrada, resultado esperado y por qué importa.

   Resultado: de la lista propuesta, los casos de ruta inexistente y método no permitido (`rutaInexistente_devuelve404_yMetodoNoPermitido_devuelve405`) ya están implementados; el resto queda como pendiente de evaluar.

## Datos sensibles

No compartí credenciales, secretos ni información confidencial con las herramientas de IA; solo el enunciado del reto y el código de este proyecto.