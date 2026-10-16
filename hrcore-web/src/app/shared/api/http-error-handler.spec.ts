import { HttpErrorResponse } from '@angular/common/http';
import { extractHttpErrorMessage } from './http-error-handler';

describe('extractHttpErrorMessage', () => {
  it('debe devolver fallback cuando el error es nulo o indefinido', () => {
    expect(extractHttpErrorMessage(null)).toBe('Error al conectar con el servidor');
    expect(extractHttpErrorMessage(undefined, 'Fallback custom')).toBe('Fallback custom');
  });

  it('debe devolver mensaje instructivo para error status 0 (servidor no disponible o caído)', () => {
    const error = new HttpErrorResponse({ status: 0, statusText: 'Unknown Error' });
    const msg = extractHttpErrorMessage(error);
    expect(msg).toContain('No se pudo conectar con el servidor backend');
    expect(msg).toContain('Docker');
  });

  it('debe devolver mensaje instructivo para error status 500 originado por proxy / backend no listo', () => {
    const proxyError = new HttpErrorResponse({
      status: 500,
      statusText: 'Internal Server Error',
      error: 'Error: connect ECONNREFUSED 172.20.0.5:8080'
    });
    const msg = extractHttpErrorMessage(proxyError);
    expect(msg).toContain('backend');
    expect(msg).not.toContain('Http failure response for');
  });

  it('debe respetar el mensaje de negocio cuando el backend 500 devuelve payload estructurado', () => {
    const businessError = new HttpErrorResponse({
      status: 500,
      error: { message: 'Error interno en la base de datos de la empresa' }
    });
    const msg = extractHttpErrorMessage(businessError);
    expect(msg).toBe('Error interno en la base de datos de la empresa');
  });

  it('debe devolver mensaje amigable para 401 si no hay message específico', () => {
    const authError = new HttpErrorResponse({ status: 401, error: {} });
    expect(extractHttpErrorMessage(authError)).toContain('Credenciales inválidas');
  });

  it('debe devolver mensaje orientativo para 404 sobre la pérdida de datos tras docker down -v', () => {
    const notFoundError = new HttpErrorResponse({ status: 404, error: {} });
    const msg = extractHttpErrorMessage(notFoundError);
    expect(msg).toContain('down -v');
  });

  it('nunca debe exponer la cadena técnica "Http failure response for"', () => {
    const rawError = {
      message: 'Http failure response for http://localhost:4200/api/auth/login: 500 Internal Server Error'
    };
    const msg = extractHttpErrorMessage(rawError);
    expect(msg).not.toContain('Http failure response for');
  });
});
