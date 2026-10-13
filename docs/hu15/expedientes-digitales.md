# HU15 — Expedientes digitales

## Modelo y API

V13 crea un expediente por empleado (`employee_files.employee_id UNIQUE`). Un expediente contiene documentos y cada documento conserva sus versiones. El vínculo opcional con un contrato se acepta solo cuando ese contrato pertenece al empleado del expediente. No se eliminan filas ni objetos confirmados.

| Método | Ruta | Permiso |
| --- | --- | --- |
| POST | `/api/employee-files` | `employee_files.create` |
| GET | `/api/employee-files` | `employee_files.list` |
| GET | `/api/employee-files/{id}`, `/by-employee/{employeeId}` | `employee_files.detail` |
| PUT | `/api/employee-files/{id}` | `employee_files.update` |
| POST | `/api/employee-files/{id}/documents` | `employee_files.documents.create` |
| GET | `/api/employee-files/{id}/documents` | `employee_files.documents.list` |
| GET | `/api/employee-files/{id}/documents/{documentId}` | `employee_files.documents.detail` |
| PUT | `/api/employee-files/{id}/documents/{documentId}` | `employee_files.documents.update` |
| PATCH | `/api/employee-files/{id}/documents/{documentId}/verify` | `employee_files.documents.verify` |
| POST | `/api/employee-files/{id}/documents/{documentId}/versions` | `employee_files.documents.upload` |
| GET | `/api/employee-files/{id}/documents/{documentId}/versions` | `employee_files.documents.detail` |
| GET | `/api/employee-files/{id}/documents/{documentId}/versions/{versionId}/download` | `employee_files.documents.download` |

El permiso adicional `employee_files.documents.confidential.read` se exige para consultar o descargar documentos confidenciales y para operar sobre un documento confidencial existente. Sin él, el listado paginado excluye tales documentos también de su total. Los permisos se registran al iniciar en `public.system_permissions` mediante el bootstrap idempotente; asígnelos con `/api/access` a los roles pertinentes. No se asignan automáticamente.

`POST /documents` crea solo metadatos. `POST /versions` recibe multipart con parte `file`. Los expedientes y documentos admiten `ACTIVE` y `ARCHIVED` en PUT; archivar impide nuevas escrituras y cargas, mantiene las lecturas autorizadas y puede revertirse. La verificación recibe `{"status":"VERIFIED"}` o `{"status":"REJECTED"}` y requiere una versión. Una versión nueva o una modificación sustancial devuelve la verificación a `PENDING`.

## MinIO local

El Compose existente incluye MinIO en `127.0.0.1:9000`, consola en `127.0.0.1:9001`, volumen `minio_data` y el inicializador `minio-init`. La imagen es `ghcr.io/coollabsio/minio:RELEASE.2025-10-15T17-29-55Z@sha256:69b55a1c1c5dc285ce04db96689f5b2102317fc77a50680a1874ca6efd1c87f9`: compilación comunitaria de una versión corregida, solo para desarrollo. El inicializador crea idempotentemente el bucket privado `hrcore-documents-dev`, una identidad de aplicación distinta del administrador y una política limitada al prefijo `tenants/*`. El cliente de inicialización usa `bitnamilegacy/minio-client:2025.7.21-debian-12-r3`, fijado por digest; la imagen oficial `minio/mc` ya no permite descarga pública en este entorno.

1. Copie `.env.sample` a `.env` si aún no existe. Sustituya `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_APP_USER` y `MINIO_APP_PASSWORD` por valores locales distintos. No publique ese archivo. Defina `DOCUMENT_S3_BUCKET=hrcore-documents-dev`, `DOCUMENT_S3_REGION=us-east-1` y `DOCUMENT_MAX_BYTES=10485760`.
2. Ejecute `docker compose up -d minio minio-init`; compruebe `docker compose ps` y `docker compose logs minio-init`. El inicializador debe terminar con código cero.
3. Inicie el backend con `docker compose up -d serv-hrcore-api`. Dentro de Docker usa `http://minio:9000`. Si ejecuta Spring Boot en el host, configure `DOCUMENT_S3_ENDPOINT=http://127.0.0.1:9000` y las credenciales de la identidad de aplicación en `DOCUMENT_S3_ACCESS_KEY` y `DOCUMENT_S3_SECRET_KEY`.
4. Con un JWT tenant autorizado y su cabecera `X-Tenant-Slug`, cree un expediente y un documento; suba un PDF: `curl -H "Authorization: Bearer <JWT>" -H "X-Tenant-Slug: <slug>" -F "file=@ejemplo.pdf;type=application/pdf" http://localhost:8080/api/employee-files/<fileId>/documents/<documentId>/versions`. Repita la subida para la segunda versión. Consulte `/versions` y descargue cada `/versions/<versionId>/download`. Compare SHA-256 local con `checksumSha256` de la respuesta.
5. Compruebe el objeto en la consola local. Ejecute `docker compose stop minio` y luego `docker compose up -d minio`; las versiones deben seguir disponibles. No use `docker compose down -v`.

El límite de Spring multipart debe ser al menos el límite documental; el ejemplo usa 10 MB de archivo y 12 MB de solicitud. Se validan extensión, MIME y firma PDF/JPEG/PNG. Esta comprobación no constituye análisis antimalware.

## Amazon S3

Cree previamente un bucket privado en la región seleccionada, active Block Public Access y cifrado en reposo. Configure `DOCUMENT_S3_BUCKET`, `DOCUMENT_S3_REGION` y `DOCUMENT_MAX_BYTES`. Deje `DOCUMENT_S3_ENDPOINT`, `DOCUMENT_S3_ACCESS_KEY` y `DOCUMENT_S3_SECRET_KEY` vacíos; el SDK usará la cadena estándar de credenciales y preferiblemente un rol IAM de la carga de trabajo. No despliegue MinIO en producción.

La identidad de aplicación requiere `s3:PutObject`, `s3:GetObject` y `s3:DeleteObject` únicamente sobre `arn:aws:s3:::<bucket>/tenants/*`; `DeleteObject` se utiliza solo para compensar subidas no confirmadas. No requiere acceso público ni permiso de crear buckets. Si se utiliza SSE-KMS, conceda además los permisos KMS correspondientes. Valide en AWS real conectividad, subida, descarga, privacidad, cifrado, aislamiento entre tenants y permisos IAM. El éxito en MinIO no prueba el comportamiento de AWS.

Referencias: [Block Public Access de S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html), [cadena de credenciales del SDK Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html) y [permisos de acciones S3](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-with-s3-policy-actions.html).

Con una identidad administrativa de verificación, sustituya el nombre del bucket en estos comandos de solo lectura:

```text
aws sts get-caller-identity
aws s3api head-bucket --bucket hrcore-documents-prod
aws s3api get-public-access-block --bucket hrcore-documents-prod
aws s3api get-bucket-encryption --bucket hrcore-documents-prod
```

Después, repita la subida y descarga por la API con el rol de aplicación, compare el SHA-256 y compruebe que un rol o tenant ajeno no puede leer el documento. Verifique en IAM que la identidad de aplicación carece de `s3:ListBucket` y de permisos sobre otros buckets.

## Integridad y operación

Cada versión usa una clave nueva con tenant, empleado, documento y UUID de versión. Las escrituras bloquean el expediente para coordinarse con su archivado; el bloqueo del documento serializa el número de versión y PostgreSQL conserva la restricción única. El archivo se sube antes de confirmar la fila y la auditoría. Ante rollback se intenta borrar **solo** el objeto nuevo. La descarga recupera el objeto a un archivo temporal acotado, comprueba tamaño y SHA-256, lo transmite con cabeceras privadas y elimina el temporal al finalizar.

Una caída del proceso entre S3 y el commit puede dejar un objeto huérfano porque V13 no tiene estado pendiente ni cola durable. Ante un incidente, compare de forma controlada las claves del prefijo tenant con `employee_document_versions.storage_key` en ese esquema, documente las diferencias y elimine únicamente objetos confirmados como huérfanos después de revisión. Nunca borre versiones registradas. Para garantizar recuperación automática se necesitaría una nueva migración autorizada.

No existe integración antimalware en el proyecto. Para producción se debe definir un control de análisis/cuarentena antes de tratar los archivos como seguros. Registre errores de almacenamiento sin contenido ni claves privadas y revise los eventos de auditoría de escritura, lectura confidencial y descarga preparada.


## Verificación efectuada

- `mvn -q '-DskipTests' package`: correcto.
- Pruebas unitarias de HU15: 15 ejecutadas, sin fallos. Cubren duplicado, archivado, pertenencia de contrato y documento, permiso confidencial, no-ops, paginación delegada, filtro confidencial, autorización de métodos, firma inválida, descarga y compensación de rollback.
- Prueba del adaptador contra MinIO aislado: 1 ejecutada, sin fallos ni omisiones; subió, recuperó y retiró un PDF temporal, y rechazó una descarga superior al límite configurado.
- Inicializador MinIO ejecutado dos veces sobre el mismo volumen: ambas terminaron con código cero. La segunda ejecución reutilizó bucket, usuario y política.
- Persistencia del volumen: se subió un objeto temporal, se detuvo y reinició MinIO, se recuperó el mismo contenido y se retiró el objeto temporal. Los contenedores de prueba quedaron detenidos y el volumen de prueba se conservó.
- `docker compose config --quiet` y `git diff --check`: correctos. La configuración local requiere añadir las nuevas variables MinIO al `.env` existente.
- La suite Maven completa se interrumpió antes de permitir conexiones de la prueba de contexto con perfil `dev`, pues no se ha confirmado que su PostgreSQL sea aislado. Quedan pendientes pruebas reales de JPA, índices, transacciones completas, concurrencia y aislamiento entre esquemas con una base autorizada; el circuito completo roles → JWT → endpoint y AWS real también requieren sus respectivos entornos.

## Archivos nuevos

```text
hrcore-api/src/main/java/com/hrcoresystem/hrcore_api/modules/tenant/employeefiles/
├── domain/
│   ├── model/
│   │   ├── DocumentType.java, FileStatus.java, VerificationStatus.java
│   │   ├── EmployeeFile.java, EmployeeDocument.java, EmployeeDocumentVersion.java
│   │   └── EmployeeFileFilter.java, EmployeeDocumentFilter.java
│   └── repository/
│       ├── EmployeeFileRepository.java, EmployeeDocumentRepository.java
│       ├── EmployeeDocumentVersionRepository.java
│       └── DocumentStoragePort.java
├── application/
│   ├── dto/
│   │   ├── CreateFileRequest.java, UpdateFileRequest.java, FileResponse.java
│   │   ├── CreateDocumentRequest.java, UpdateDocumentRequest.java, DocumentResponse.java
│   │   └── VersionResponse.java, VerificationRequest.java
│   └── service/
│       ├── EmployeeFilePolicy.java, EmployeeFileManagementService.java
│       ├── EmployeeDocumentManagementService.java
│       └── DocumentVersionService.java
└── infrastructure/
    ├── api/EmployeeFileController.java
    ├── persistence/
    │   ├── EmployeeFileEntity.java, EmployeeFileJpaRepository.java, EmployeeFileRepositoryAdapter.java
    │   ├── EmployeeDocumentEntity.java, EmployeeDocumentJpaRepository.java, EmployeeDocumentRepositoryAdapter.java
    │   └── EmployeeDocumentVersionEntity.java, EmployeeDocumentVersionJpaRepository.java, EmployeeDocumentVersionRepositoryAdapter.java
    └── storage/
        ├── DocumentStorageProperties.java, DocumentStorageConfiguration.java
        └── S3DocumentStorageAdapter.java
hrcore-api/src/test/java/com/hrcoresystem/hrcore_api/modules/tenant/employeefiles/
├── EmployeeFileManagementServiceTest.java
├── EmployeeDocumentManagementServiceTest.java
├── EmployeeFilePolicyTest.java
├── DocumentVersionServiceTest.java
├── EmployeeFileControllerPermissionsTest.java
├── EmployeeFileMethodSecurityTest.java
└── S3DocumentStorageAdapterIntegrationTest.java
docker/
├── minio-app-policy.json
└── minio-init.sh
docs/hu15/
└── expedientes-digitales.md
```

Fuera del módulo se modificaron solo `PlatformBootstrapService` (12 permisos), `pom.xml` (AWS SDK v2), `application.yml`, `application-dev.yml`, `.env.sample` y `docker-compose.yml` (configuración local y del almacenamiento). No se cambiaron migraciones ni otros módulos funcionales.


