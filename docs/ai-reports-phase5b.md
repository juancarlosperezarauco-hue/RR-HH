# HRCore AI Reports — Subfase 5B

## Conversaciones y turnos

`POST /api/hr/ai/conversations/{id}/messages` acepta `clientMessageId` y `message`. El largo máximo se configura con `AI_CONVERSATION_MESSAGE_MAX_LENGTH` (por defecto 4.000 y nunca mayor que la columna de V25). Las preguntas se planifican de forma independiente; esta subfase no envía historial ni planes anteriores a FastAPI/Gemini.

La pregunta se guarda como `PENDING` antes de la llamada externa. Si la planificación y consulta terminan, se guardan la respuesta, un plan que pasó por el motor existente y metadatos acotados. Las filas de resultados solo se devuelven en la petición actual; no se guardan. Una repetición con el mismo identificador y contenido devuelve el resultado persistido sin planificar otra vez, pero no reproduce filas. Un identificador repetido con otro contenido responde `409`.

La reserva incrementa la versión optimista de la conversación y verifica otros mensajes pendientes en una transacción breve. No se conserva ninguna transacción mientras se espera al planificador ni al motor SQL. Mensajes `PENDING` anteriores al plazo configurable `AI_CONVERSATION_PENDING_TIMEOUT` (por defecto `PT2M`) se marcan como fallidos y reciben una respuesta de error segura al procesar una petición posterior.

V26 añade `in_reply_to_message_id` a `ai_messages`, con una FK compuesta al mensaje de usuario y unicidad por respuesta. V25 no relacionaba de forma durable la respuesta con la solicitud, requisito para reintentos y paginación consistentes. V25 permanece sin cambios.

## Actualización de reportes

`POST /api/hr/ai/conversations/{id}/reports/{reportRunId}/refresh` reejecuta un plan guardado. Requiere propiedad, acceso analítico vigente, versión de plan/catálogo compatible y la autorización completa de `AnalyticQueryService`. No llama a FastAPI/Gemini. Cada actualización exitosa crea otra fila `ai_report_runs`, ligada a la pregunta original; devuelve sus filas actuales únicamente en la respuesta.

Crear, listar, leer, borrar o consultar historial no llama a IA. El propietario tenant puede borrar su conversación aunque ya no tenga permisos analíticos; no obtiene por ello acceso al historial ni a reportes.

## Límites

5B no interpreta seguimientos ni proporciona memoria a FastAPI/Gemini; corresponde a 5C. Las llamadas de IA siguen el modo configurado de Fase 4. Los límites de concurrencia y recuperación de pendientes operan sobre PostgreSQL por tenant y son compartidos entre instancias mediante versionado y estado persistido.
