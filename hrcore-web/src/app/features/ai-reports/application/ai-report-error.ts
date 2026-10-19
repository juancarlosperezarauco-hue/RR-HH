import { HttpErrorResponse } from '@angular/common/http';

export function aiReportError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const messages: Record<number, string> = {
      0: 'No se pudo confirmar la operación. Comprueba la conexión y reintenta de forma segura.',
      401: 'Tu sesión no está disponible. Vuelve a iniciar sesión.',
      403: 'No tienes permisos para esta operación.',
      404: 'La conversación o el reporte ya no está disponible.',
      409: 'Hay un turno pendiente o el reporte ya no es compatible. Recarga el historial.',
      429: 'El servicio alcanzó su límite temporal. Inténtalo más tarde.',
      502: 'El planificador no devolvió una respuesta válida.',
      503: 'El servicio de reportes no está disponible temporalmente.',
      504: 'La consulta superó el tiempo de espera. Puedes reintentar la misma solicitud.'
    };
    const body: unknown = error.error;
    if (error.status !== 0 && body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') return body.message;
    return messages[error.status] || 'No se pudo completar la operación.';
  }
  return error instanceof Error ? error.message : 'No se pudo completar la operación.';
}
