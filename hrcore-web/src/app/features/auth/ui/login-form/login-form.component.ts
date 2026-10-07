import { Component, ElementRef, EventEmitter, ViewChild, inject, Input, OnDestroy, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { LoginRequest } from '../../../../entities/auth/model/login-request.model';
import { FaceLoginRequest } from '../../../../entities/auth/model/face-login-request.model';

@Component({
  selector: 'app-login-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, LucideAngularModule],
  template: `
    <div class="mx-auto w-full max-w-md rounded-[28px] border border-[#C8E6C9] bg-white p-8 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
      <div class="mb-8 text-center">
        <div class="mx-auto mb-4 inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-[#2E7D32] text-white shadow-sm">
          <lucide-icon name="shield" class="h-6 w-6"></lucide-icon>
        </div>
        <h2 class="text-2xl font-black tracking-tight text-[#1B5E20]">Iniciar sesión</h2>
        <p class="mt-2 text-sm text-[#5F6F5F]">Elige entre contraseña o reconocimiento facial.</p>
      </div>

      <div *ngIf="status === 'error'" class="mb-6 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
        {{ error }}
      </div>

      <div class="mb-6 grid grid-cols-2 rounded-full bg-[#F1F8E9] p-1">
        <button
          type="button"
          (click)="setActiveTab('password')"
          [class.bg-white]="activeTab === 'password'"
          [class.text-[#1B5E20]]="activeTab === 'password'"
          [class.text-[#6B7D6C]]="activeTab !== 'password'"
          class="rounded-full px-4 py-2 text-sm font-semibold transition-colors">
          Contraseña
        </button>
        <button
          type="button"
          (click)="setActiveTab('face')"
          [class.bg-white]="activeTab === 'face'"
          [class.text-[#1B5E20]]="activeTab === 'face'"
          [class.text-[#6B7D6C]]="activeTab !== 'face'"
          class="rounded-full px-4 py-2 text-sm font-semibold transition-colors">
          Rostro
        </button>
      </div>

      <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-4">
        @if (activeTab === 'password') {
          <section class="space-y-4">
            <div>
              <label for="tenantSlug" class="mb-1 block text-sm font-semibold text-[#567157]">Nombre de organización</label>
              <input
                id="tenantSlug"
                type="text"
                autocomplete="off"
                autocapitalize="off"
                spellcheck="false"
                formControlName="tenantSlug"
                class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                [class.border-red-300]="isFieldInvalid('tenantSlug')"
                placeholder="ej: miempresa">
              <span *ngIf="isFieldInvalid('tenantSlug')" class="mt-1 text-xs text-red-600">Ingresa el identificador único de tu empresa.</span>
            </div>

            <div>
              <label for="email" class="mb-1 block text-sm font-semibold text-[#567157]">Correo electrónico</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                [class.border-red-300]="isFieldInvalid('email')"
                placeholder="admin@miempresa.com">
              <span *ngIf="isFieldInvalid('email')" class="mt-1 text-xs text-red-600">Ingresa un correo válido.</span>
            </div>

            <div>
              <label for="password" class="mb-1 block text-sm font-semibold text-[#567157]">Contraseña</label>
              <div class="relative">
                <input
                  id="password"
                  [type]="showPassword ? 'text' : 'password'"
                  formControlName="password"
                  class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 pr-10 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                  [class.border-red-300]="isFieldInvalid('password')"
                  placeholder="••••••••">
                <button
                  type="button"
                  (click)="togglePasswordVisibility()"
                  class="absolute inset-y-0 right-0 flex items-center pr-3 text-[#6B7D6C] transition-colors hover:text-[#2E7D32]"
                  tabindex="-1">
                  <lucide-icon [name]="showPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                </button>
              </div>
              <span *ngIf="isFieldInvalid('password')" class="mt-1 text-xs text-red-600">La contraseña es requerida.</span>
            </div>

            <div class="flex items-center justify-between gap-3">
              <a routerLink="/forgot-password" class="text-sm font-semibold text-[#2E7D32] transition-colors hover:text-[#256428] hover:underline">
                Olvidé mi contraseña
              </a>
            </div>

            <button
              type="submit"
              [disabled]="status === 'loading'"
              class="flex w-full cursor-pointer items-center justify-center rounded-full bg-[#2E7D32] px-4 py-3 font-semibold text-white transition-colors hover:bg-[#256428] disabled:cursor-not-allowed disabled:opacity-50">
              <ng-container *ngIf="status !== 'loading'; else loadingSpinner">
                Ingresar
              </ng-container>
              <ng-template #loadingSpinner>
                <svg class="mr-3 h-5 w-5 animate-spin" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Autenticando...
              </ng-template>
            </button>
          </section>
        } @else {
          <section class="space-y-4">
            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label for="faceTenantSlug" class="mb-1 block text-sm font-semibold text-[#567157]">Nombre de organización</label>
                <input
                  id="faceTenantSlug"
                  type="text"
                  autocomplete="off"
                  autocapitalize="off"
                  spellcheck="false"
                  formControlName="tenantSlug"
                  class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                  [class.border-red-300]="isFieldInvalid('tenantSlug')"
                  placeholder="ej: miempresa">
                <span *ngIf="isFieldInvalid('tenantSlug')" class="mt-1 text-xs text-red-600">El tenant es obligatorio para el login facial.</span>
              </div>

              <div>
                <label for="faceEmail" class="mb-1 block text-sm font-semibold text-[#567157]">Correo electrónico</label>
                <input
                  id="faceEmail"
                  type="email"
                  formControlName="email"
                  class="w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white"
                  [class.border-red-300]="isFieldInvalid('email')"
                  placeholder="admin@miempresa.com">
                <span *ngIf="isFieldInvalid('email')" class="mt-1 text-xs text-red-600">El correo es obligatorio para el login facial.</span>
              </div>
            </div>

            <div class="rounded-3xl border border-dashed border-[#DDEED8] bg-[#FBFDF8] p-4">
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-sm font-semibold text-[#1B5E20]">Acceso facial</p>
                  <p class="mt-1 text-xs leading-5 text-[#6B7D6C]">Usa la cámara frontal o la cámara del dispositivo. Necesitas tenant, correo e imagen.</p>
                </div>
                <lucide-icon name="camera" class="h-5 w-5 text-[#2E7D32]"></lucide-icon>
              </div>

              <div class="mt-4 grid gap-3 sm:grid-cols-2">
                <div class="space-y-2">
                  <button
                    type="button"
                    (click)="openSelfieCamera()"
                    class="inline-flex w-full items-center justify-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                    <lucide-icon name="camera" class="h-4 w-4"></lucide-icon>
                    Tomar selfie
                  </button>
                </div>

                <div class="space-y-2">
                  <input
                    #galleryInput
                    type="file"
                    accept="image/*"
                    class="hidden"
                    (change)="onFaceFileSelected($event)">
                  <button
                    type="button"
                    (click)="openGallery()"
                    class="inline-flex w-full items-center justify-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                    <lucide-icon name="camera" class="h-4 w-4"></lucide-icon>
                    Usar foto
                  </button>
                </div>
              </div>

              @if (cameraOpen) {
                <div class="mt-4 space-y-3 rounded-3xl border border-[#C8E6C9] bg-white p-4 shadow-sm">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <p class="text-sm font-semibold text-[#1B5E20]">Cámara activa</p>
                      <p class="text-xs text-[#6B7D6C]">
                        {{ cameraFacingMode === 'user' ? 'Cámara frontal' : 'Cámara trasera' }}
                      </p>
                    </div>
                    <button
                      type="button"
                      (click)="closeCamera()"
                      class="rounded-full border border-[#C8E6C9] bg-white px-3 py-1.5 text-xs font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                      Cerrar
                    </button>
                  </div>

                  <div class="overflow-hidden rounded-2xl border border-[#E8F2E2] bg-black">
                    <video #cameraVideo autoplay playsinline muted class="h-64 w-full object-cover"></video>
                  </div>

                  @if (cameraError) {
                    <p class="text-xs text-red-600">{{ cameraError }}</p>
                  }

                  <div class="flex flex-wrap gap-3">
                    <button
                      type="button"
                      (click)="captureCameraPhoto()"
                      [disabled]="cameraBusy"
                      class="inline-flex items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-50">
                      <lucide-icon name="camera" class="h-4 w-4"></lucide-icon>
                      {{ cameraBusy ? 'Capturando...' : 'Capturar foto' }}
                    </button>
                    <button
                      type="button"
                      (click)="openSelfieCamera()"
                      [disabled]="cameraBusy"
                      class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9] disabled:cursor-not-allowed disabled:opacity-50">
                      Cambiar a selfie
                    </button>
                  </div>
                </div>
              }

              <p class="mt-4 text-xs text-[#6B7D6C]">
                Imagen seleccionada: <span class="font-semibold text-[#1B5E20]">{{ faceFileLabel() }}</span>
              </p>

              <div *ngIf="selectedFacePreview" class="mt-4 overflow-hidden rounded-2xl border border-[#E8F2E2] bg-white">
                <img [src]="selectedFacePreview" alt="Vista previa de rostro" class="h-40 w-full object-cover">
              </div>

              <div class="mt-4 flex flex-wrap gap-3">
                <button
                  type="button"
                  (click)="onFaceLogin()"
                  [disabled]="status === 'loading' || !selectedFaceFile"
                  class="inline-flex items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-50">
                  <lucide-icon name="camera" class="h-4 w-4"></lucide-icon>
                  Entrar con rostro
                </button>
              </div>
            </div>
          </section>
        }

        <div class="mt-6 border-t border-[#E8F2E2] pt-4 text-center text-sm text-[#5F6F5F]">
          ¿No tienes una empresa registrada? <br>
          <a routerLink="/prices" class="mt-1 inline-block font-semibold text-[#2E7D32] transition-colors hover:text-[#256428] hover:underline">
            Regístrate y crea tu entorno
          </a>
        </div>

        <div class="mt-4 text-center text-sm text-[#666666] border-t border-[#C8E6C9] pt-4">
          <a routerLink="/" class="font-medium text-[#4CAF50] hover:text-[#2E7D32] transition-colors">
            ← Volver al inicio
          </a>
        </div>
      </form>
    </div>
  `
})
export class LoginFormComponent implements OnDestroy {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  @Input() error: string | null = null;

  @Output() loginSubmit = new EventEmitter<LoginRequest>();
  @Output() faceLoginSubmit = new EventEmitter<FaceLoginRequest>();

  activeTab: 'password' | 'face' = 'password';
  showPassword = false;
  selectedFaceFile: File | null = null;
  selectedFacePreview: string | null = null;
  cameraOpen = false;
  cameraError: string | null = null;
  cameraBusy = false;
  cameraFacingMode: 'user' | 'environment' = 'user';
  private cameraStream: MediaStream | null = null;

  @ViewChild('cameraVideo') cameraVideo?: ElementRef<HTMLVideoElement>;
  @ViewChild('galleryInput') galleryInput?: ElementRef<HTMLInputElement>;

  private readonly fb = inject(FormBuilder);

  loginForm: FormGroup = this.fb.group({
    tenantSlug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  onSubmit(): void {
    if (this.loginForm.valid && this.status !== 'loading') {
      const tenantSlug = typeof this.loginForm.value.tenantSlug === 'string'
        ? this.loginForm.value.tenantSlug.trim()
        : '';

      this.loginSubmit.emit({
        email: this.loginForm.value.email,
        password: this.loginForm.value.password,
        tenantSlug
      });
    } else {
      this.loginForm.markAllAsTouched();
    }
  }

  setActiveTab(tab: 'password' | 'face'): void {
    this.activeTab = tab;
  }

  onFaceFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (this.selectedFacePreview) {
      URL.revokeObjectURL(this.selectedFacePreview);
      this.selectedFacePreview = null;
    }

    this.selectedFaceFile = file;
    if (file) {
      this.selectedFacePreview = URL.createObjectURL(file);
    }
  }

  openSelfieCamera(): void {
    void this.openCamera('user');
  }

  openGallery(): void {
    this.galleryInput?.nativeElement.click();
  }

  private async openCamera(facingMode: 'user' | 'environment'): Promise<void> {
    if (this.status === 'loading') {
      return;
    }

    this.cameraError = null;
    this.cameraFacingMode = facingMode;
    this.cameraOpen = true;

    await this.stopCamera();

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode
        },
        audio: false
      });

      this.cameraStream = stream;

      setTimeout(() => {
        const video = this.cameraVideo?.nativeElement;
        if (video) {
          video.srcObject = stream;
          video.play().catch(() => undefined);
        }
      });
    } catch (error: any) {
      this.cameraError = 'No se pudo acceder a la cámara. Revisa permisos o usa un navegador compatible.';
      this.cameraOpen = false;
      this.cameraStream = null;
      console.error('camera error', error);
    }
  }

  async captureCameraPhoto(): Promise<void> {
    const video = this.cameraVideo?.nativeElement;
    if (!video || !video.videoWidth || !video.videoHeight || this.cameraBusy) {
      return;
    }

    this.cameraBusy = true;
    try {
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const context = canvas.getContext('2d');
      if (!context) {
        return;
      }

      context.drawImage(video, 0, 0, canvas.width, canvas.height);
      const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.92));
      if (!blob) {
        return;
      }

      this.setSelectedFaceBlob(blob, `selfie-${Date.now()}.jpg`);
      await this.stopCamera();
      this.cameraOpen = false;
    } finally {
      this.cameraBusy = false;
    }
  }

  async closeCamera(): Promise<void> {
    await this.stopCamera();
    this.cameraOpen = false;
  }

  private setSelectedFaceBlob(blob: Blob, fileName: string): void {
    if (this.selectedFacePreview) {
      URL.revokeObjectURL(this.selectedFacePreview);
      this.selectedFacePreview = null;
    }

    const file = new File([blob], fileName, { type: blob.type || 'image/jpeg' });
    this.selectedFaceFile = file;
    this.selectedFacePreview = URL.createObjectURL(file);
  }

  private async stopCamera(): Promise<void> {
    if (this.cameraStream) {
      this.cameraStream.getTracks().forEach(track => track.stop());
      this.cameraStream = null;
    }

    const video = this.cameraVideo?.nativeElement;
    if (video) {
      video.pause();
      (video as HTMLVideoElement & { srcObject?: MediaStream | null }).srcObject = null;
    }
  }

  onFaceLogin(): void {
    if (this.status === 'loading') {
      return;
    }

    const tenantSlug = typeof this.loginForm.value.tenantSlug === 'string'
      ? this.loginForm.value.tenantSlug.trim()
      : '';
    const email = typeof this.loginForm.value.email === 'string'
      ? this.loginForm.value.email.trim()
      : '';

    const tenantField = this.loginForm.get('tenantSlug');
    const emailField = this.loginForm.get('email');

    if (!tenantSlug || !email || !this.selectedFaceFile || tenantField?.invalid || emailField?.invalid) {
      tenantField?.markAsTouched();
      emailField?.markAsTouched();
      return;
    }

    this.faceLoginSubmit.emit({
      tenantSlug,
      email,
      image: this.selectedFaceFile
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }

  faceFileLabel(): string {
    return this.selectedFaceFile?.name || 'No se ha seleccionado ninguna imagen';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  ngOnDestroy(): void {
    void this.stopCamera();
    if (this.selectedFacePreview) {
      URL.revokeObjectURL(this.selectedFacePreview);
    }
  }
}
