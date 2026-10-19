# HRCore AI Reports — Fase 1

## Implementado

- Catálogo semántico v1 declarado en el servidor para `employees.roster` y `attendance.daily`. Registra dimensiones, tipos, operadores, métricas, grano y relaciones temporales descriptivas. No contiene expresiones SQL.
- `QueryPlanV1` y validador estricto del contrato JSON: claves desconocidas, propiedades obligatorias, tipos, valores, fechas, límites, dataset, modo, agrupación, filtros, `HAVING`, orden y visualización. El rango de asistencia es `[from, toExclusive)` y no supera 366 días.
- Política independiente que calcula la unión de autoridades para todas las referencias de un plan validado, incluidas claves no visibles. Usa el principal autenticado, sus autoridades efectivas y el contexto tenant de Spring. También produce un catálogo de capacidades filtrado para uso interno futuro.
- Errores internos con códigos estables `INVALID_QUERY_PLAN`, `UNSUPPORTED_QUERY`, `QUERY_NOT_ALLOWED`, `QUERY_TOO_COMPLEX` y `CATALOG_VERSION_MISMATCH`, con mensajes que no enumeran campos restringidos.
- Pruebas unitarias con JSON y principales ficticios. No se consultan empleados ni bases de datos.

## Límites deliberados

- `employee.employment_status_at_date` figura en el catálogo pero está inhabilitado: el estado actual del período laboral no demuestra el estado en cualquier fecha histórica.
- Las etiquetas de departamento y cargo son nombres actuales de entidades asociadas por vigencia; no se afirma que sean nombres históricos.
- La política solo admite acceso con las autoridades generales de reportes y dominio de la matriz v1. Los permisos de equipo, autoservicio o departamento no amplían el alcance sin una restricción de filas verificada.
- No hay endpoint HTTP, SQL, acceso a datos, migraciones, interfaz, chat ni integración con FastAPI o Gemini. Los errores aún no se asignan al formato HTTP; eso corresponde al futuro adaptador.
- La fase 1 no prueba aislamiento entre esquemas tenant ni la exactitud de relaciones y métricas sobre datos reales.

## Decisiones para Fase 2

1. Especificar y probar el compilador SQL con parámetros enlazados, relaciones temporales y tratamiento de empleados sin asignación primaria.
2. Definir la fuente histórica fiable para `employee.employment_status_at_date` y si debe publicarse.
3. Probar aislamiento entre tenants con bases separadas, rol de lectura mínimo, transacciones `READ ONLY` y `statement_timeout`.
4. Definir y probar restricciones por fila antes de habilitar alcances propios, de supervisor o departamento.
5. Resolver etiquetas históricas, supresión de agregados pequeños y límites de carga antes de usar datos reales.
