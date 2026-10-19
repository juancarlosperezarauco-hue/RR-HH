# HRCore AI Reports: Fase 2, motor interno inicial

`AnalyticQueryService.execute(String)` acepta exclusivamente QueryPlan v1. En cada llamada
valida el plan, calcula las autoridades efectivas de Spring Security, verifica el contexto
tenant y su nombre de esquema, compila SQL con identificadores del servidor y ejecuta
una página de solo lectura. No se añadió endpoint ni se modificaron los reportes fijos.

Están habilitados `attendance.daily` y `employees.roster` en detalle y agregado. La
asistencia se vincula al período laboral del registro y a la asignación primaria activa
vigente en `work_date`; el roster usa `asOfDate`. Las asignaciones ausentes conservan
al empleado con departamento y cargo nulos. Los nombres de departamento y cargo son
las etiquetas actuales. El estado laboral histórico sigue deshabilitado en el catálogo.

La transacción de ejecución es nueva, de solo lectura y tiene límite de 3 segundos.
PostgreSQL recibe también `SET TRANSACTION READ ONLY` y `statement_timeout` de 3 segundos.
El tamaño máximo de página es 100 y se lee una fila adicional para `hasMore`.
El resultado incluye columnas tipadas, filas, página y advertencias cuando alguna fila
de la página carece de asignación primaria, incluso si el plan no selecciona columnas
organizativas. Los fallos internos de ejecución y timeout
se comunican mediante códigos, sin incluir SQL.

La integración usa un PostgreSQL 16 desechable distinto de la base activa del proyecto.
Migra dos esquemas nuevos, carga datos ficticios y elimina los esquemas al terminar.
La credencial común de base de datos tiene acceso técnico a ambos esquemas; el
aislamiento probado corresponde a la ruta `TenantAwareDataSource` de Spring.

## Línea base de deuda técnica de asistencia

Antes de esta fase había 11 resultados fallidos en pruebas adicionales de asistencia:
7 errores en `AttendanceCalculationServiceTest`, 1 fallo y 1 error en
`AttendanceOvertimeServiceTest`, y 2 errores en `AttendancePunchServiceTest`.
Estos casos no se corrigen en Fase 2. Sus resultados se comparan por caso al ejecutar
la regresión.

## Pendiente

No hay interfaz HTTP, FastAPI, Gemini ni Angular. La orquestación y los contratos de
exposición quedan para fases posteriores. La base de datos debe mantener las
restricciones existentes que evitan solapamientos de períodos y asignaciones primarias.
