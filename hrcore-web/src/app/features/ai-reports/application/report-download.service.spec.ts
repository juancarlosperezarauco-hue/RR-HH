import { fakeAsync, tick } from '@angular/core/testing';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { exportFilename, ReportDownloadService } from './report-download.service';

describe('Descargas de reportes: nombres y liberación de archivos', () => {
  it('acepta nombres del servidor y rechaza rutas, nombres personales y extensiones incorrectas', () => {
    expect(exportFilename("attachment; filename*=UTF-8''hrcore-reporte-20261020-120000.xlsx", 'xlsx')).toBe('hrcore-reporte-20261020-120000.xlsx');
    for (const value of ['../../private.csv', 'Áda.csv', 'hrcore-reporte-20261020-120000.exe'])
      expect(exportFilename(`attachment; filename="${value}"`, 'csv')).toBe('hrcore-reporte.csv');
    expect(exportFilename("attachment; filename*=UTF-8''%invalid", 'pdf')).toBe('hrcore-reporte.pdf');
  });
  it('descarga un Blob y elimina el enlace y su URL temporal', fakeAsync(() => {
    const create = spyOn(URL, 'createObjectURL').and.returnValue('blob:synthetic');
    const revoke = spyOn(URL, 'revokeObjectURL');
    const click = spyOn(HTMLAnchorElement.prototype, 'click');
    new ReportDownloadService().save(new HttpResponse({ body: new Blob(['header\r\n']), headers: new HttpHeaders({
      'Content-Type': 'text/csv;charset=UTF-8', 'Content-Disposition': 'attachment; filename="hrcore-reporte-20261020-120000.csv"'
    }) }), 'csv');
    expect(create).toHaveBeenCalledTimes(1); expect(click).toHaveBeenCalledTimes(1);
    expect(document.querySelector('a[download="hrcore-reporte-20261020-120000.csv"]')).toBeNull();
    tick(); expect(revoke).toHaveBeenCalledOnceWith('blob:synthetic');
  }));
  it('rechaza una respuesta que no sea un archivo del formato solicitado', () => {
    expect(() => new ReportDownloadService().save(new HttpResponse({ body: new Blob(['{}']),
      headers: new HttpHeaders({ 'Content-Type': 'application/json' }) }), 'pdf')).toThrow();
  });
});
