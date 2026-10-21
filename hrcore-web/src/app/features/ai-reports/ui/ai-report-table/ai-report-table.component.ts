import { Component, input } from '@angular/core';
import { QueryResult } from '../../../../entities/reports/model/ai-conversation.model';
import { columnLabel, formatReportValue } from '../../application/report-presentation';

@Component({ selector: 'app-ai-report-table', standalone: true, template: `
  <div class="max-w-full overflow-x-auto rounded-md border" tabindex="0" role="region" aria-label="Tabla de resultados; desplazamiento horizontal disponible">
    <table class="w-full text-left text-sm"><caption class="sr-only">Página {{ result().pageNumber + 1 }} del reporte analítico</caption>
      <thead class="bg-muted"><tr>@for (column of result().columns; track column.key) { <th scope="col" class="whitespace-nowrap px-4 py-3 font-semibold">{{ label(column.key) }}</th> }</tr></thead>
      <tbody class="divide-y">@for (row of result().rows; track $index) { <tr class="hover:bg-muted/50">@for (column of result().columns; track column.key) { <td class="max-w-sm whitespace-pre-wrap break-words px-4 py-3 align-top" [class.tabular-nums]="column.type === 'INTEGER'" [class.text-right]="column.type === 'INTEGER'">{{ format(row[column.key], column) }}</td> }</tr> }
        @empty { <tr><td [attr.colspan]="result().columns.length || 1" class="px-4 py-8 text-center text-muted-foreground">No hay resultados para esta consulta o página.</td></tr> }</tbody>
    </table>
  </div>` })
export class AiReportTableComponent {
  readonly result = input.required<QueryResult>();
  readonly label = columnLabel; readonly format = formatReportValue;
}
