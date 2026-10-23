# Notas de uso de IA (borrador personal)

Formato por entrada:
- Fecha/hora:
- Herramienta:
- Prompt (resumen):
- Qué me dio:
- Qué acepté / qué cambié y por qué:
- Cómo lo verifiqué (prueba, lectura de código, documentación):

---

## 1. Creación del esqueleto del proyecto
- Herramienta: Claude
- Prompt: pedí generar el esqueleto de un proyecto Spring Boot 3 con Java 21, Web y Validation.
- Qué me dio: pom.xml, clase principal, application.properties, .gitignore y prueba contextLoads.
- Qué revisé: versiones (Spring Boot 3.4.5, Java 21), que solo estuvieran las dependencias necesarias.
- Verificación: correr `mvn spring-boot:run` y `mvn test`.
