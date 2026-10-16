import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extrae y formatea mensajes de error HTTP amigables para el usuario,
 * evitando exponer excepciones técnicas crudas del framework o del proxy
 * (ej: "Http failure response for http://...: 500 Internal Server Error").
 */
export function extractHttpErrorMessage(
  err: unknown,
  fallback = 'Error al conectar con el servidor'
): string {
  if (!err) {
    return fallback;
  }

  const httpErr = err as Partial<HttpErrorResponse> & { error?: any; message?: string };

  // Servidor caído, sin red o error de conexión
  if (httpErr.status === 0) {
    return 'No se pudo conectar con el servidor backend (puerto 8080). Verifica que los contenedores de Docker estén activos.';
  }

  // Error de proxy / Gateway Timeout / Servicio no disponible
  if (httpErr.status === 502 || httpErr.status === 503 || httpErr.status === 504) {
    return 'El servidor backend aún se está inicializando o no responde. Por favor espera unos momentos e inténtalo de nuevo.';
  }

  // Error 500: Si viene de Vite proxy (ECONNREFUSED) o sin payload estructurado
  if (httpErr.status === 500) {
    if (httpErr.error && typeof httpErr.error === 'object' && httpErr.error.message) {
      return httpErr.error.message;
    }
    return 'El backend no está listo o rechazó la conexión (puerto 8080). Si acabas de iniciar Docker, Spring Boot aún está compilando o descargando dependencias.';
  }

  // Error 401: Credenciales inválidas
  if (httpErr.status === 401) {
    if (httpErr.error && typeof httpErr.error === 'object' && httpErr.error.message) {
      return httpErr.error.message;
    }
    return 'Credenciales inválidas. Verifica tu correo, contraseña u organización.';
  }

  // Error 404: No encontrado (organización o usuario no registrado)
  if (httpErr.status === 404) {
    if (httpErr.error && typeof httpErr.error === 'object' && httpErr.error.message) {
      return httpErr.error.message;
    }
    return 'La organización o el usuario no existe. Si restableciste la base de datos con "down -v", debes registrar nuevamente tu empresa.';
  }

  // Si el backend envió un payload JSON estructurado con { message: '...' }
  if (httpErr.error && typeof httpErr.error === 'object') {
    if (httpErr.error.message && typeof httpErr.error.message === 'string') {
      return httpErr.error.message;
    }
    if (httpErr.error.error && typeof httpErr.error.error === 'string') {
      return httpErr.error.error;
    }
  }

  // Si err.error es un string directo que no sea un volcado de HTML
  if (typeof httpErr.error === 'string' && httpErr.error.trim() && !httpErr.error.includes('<html')) {
    return httpErr.error.trim();
  }

  // Mensaje genérico de JS, ignorando el formato técnico crudo de Angular
  if (httpErr.message && !httpErr.message.startsWith('Http failure response for')) {
    return httpErr.message;
  }

  return fallback;
}
