# CONTEXT - Arquitectura y Guía de Crecimiento

## 1. Objetivo del documento

Este documento define cómo debe evolucionar el proyecto `CredVault` para mantener coherencia técnica, seguridad y mantenibilidad.

## 2. Arquitectura actual del proyecto

Stack principal:
- Java 21
- Spring Boot 3 (Web, Thymeleaf, Security, Validation, Data JPA, OAuth2 Client)
- Hibernate/JPA
- Cifrado de secretos con AES-GCM mediante `EncryptedStringConverter`

Estructura por paquetes (arquitectura en capas):
- `controller`: capa web MVC (entrada HTTP, binding de formularios, navegación de vistas, códigos de respuesta/redirección)
- `service`: casos de uso y reglas de negocio
- `repository`: acceso a datos con Spring Data JPA
- `domain`: entidades JPA y enums de dominio
- `dto`: formularios/objetos de intercambio de datos para UI
- `security`: autenticación, autorización y sincronización OAuth
- `crypto`: cifrado/descifrado de campos sensibles
- `logging`: auditoría y trazabilidad de acciones

Recursos:
- `src/main/resources/templates`: vistas Thymeleaf
- `src/main/resources/static`: CSS y estáticos
- `application*.yml`: configuración por perfil

Tests:
- `src/test/java/.../controller`: pruebas unitarias de controladores con mocks
- `src/test/java/.../service`: pruebas unitarias de servicios
- tests de seguridad/logging/integración según componente

## 3. Principios de diseño obligatorios

1. Controladores delgados
- Los controladores no deben contener lógica de negocio ni decisiones complejas de dominio.
- Responsabilidades permitidas: parseo de entrada, validación básica, llamada a servicio, preparación del `Model`, respuesta HTTP.
- Si un método de controlador empieza a crecer en filtros/reglas/comparadores, mover esa lógica a un servicio específico.

2. Negocio en servicios
- Toda regla funcional debe residir en `service`.
- Los servicios deben exponer operaciones con nombres de caso de uso (`create`, `update`, `restore`, etc.).
- Aplicar `@Transactional` cuando haya cambios de estado o varias operaciones de persistencia.

3. Repositorios sin negocio
- Los repositorios solo ejecutan consultas/persistencia.
- No incluir reglas funcionales, validaciones de negocio ni transformaciones complejas en repositorios.

4. Entidades enfocadas al dominio
- Las entidades `domain` modelan estado y relaciones.
- Evitar acoplar entidades a detalles de UI o HTTP.

5. DTOs/formularios para la capa web
- No exponer entidades JPA directamente en formularios/vistas cuando haya riesgo de acoplamiento.
- Toda validación de entrada debe declararse en DTOs (`jakarta.validation`) y complementarse en servicio cuando sea regla de negocio.

## 4. Buenas prácticas de testing (obligatorias)

1. Siempre añadir tests unitarios en cada cambio funcional.
2. Si se modifica un servicio:
- añadir/ajustar test de servicio para la nueva regla;
- cubrir casos felices y de error.
3. Si se modifica un controlador:
- añadir/ajustar test de controlador para flujos de vista, validación y redirecciones.
4. Corregir o ampliar tests existentes antes de fusionar cambios.
5. Evitar merges con lógica nueva sin cobertura razonable.

Regla práctica:
- Cambio pequeño: mínimo 1 test que falle antes y pase después.
- Cambio con varias ramas: al menos 1 test por rama relevante (éxito, validación, error/permisos).

## 5. Seguridad y secretos

1. Nunca logar secretos (passwords, tokens, valores cifrados/descifrados).
2. Nunca hardcodear claves o credenciales.
3. Mantener `APP_ENCRYPTION_KEY` exclusivamente en entorno seguro.
4. Revisar autorizaciones por endpoint en cada funcionalidad nueva.
5. Cualquier cambio de autenticación/autorización debe ir con tests de seguridad.

## 6. Persistencia y modelo de datos

1. Cambios de modelo deben preservar compatibilidad o incluir estrategia de migración.
2. Mantener consistencia de soft-delete (`deleted`, `deletedAt`, `deletedBy`) en cualquier operación relacionada.
3. Operaciones que afecten histórico (`CredentialHistory`) deben conservar trazabilidad.
4. Evitar consultas N+1 en flujos de listado; optimizar repositorios cuando sea necesario.

## 7. Criterios de calidad de código

1. Métodos cortos y con una sola responsabilidad.
2. Nombres explícitos y orientados a dominio.
3. Evitar duplicación: extraer utilidades o servicios específicos.
4. Manejar errores de forma consistente (mensajes claros para usuario y excepciones adecuadas en negocio).
5. No introducir deuda técnica silenciosa: si algo queda pendiente, documentarlo en TODO con contexto breve.

## 8. Convenciones para nuevas funcionalidades

Flujo recomendado:
1. Definir caso de uso y permisos.
2. Crear/ajustar DTO + validaciones.
3. Implementar lógica en servicio.
4. Exponer endpoint/controlador delgado.
5. Actualizar plantilla Thymeleaf si aplica.
6. Añadir tests (servicio/controlador/seguridad según corresponda).
7. Revisar impactos en cifrado, auditoría y soft-delete.

## 9. Definition of Done (DoD) mínima por PR

Para considerar una tarea terminada:
1. Compila (`mvn test` sin fallos).
2. Tests nuevos/actualizados incluidos.
3. Sin lógica de negocio nueva en controladores.
4. Sin exposición de secretos ni degradación de permisos.
5. Documentación actualizada cuando cambie comportamiento relevante.
6. Loggin de la funcionalidad para tener trazabilidad de todas las acciones que se realizan.

## 10. Evolución recomendada a medio plazo

1. Extraer lógica de filtrado/ordenación compleja de controladores a servicios dedicados.
2. Introducir capa de mapeo explícita (mapper) cuando crezca el número de DTOs.
3. Definir métricas de cobertura mínima de tests por paquete crítico (`service`, `security`).
4. Valorar migraciones versionadas de base de datos (Flyway/Liquibase) para entornos productivos.
