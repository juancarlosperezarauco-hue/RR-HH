export interface ManualTopic {
  title: string;
  paragraphs: string[];
  bullets?: string[];
}

export interface ManualSection {
  id: string;
  label: string;
  eyebrow: string;
  title: string;
  summary: string;
  topics: ManualTopic[];
}

export const MANUAL_SECTIONS: ManualSection[] = [
  {
    id: 'inicio',
    label: 'Primeros pasos',
    eyebrow: '01 · Puesta en marcha',
    title: 'Configura tu espacio de trabajo',
    summary: 'El recorrido recomendado para entrar a HR Core, completar la empresa y dejar lista la base de tu operación.',
    topics: [
      {
        title: '1. Activa tu cuenta',
        paragraphs: ['Después del registro, revisa el correo de activación y confirma tu cuenta. Si no lo encuentras, revisa spam o solicita el reenvío desde la pantalla de acceso.', 'Inicia sesión con tu correo, contraseña y el identificador de la empresa cuando la aplicación lo solicite.'],
        bullets: ['Una cuenta pendiente de verificación no puede utilizar todas las funciones.', 'Usa “Recuperar contraseña” para generar un enlace nuevo cuando sea necesario.']
      },
      {
        title: '2. Completa la configuración de la empresa',
        paragraphs: ['La pantalla de configuración inicial solicita la identidad de la empresa, país, moneda y zona horaria. Estos datos sirven como referencia para la operación y los registros de tiempo.', 'Completa el asistente con información real y finaliza el proceso. El estado de configuración se guarda en el tenant y determina cuándo el espacio está listo para operar.']
      },
      {
        title: '3. Orden recomendado',
        paragraphs: ['Una vez dentro, configura primero la estructura organizativa y después registra a las personas. Luego define jornadas, contratos, permisos y reglas de acceso.',
        ],
        bullets: ['Empresa y preferencias', 'Departamentos y cargos', 'Empleados y asignaciones', 'Usuarios, roles y permisos', 'Turnos, asistencia, ausencias e incidencias']
      }
    ]
  },
  {
    id: 'acceso',
    label: 'Usuarios y acceso',
    eyebrow: '02 · Seguridad',
    title: 'Administra usuarios, roles y permisos',
    summary: 'Controla quién puede entrar al tenant y qué operaciones puede realizar cada persona.',
    topics: [
      {
        title: 'Usuarios',
        paragraphs: ['Desde Usuarios puedes crear cuentas, consultar su estado, editar sus datos y activar o desactivar el acceso. Una cuenta puede estar pendiente de verificación, activa o inactiva.', 'Para que una persona trabaje en la aplicación debe tener una cuenta verificada y, cuando corresponda, estar vinculada a un empleado.']
      },
      {
        title: 'Roles',
        paragraphs: ['Un rol reúne permisos con un propósito operativo. El rol inicial del tenant es el administrador propietario; a partir de ahí el owner puede crear roles propios para limitar el acceso por responsabilidad.', 'Asigna únicamente los permisos necesarios para cada función. Los permisos controlan la lectura, creación, edición, activación y acciones especiales de cada módulo.'],
        bullets: ['Crea el rol con un nombre claro.', 'Selecciona los permisos que realmente necesita.', 'Asigna el rol a los usuarios que correspondan.', 'Revisa el acceso después de un cambio de responsabilidad.']
      },
      {
        title: 'Vincular una cuenta con un empleado',
        paragraphs: ['La vinculación relaciona la identidad de acceso con el registro laboral. Busca una cuenta activa o pendiente de verificación desde el formulario del empleado y confirma la asociación.', 'Si el backend rechaza una cuenta inactiva, primero actívala desde Usuarios. La verificación del correo sigue siendo necesaria para iniciar sesión.']
      },
      {
        title: 'Sesiones y seguridad',
        paragraphs: ['En Seguridad puedes consultar sesiones abiertas, cerrar una sesión concreta o revocar las demás. Desde tu perfil también puedes cambiar la contraseña y actualizar tus datos personales.', 'Cierra sesiones desconocidas y no compartas contraseñas. La autenticación de plataforma y la de tenant son accesos separados.']
      }
    ]
  },
  {
    id: 'organizacion',
    label: 'Organización',
    eyebrow: '03 · Estructura laboral',
    title: 'Construye la estructura de tu empresa',
    summary: 'Define dónde trabaja cada persona, qué cargo ocupa y cómo se representa la jerarquía.',
    topics: [
      {
        title: 'Departamentos',
        paragraphs: ['Crea departamentos con nombre, descripción y, si aplica, un departamento padre. La estructura permite agrupar personas y construir el organigrama.', 'Activa o desactiva departamentos cuando cambie la estructura. Evita duplicar nombres para conservar una lectura clara.']
      },
      {
        title: 'Cargos',
        paragraphs: ['Registra el nombre del cargo, su propósito, responsabilidades, nivel organizacional y rango salarial opcional. El código del cargo se genera en el backend; no es necesario inventarlo manualmente.', 'Los requisitos del cargo pueden documentar experiencia, formación u otras condiciones esperadas.']
      },
      {
        title: 'Empleados y asignaciones',
        paragraphs: ['Registra la identidad del empleado y su fecha de ingreso. Después crea la asignación vigente indicando departamento, cargo, responsable y fecha de inicio.', 'Una persona puede tener historial de asignaciones. Para una transferencia, cierra la asignación anterior y registra la nueva; no superpongas períodos laborales.'],
        bullets: ['La fecha de nacimiento debe corresponder a una persona adulta.', 'La nacionalidad se selecciona del catálogo disponible.', 'El género utiliza las opciones definidas por el sistema.', 'La recontratación debe comenzar después del fin del período laboral anterior.']
      },
      {
        title: 'Organigrama y equipo',
        paragraphs: ['Organigrama muestra la jerarquía de departamentos, cargos y responsables. Mi equipo ofrece la vista operativa de las personas que dependen de tu alcance.', 'Si no aparecen nodos, revisa que existan departamentos activos y asignaciones vigentes.']
      }
    ]
  },
  {
    id: 'expedientes',
    label: 'Contratos y expedientes',
    eyebrow: '04 · Información laboral',
    title: 'Mantén la documentación organizada',
    summary: 'Centraliza contratos, períodos laborales y documentos asociados a cada empleado.',
    topics: [
      {
        title: 'Expedientes',
        paragraphs: ['Cada empleado puede tener un expediente con documentos clasificados. Desde Expedientes crea documentos, consulta sus versiones y descarga los archivos autorizados.', 'Carga archivos en formatos admitidos por la aplicación y agrega una descripción que facilite encontrarlos. La verificación de un documento queda registrada para el control posterior.']
      },
      {
        title: 'Contratos',
        paragraphs: ['Crea contratos asociados a un empleado y a su período laboral. Gestiona su estado, consulta la historia, renueva cuando corresponda y genera el documento PDF desde el detalle.', 'Antes de crear o renovar un contrato confirma que el período laboral y la asignación sean los correctos.']
      },
      {
        title: 'Contactos de emergencia',
        paragraphs: ['En el detalle del empleado registra uno o más contactos de emergencia, mantén sus datos actualizados y elimina los que ya no correspondan.']
      }
    ]
  },
  {
    id: 'jornada',
    label: 'Jornada y asistencia',
    eyebrow: '05 · Tiempo trabajado',
    title: 'Configura turnos y registra asistencia',
    summary: 'Define calendarios de trabajo, asigna jornadas y controla entradas, salidas, horas extra y ajustes.',
    topics: [
      {
        title: 'Turnos y descansos',
        paragraphs: ['En Turnos crea la jornada con sus horarios, tolerancias y descansos. La tolerancia de entrada permite considerar un margen al inicio; la de salida define el margen asociado al cierre.', 'Asigna un turno a empleados o a grupos según el período correspondiente. Las excepciones de calendario sirven para feriados, cierres o días especiales.']
      },
      {
        title: 'Asistencia',
        paragraphs: ['El registro normal de asistencia captura entradas y salidas. Los registros históricos requieren una nota que explique por qué se está cargando información de una fecha anterior.', 'Consulta los registros y utiliza el cálculo o procesamiento de rango cuando necesites consolidar resultados.']
      },
      {
        title: 'Ajustes y horas extra',
        paragraphs: ['Los ajustes corrigen registros de asistencia y pasan por un flujo de aprobación. Las horas extra también se registran y deben aprobarse antes de considerarse válidas.', 'Conserva una justificación clara en cada corrección para mantener la trazabilidad.']
      }
    ]
  },
  {
    id: 'ausencias',
    label: 'Ausencias',
    eyebrow: '06 · Permisos y licencias',
    title: 'Gestiona saldos y solicitudes',
    summary: 'Administra tipos de ausencia, saldos por período y el flujo de aprobación de solicitudes.',
    topics: [
      {
        title: 'Tipos y saldos',
        paragraphs: ['Crea los tipos de ausencia que utiliza la empresa y define los saldos por empleado, año y período laboral. Ajusta un saldo solo cuando exista una razón documentada.', 'Selecciona el período laboral correcto para evitar que el saldo quede asociado a una relación anterior.']
      },
      {
        title: 'Solicitudes',
        paragraphs: ['Una solicitud se crea con el tipo, rango de fechas y motivo. El flujo habitual es: borrador, envío, revisión, aprobación o rechazo; mientras no se envíe puede editarse.', 'Adjunta evidencia cuando el caso lo requiera. Las solicitudes históricas o muy extensas deben revisarse antes de enviarse.']
      },
      {
        title: 'Vista del empleado',
        paragraphs: ['Cada empleado puede consultar sus saldos y sus propias solicitudes desde el espacio personal, respetando el alcance y permisos asignados.']
      }
    ]
  },
  {
    id: 'incidencias',
    label: 'Incidencias',
    eyebrow: '07 · Seguimiento',
    title: 'Registra y resuelve incidencias',
    summary: 'Documenta situaciones laborales, asigna responsables y conserva la evidencia del proceso.',
    topics: [
      {
        title: 'Crear una incidencia',
        paragraphs: ['Registra el empleado, tipo, gravedad, título, descripción, fecha y lugar. Escribe hechos verificables y evita colocar información irrelevante.', 'Adjunta evidencias desde el detalle, indicando el tipo y una descripción del archivo.']
      },
      {
        title: 'Flujo de atención',
        paragraphs: ['Una incidencia puede asignarse a un responsable, pasar a revisión, recibir la respuesta del empleado, resolverse, cerrarse o cancelarse según el caso.', 'Las acciones asociadas permiten planificar medidas, iniciarlas, completarlas o cancelarlas. Cada transición conserva el historial.']
      },
      {
        title: 'Buenas prácticas',
        paragraphs: ['Mantén las notas profesionales, usa la gravedad con criterio y evita cerrar una incidencia mientras existan acciones pendientes o información por revisar.']
      }
    ]
  },
  {
    id: 'reportes',
    label: 'Reportes',
    eyebrow: '08 · Análisis',
    title: 'Consulta indicadores de RR. HH.',
    summary: 'Obtén una lectura resumida de empleados, contratos, asistencia, ausencias e incidencias.',
    topics: [
      {
        title: 'Reportes disponibles',
        paragraphs: ['La sección de reportes ofrece vistas de empleados, contratos, asistencia, permisos e incidencias. Cada reporte permite aplicar filtros y consultar métricas agrupadas.', 'Los datos respetan el período y el alcance autorizado del usuario.']
      },
      {
        title: 'Exportación',
        paragraphs: ['Cuando tu rol tenga permiso de exportación, descarga el reporte seleccionado en CSV para continuar el análisis. Revisa el filtro aplicado antes de compartir el archivo.']
      }
    ]
  },
  {
    id: 'suscripcion',
    label: 'Plan y suscripción',
    eyebrow: '09 · Cuenta del tenant',
    title: 'Consulta el plan de tu organización',
    summary: 'Revisa los límites, el estado de la suscripción y las opciones de facturación disponibles.',
    topics: [
      {
        title: 'Estado y límites',
        paragraphs: ['Suscripción muestra el plan actual, su estado, fechas relevantes y los límites de usuarios o roles. El plan DEMO sirve para probar el flujo sin activar un cobro.', 'Los límites del plan pueden impedir crear más usuarios o roles hasta que se libere capacidad o se actualice la suscripción.']
      },
      {
        title: 'Planes de pago',
        paragraphs: ['Los planes de pago se contratan mediante el checkout seguro. Después de completar el pago, vuelve a la aplicación para consultar el resultado y la suscripción provisionada.', 'El portal del cliente permite gestionar la información de facturación cuando está disponible para la cuenta.']
      }
    ]
  },
  {
    id: 'notificaciones',
    label: 'Notificaciones y respaldos',
    eyebrow: '10 · Operación',
    title: 'Mantén el control de la actividad',
    summary: 'Consulta avisos, preferencias y respaldos sin perder el contexto de tu organización.',
    topics: [
      {
        title: 'Notificaciones',
        paragraphs: ['Notificaciones reúne avisos generados por la plataforma. Puedes filtrar las no leídas, abrir el detalle, marcar avisos como leídos o archivarlos.', 'Las preferencias determinan qué canales y tipos de aviso recibe tu cuenta. Los dispositivos de notificación se registran y pueden desactivarse.']
      },
      {
        title: 'Respaldos',
        paragraphs: ['Respaldos permite consultar y descargar copias generadas para el tenant cuando tu rol tiene el permiso correspondiente. Las restauraciones son operaciones sensibles: verifica el archivo y confirma el alcance antes de ejecutarlas.', 'No elimines un respaldo hasta validar que existe otra copia disponible y que la política de conservación lo permite.']
      },
      {
        title: 'Auditoría',
        paragraphs: ['El historial de auditoría ayuda a revisar acciones relevantes, quién las realizó y cuándo ocurrieron. Úsalo para investigar cambios de acceso, configuraciones o información laboral.']
      }
    ]
  },
  {
    id: 'plataforma',
    label: 'Administración de plataforma',
    eyebrow: '11 · Superadmin',
    title: 'Opera la plataforma HR Core',
    summary: 'Funciones reservadas para el equipo de plataforma: planes, tenants, suscripciones, auditoría y respaldos globales.',
    topics: [
      {
        title: 'Acceso de plataforma',
        paragraphs: ['El acceso de Superadmin utiliza una entrada independiente de la aplicación tenant. Inicia sesión desde el acceso de plataforma y protege esta cuenta con una contraseña exclusiva.', 'La sesión de plataforma permite consultar la actividad y administrar recursos globales sin cambiar el contexto de un tenant.']
      },
      {
        title: 'Planes y suscripciones',
        paragraphs: ['En Planes administra el catálogo, límites, precios, visibilidad y estado de cada plan. En Suscripciones consulta las asignaciones por tenant y realiza las acciones permitidas por la operación.', 'El plan DEMO es el punto de entrada para pruebas; los planes de pago dependen de la configuración de checkout y del estado informado por Stripe.']
      },
      {
        title: 'Tenants y soporte operativo',
        paragraphs: ['La administración de tenants permite consultar organizaciones, revisar su estado y activar o desactivar cuando exista una razón operativa. Antes de intervenir, verifica el tenant y documenta el motivo.', 'Los respaldos globales y las restauraciones son operaciones sensibles. Utiliza los controles de verificación y conserva una copia antes de restaurar.']
      },
      {
        title: 'Auditoría y seguridad',
        paragraphs: ['La auditoría de plataforma ayuda a revisar cambios de planes, tenants, suscripciones y acciones administrativas. Revisa las sesiones activas y revoca accesos que no reconozcas.', 'No compartas credenciales de plataforma ni ejecutes restauraciones sin confirmar el destino y el archivo.']
      }
    ]
  },
  {
    id: 'ayuda',
    label: 'Ayuda y solución',
    eyebrow: '12 · Resolución rápida',
    title: 'Qué revisar cuando algo no funciona',
    summary: 'Una lista breve para diagnosticar los problemas más comunes antes de solicitar soporte.',
    topics: [
      {
        title: 'No puedo iniciar sesión',
        paragraphs: ['Confirma el correo, la contraseña y el tenant. Si la cuenta está pendiente, activa el correo; si está inactiva, un administrador debe habilitarla.', 'Si el enlace expiró, solicita un nuevo correo de activación o restablece la contraseña.']
      },
      {
        title: 'No veo una opción o acción',
        paragraphs: ['La navegación y las acciones dependen del rol y de los permisos. Solicita al owner que revise el rol asignado y que la cuenta esté activa.', 'Un permiso de lectura no siempre permite crear, editar, aprobar, exportar o eliminar.']
      },
      {
        title: 'Un registro no aparece',
        paragraphs: ['Revisa filtros, estado, período laboral, departamento y paginación. Para organigramas y equipos, confirma que existan asignaciones vigentes.', 'Si una operación devuelve un conflicto, corrige la fecha o el estado indicado por el mensaje antes de reintentar.']
      },
      {
        title: 'Cuándo solicitar soporte',
        paragraphs: ['Incluye la pantalla, la acción realizada, la hora aproximada, el mensaje completo y el identificador del registro. No compartas contraseñas, tokens ni archivos sensibles.']
      }
    ]
  }
];
