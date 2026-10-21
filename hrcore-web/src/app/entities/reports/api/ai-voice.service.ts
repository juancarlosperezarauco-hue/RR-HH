import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse } from '../../../shared/api';

export interface AiVoiceConfig { enabled: boolean; maxDurationSeconds: number; maxBytes: number; }

@Injectable({ providedIn: 'root' })
export class AiVoiceService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/api/hr/ai/transcriptions`;
  config() {
    return this.http.get<ApiResponse<AiVoiceConfig>>(`${this.url}/config`).pipe(map(response => response.data));
  }
  transcribe(audio: Blob, extension: string) {
    const form = new FormData();
    form.append('file', audio, `recording.${extension}`);
    return this.http.post<ApiResponse<{ text: string }>>(this.url, form).pipe(map(response => response.data.text));
  }
}
