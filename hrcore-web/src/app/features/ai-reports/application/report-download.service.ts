import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { AiReportExportFormat } from '../../../entities/reports/model/ai-conversation.model';

@Injectable({ providedIn: 'root' })
export class ReportDownloadService {
  save(response: HttpResponse<Blob>, format: AiReportExportFormat): void {
    const mime = { csv: 'text/csv', xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', pdf: 'application/pdf' }[format];
    if (!response.body?.size || response.headers.get('Content-Type')?.split(';')[0].trim() !== mime)
      throw new Error('No se recibió un archivo de reporte válido.');
    const filename = exportFilename(response.headers.get('Content-Disposition') ?? '', format);
    const url = URL.createObjectURL(response.body);
    const anchor = document.createElement('a');
    try {
      anchor.href = url; anchor.download = filename; anchor.hidden = true;
      document.body.appendChild(anchor); anchor.click();
    } finally {
      anchor.remove(); setTimeout(() => URL.revokeObjectURL(url), 0);
    }
  }
}

export function exportFilename(disposition: string, format: AiReportExportFormat): string {
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
  const plain = /filename="([^"]+)"/i.exec(disposition)?.[1] ?? /filename=([^;\s]+)/i.exec(disposition)?.[1];
  let candidate = plain ?? '';
  if (encoded) { try { candidate = decodeURIComponent(encoded); } catch { candidate = ''; } }
  return new RegExp(`^hrcore-reporte-\\d{8}-\\d{6}\\.${format}$`).test(candidate)
    ? candidate : `hrcore-reporte.${format}`;
}
