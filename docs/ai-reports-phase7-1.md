# HRCore AI Reports — Fase 7.1: voz a texto

## Arquitectura y contrato

El chat de Angular captura audio con `getUserMedia` y `MediaRecorder`. Spring Boot recibe un único archivo en `POST /api/hr/ai/transcriptions`, verifica la sesión tenant y el acceso a AI Reports, inspecciona el audio y lo envía directamente a `https://api.groq.com/openai/v1/audio/transcriptions`. La respuesta `ApiResponse<{text:string}>` se añade al borrador editable de la conversación seleccionada. El usuario debe pulsar «Enviar»; solo entonces el texto entra en el flujo actual de AI Reports con Gemini. Ni FastAPI ni Gemini reciben la grabación. No se crean filas de audio o transcripción en PostgreSQL ni objetos en MinIO.

`GET /api/hr/ai/transcriptions/config` devuelve `{enabled,maxDurationSeconds,maxBytes}` sin secretos. Ambos endpoints requieren el mismo JWT, tenant y permisos de entrada que el chat, más acceso efectivo al catálogo. La respuesta lleva `Cache-Control: no-store`.

La implementación reside en `hrcore-api` bajo `modules/tenant/reports/application/voice` y `infrastructure/api`, en el componente de chat Angular y `AiVoiceService` HTTP. No cambia QueryPlan, permisos existentes, historial, exportaciones ni reportes operativos.

## Configuración

Voz está apagada por defecto. En Docker Compose, configure `AI_VOICE_ENABLED=true` y `GROQ_API_KEY` en el entorno privado del servicio API. El navegador nunca recibe la clave. Para una cuenta Free, entre en [GroqCloud API Keys](https://console.groq.com/keys), cree manualmente una clave y consérvela fuera de Git. No se configura pago ni upgrade automático. En AWS, inyecte la misma configuración como secretos y variables del contenedor API; no la ponga en la imagen ni en Angular. La imagen API necesita `ffprobe` (en `Dockerfile.dev` se instala con el paquete `ffmpeg`), pero no aloja modelos de voz.

| Variable | Valor inicial | Uso |
| --- | --- | --- |
| `AI_VOICE_ENABLED` | `false` | Habilitación explícita |
| `GROQ_API_KEY` | vacío | Clave privada; vacío deshabilita voz |
| `GROQ_STT_MODEL` | `whisper-large-v3-turbo` | Modelo de Groq |
| `AI_VOICE_MAX_DURATION_SECONDS` | `30` | Límite en cliente y servidor |
| `AI_VOICE_MAX_BYTES` | `8388608` | Máximo 8 MiB; Spring tiene además su límite multipart global |
| `AI_VOICE_USER_RPM` / `AI_VOICE_TENANT_RPM` / `AI_VOICE_GLOBAL_RPM` | `5` / `10` / `15` | Solicitudes por minuto por instancia |
| `AI_VOICE_USER_CONCURRENT` / `AI_VOICE_TENANT_CONCURRENT` / `AI_VOICE_MAX_CONCURRENT` | `1` / `2` / `2` | Transcripciones simultáneas por instancia |
| `AI_VOICE_PROBE_TIMEOUT_SECONDS` / `AI_VOICE_PROVIDER_TIMEOUT_SECONDS` | `3` / `30` | Tiempo máximo de inspección y de llamada al proveedor |
| `AI_VOICE_FFPROBE_PATH` | `ffprobe` | Ejecutable local de inspección |

Los contadores son locales a cada JVM. Antes de desplegar varias instancias, aplique los límites agregados en un gateway o almacén compartido; de otro modo el tráfico total puede multiplicar estos valores. La [cuota Free de Groq](https://console.groq.com/docs/rate-limits) es por organización y también contempla segundos de audio; un 429 se muestra como cuota agotada y no se reintenta automáticamente.

## Navegadores, validación y errores

La elección de MIME usa `MediaRecorder.isTypeSupported`. Se prueban WebM/Opus, Ogg/Opus y MP4/AAC, correspondientes a los formatos de grabación disponibles en versiones compatibles de Chrome, Firefox, Safari, Android e iOS. Si el navegador no ofrece uno de ellos o no permite micrófono, el chat mantiene el teclado y muestra el estado correspondiente. La captura requiere contexto seguro HTTPS o localhost. El servidor comprueba firma, contenedor, pista de audio, codec y duración con `ffprobe`, sin confiar en el nombre del archivo. El archivo temporal se elimina al terminar o fallar, y los tracks y blobs se liberan en el navegador.

Las respuestas de error son `{success:false,code,message,errors,timestamp}`. Audio inválido devuelve 400, tamaño excesivo 413, duración excesiva 422, límite local o cuota Groq 429, servicio desactivado o credenciales del proveedor rechazadas 503, respuesta de proveedor inválida o 5xx 502 y timeout 504. JWT y permisos mantienen 401/403. No se registran audio, claves ni transcripciones. Una transcripción de más de 2000 caracteres se rechaza para conservar el límite del chat.

## Privacidad y prueba posterior

El aviso junto al micrófono informa que el audio sale a GroqCloud. Según [Groq, los datos de inferencia no se conservan por defecto](https://console.groq.com/docs/your-data), salvo registros temporales para fiabilidad o abuso de hasta 30 días; Groq ofrece Zero Data Retention en Data Controls. Los metadatos de uso siguen reteniéndose y Groq indica almacenamiento de datos de cliente en Estados Unidos cuando corresponda. Revise estas condiciones y los requisitos de privacidad del tenant antes de habilitar datos reales. Para pruebas, use audio sintético o voluntario sin datos personales sensibles.

Las pruebas automáticas usan mocks y no llaman a Groq. Cuando se autoricen llamadas reales, configure una clave Free, habilite `AI_VOICE_ENABLED`, pruebe en HTTPS con una cuenta tenant autorizada y un audio no sensible de menos de 30 segundos, confirme que el texto aparece editable, corrija el borrador y envíelo manualmente. Verifique también un 429 o indisponibilidad sin reintento ni envío automático. Hasta esa autorización, la integración permanece pendiente de validación en vivo.

## Verificación realizada y pendiente

- Spring: 57 pruebas focalizadas de AI Reports y voz correctas, incluidas respuestas del proveedor simulado, timeout, formato, duración, tamaño, permisos de servicio y limpieza de temporales.
- Integración HTTP: 12 pruebas correctas con PostgreSQL y planner mock desechables; incluyen autenticación, permisos, aislamiento tenant, cinco reportes legacy y la ruta de voz con `TranscriptionProvider` simulado. Los contenedores desechables se detuvieron al concluir.
- Angular: 109 pruebas de regresión correctas en ChromeHeadless 155 y cinco pruebas adicionales de voz correctas tras añadir cobertura MP4/cambio de tenant. Builds de desarrollo y producción correctos; el build de producción mantiene un aviso de presupuesto de bundle inicial.
- Imagen API: build Docker correcto y prueba con audio sintético WebM/Opus, Ogg/Opus y MP4/AAC correcta. La prueba detectó una opción incompatible de `ffprobe` y se corrigió la invocación; la imagen temporal se eliminó.
- `git diff --check`: correcto. La ejecución indiscriminada de la suite Spring (283 pruebas) falló en 15 aserciones y 17 errores de módulos ajenos, además de la ausencia de PostgreSQL en el perfil `dev` y el bloqueo de loopback del sandbox; 25 pruebas quedaron omitidas. Las pruebas de AI Reports que requieren loopback se repitieron fuera del sandbox y pasaron.
- Pendiente: prueba manual en Chrome, Firefox, Safari, Android e iOS reales; validación real con Groq solo después de autorización expresa. Los límites por proceso necesitan un control compartido antes de escalar a múltiples instancias.
