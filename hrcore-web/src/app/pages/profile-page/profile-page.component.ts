import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, OnInit, ViewChild, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { environment } from '../../../environments/environment';
import { ProfileUseCase } from '../../features/profile';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, LucideAngularModule],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              Mi cuenta
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Mi perfil
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Actualiza tus datos visibles y la foto usada para el login facial.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <a
              routerLink="/dashboard"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="arrow-left" class="h-4 w-4"></lucide-icon>
              Volver
            </a>
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32]">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Recargar
            </button>
          </div>
        </div>
      </section>

      @if (profileUseCase.status() === 'loading' && !profile()) {
        <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-8 text-sm text-[#6B7D6C] shadow-sm">
          Cargando perfil...
        </section>
      }

      @if (error()) {
        <section class="rounded-[24px] border border-[#F3C6C6] bg-[#FFF7F7] p-6 shadow-sm">
          <p class="text-sm font-semibold text-[#B42318]">{{ error() }}</p>
        </section>
      }

      @if (profile(); as currentProfile) {
        <section class="grid gap-6 xl:grid-cols-[0.8fr_1.2fr]">
          <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
            <div class="flex items-center gap-4">
              <div class="flex h-24 w-24 overflow-hidden rounded-3xl border border-[#C8E6C9] bg-[#F7FBF3]">
                @if (selectedPhotoPreview()) {
                  <img [src]="selectedPhotoPreview()" alt="Foto de perfil" class="h-full w-full object-cover">
                } @else if (previewSrc()) {
                  <img [src]="previewSrc()" alt="Foto de perfil" class="h-full w-full object-cover">
                } @else {
                  <div class="flex h-full w-full items-center justify-center text-2xl font-black text-[#2E7D32]">
                    {{ initials(currentProfile.firstName, currentProfile.lastName) }}
                  </div>
                }
              </div>

              <div class="min-w-0 flex-1">
                <p class="text-sm font-semibold uppercase tracking-[0.12em] text-[#6B7D6C]">Usuario</p>
                <p class="mt-1 truncate text-xl font-black text-[#1B5E20]">
                  {{ currentProfile.firstName }} {{ currentProfile.lastName }}
                </p>
                <p class="mt-1 break-words text-sm text-[#6B7D6C]">{{ currentProfile.email }}</p>
              </div>
            </div>

            <div class="mt-6 space-y-3 rounded-2xl border border-dashed border-[#DDEED8] bg-[#FBFDF8] p-4">
              <p class="text-sm font-semibold text-[#1B5E20]">Foto de perfil</p>
              <p class="text-xs leading-5 text-[#6B7D6C]">
                Esta imagen también se usa para el login facial.
              </p>

              <div class="grid gap-3 sm:grid-cols-2">
                <button
                  type="button"
                  (click)="openProfileSelfieCamera()"
                  class="inline-flex items-center justify-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                  <lucide-icon name="camera" class="h-4 w-4"></lucide-icon>
                  Tomar foto
                </button>

                <div>
                  <input
                    #profileGalleryInput
                    type="file"
                    accept="image/*"
                    class="hidden"
                    (change)="onPhotoSelected($event)">
                  <button
                    type="button"
                    (click)="openProfileGallery()"
                    class="inline-flex w-full items-center justify-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                    <lucide-icon name="camera" class="h-4 w-4"></lucide-icon>
                    Usar foto
                  </button>
                </div>
              </div>

              @if (cameraOpen()) {
                <div class="mt-4 space-y-3 rounded-3xl border border-[#C8E6C9] bg-white p-4 shadow-sm">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <p class="text-sm font-semibold text-[#1B5E20]">Cámara activa</p>
                      <p class="text-xs text-[#6B7D6C]">Cámara frontal para tu perfil</p>
                    </div>
                    <button
                      type="button"
                      (click)="closeProfileCamera()"
                      class="rounded-full border border-[#C8E6C9] bg-white px-3 py-1.5 text-xs font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                      Cerrar
                    </button>
                  </div>

                  <div class="overflow-hidden rounded-2xl border border-[#E8F2E2] bg-black">
                    <video #profileCameraVideo autoplay playsinline muted class="h-64 w-full object-cover"></video>
                  </div>

                  @if (cameraError()) {
                    <p class="text-xs text-red-600">{{ cameraError() }}</p>
                  }

                  <div class="flex flex-wrap gap-3">
                    <button
                      type="button"
                      (click)="captureProfileCameraPhoto()"
                      [disabled]="cameraBusy()"
                      class="inline-flex items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-50">
                      <lucide-icon name="camera" class="h-4 w-4"></lucide-icon>
                      {{ cameraBusy() ? 'Capturando...' : 'Capturar foto' }}
                    </button>
                  </div>
                </div>
              }

              <div class="flex flex-wrap gap-3">
                <button
                  type="button"
                  (click)="removePhoto()"
                  [disabled]="profileUseCase.status() === 'saving' || !currentProfile.profilePhotoAvailable"
                  class="inline-flex items-center gap-2 rounded-full border border-red-200 bg-red-50 px-4 py-2 text-sm font-semibold text-red-700 transition-colors hover:bg-red-100 disabled:cursor-not-allowed disabled:opacity-50">
                  <lucide-icon name="trash-2" class="h-4 w-4"></lucide-icon>
                  Quitar foto
                </button>
              </div>

              @if (selectedPhotoName()) {
                <p class="text-xs text-[#6B7D6C]">
                  Imagen nueva: <span class="font-semibold text-[#1B5E20]">{{ selectedPhotoName() }}</span>
                </p>
              }

              @if (selectedPhotoPreview()) {
                <div class="overflow-hidden rounded-2xl border border-[#E8F2E2] bg-white">
                  <img [src]="selectedPhotoPreview()" alt="Vista previa" class="h-48 w-full object-cover">
                </div>
              }
            </div>
          </div>

          <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
            <h2 class="text-lg font-bold text-[#1B5E20]">Datos del perfil</h2>
            <p class="mt-1 text-sm text-[#6B7D6C]">Puedes actualizar nombre, apellido o foto. El correo no se edita aquí.</p>

            <form [formGroup]="profileForm" (ngSubmit)="save()" class="mt-6 space-y-5">
              <div class="grid gap-5 sm:grid-cols-2">
                <div class="space-y-2">
                  <label class="text-sm font-semibold text-[#567157]" for="firstName">Nombre</label>
                  <input
                    id="firstName"
                    type="text"
                    formControlName="firstName"
                    class="flex h-11 w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-3 py-2 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32] focus:bg-white"
                    placeholder="Ej: Ana">
                </div>
                <div class="space-y-2">
                  <label class="text-sm font-semibold text-[#567157]" for="lastName">Apellido</label>
                  <input
                    id="lastName"
                    type="text"
                    formControlName="lastName"
                    class="flex h-11 w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-3 py-2 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32] focus:bg-white"
                    placeholder="Ej: Pérez">
                </div>
              </div>

              <div class="rounded-2xl border border-[#E8F2E2] bg-[#FBFDF8] p-4 text-sm text-[#6B7D6C]">
                <div class="flex items-start gap-3">
                  <lucide-icon name="shield-check" class="mt-0.5 h-4 w-4 text-[#2E7D32]"></lucide-icon>
                  <p>
                    Tu correo permanece bloqueado por seguridad. Si subes una nueva foto, se reemplaza la usada para el login facial.
                  </p>
                </div>
              </div>

              <div class="flex flex-wrap gap-3 pt-2">
                <button
                  type="submit"
                  [disabled]="profileUseCase.status() === 'saving' || !hasAnyChange()"
                  class="inline-flex items-center gap-2 rounded-full bg-[#1B5E20] px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-50">
                  <lucide-icon name="save" class="h-4 w-4"></lucide-icon>
                  {{ profileUseCase.status() === 'saving' ? 'Guardando...' : 'Guardar cambios' }}
                </button>
                <button
                  type="button"
                  (click)="resetForm()"
                  class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-5 py-3 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                  <lucide-icon name="rotate-ccw" class="h-4 w-4"></lucide-icon>
                  Restaurar
                </button>
              </div>
            </form>
          </div>
        </section>
      }
    </div>
  `
})
export class ProfilePageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  readonly profileUseCase = inject(ProfileUseCase);
  private readonly destroyRef = inject(DestroyRef);

  readonly profile = this.profileUseCase.profile;
  readonly error = this.profileUseCase.error;
  readonly selectedPhotoName = signal<string | null>(null);
  readonly selectedPhotoPreview = signal<string | null>(null);
  readonly previewSrc = signal<string | null>(null);
  readonly cameraOpen = signal(false);
  readonly cameraError = signal<string | null>(null);
  readonly cameraBusy = signal(false);

  readonly profileForm = this.fb.group({
    firstName: [''],
    lastName: ['']
  });

  private originalFirstName = '';
  private originalLastName = '';
  private currentPhotoFile: File | null = null;
  private cameraStream: MediaStream | null = null;

  @ViewChild('profileCameraVideo') profileCameraVideo?: ElementRef<HTMLVideoElement>;
  @ViewChild('profileGalleryInput') profileGalleryInput?: ElementRef<HTMLInputElement>;

  constructor() {
    effect(() => {
      const profile = this.profile();
      if (!profile) {
        return;
      }

      this.originalFirstName = profile.firstName || '';
      this.originalLastName = profile.lastName || '';
      this.profileForm.patchValue({
        firstName: profile.firstName || '',
        lastName: profile.lastName || ''
      }, { emitEvent: false });

      this.previewSrc.set(this.resolvePreview(profile));
    }, { allowSignalWrites: true });

    this.destroyRef.onDestroy(() => {
      this.revokeSelectedPreview();
    });
  }

  async ngOnInit(): Promise<void> {
    await this.profileUseCase.loadProfile();
  }

  async reload(): Promise<void> {
    await this.profileUseCase.loadProfile();
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.currentPhotoFile = file;
    this.selectedPhotoName.set(file?.name || null);
    this.revokeSelectedPreview();
    if (file) {
      this.selectedPhotoPreview.set(URL.createObjectURL(file));
    }
  }

  openProfileSelfieCamera(): void {
    void this.openProfileCamera('user');
  }

  openProfileGallery(): void {
    this.profileGalleryInput?.nativeElement.click();
  }

  private async openProfileCamera(facingMode: 'user' | 'environment'): Promise<void> {
    this.cameraError.set(null);
    this.cameraOpen.set(true);
    await this.stopProfileCamera();

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode },
        audio: false
      });

      this.cameraStream = stream;
      setTimeout(() => {
        const video = this.profileCameraVideo?.nativeElement;
        if (video) {
          video.srcObject = stream;
          video.play().catch(() => undefined);
        }
      });
    } catch (error) {
      this.cameraError.set('No se pudo acceder a la cámara.');
      this.cameraOpen.set(false);
      console.error('profile camera error', error);
    }
  }

  async captureProfileCameraPhoto(): Promise<void> {
    const video = this.profileCameraVideo?.nativeElement;
    if (!video || !video.videoWidth || !video.videoHeight || this.cameraBusy()) {
      return;
    }

    this.cameraBusy.set(true);
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

      this.setProfilePhotoBlob(blob, `profile-${Date.now()}.jpg`);
      await this.stopProfileCamera();
      this.cameraOpen.set(false);
    } finally {
      this.cameraBusy.set(false);
    }
  }

  async closeProfileCamera(): Promise<void> {
    await this.stopProfileCamera();
    this.cameraOpen.set(false);
  }

  private setProfilePhotoBlob(blob: Blob, fileName: string): void {
    this.currentPhotoFile = new File([blob], fileName, { type: blob.type || 'image/jpeg' });
    this.selectedPhotoName.set(this.currentPhotoFile.name);
    this.revokeSelectedPreview();
    this.selectedPhotoPreview.set(URL.createObjectURL(this.currentPhotoFile));
  }

  private async stopProfileCamera(): Promise<void> {
    if (this.cameraStream) {
      this.cameraStream.getTracks().forEach(track => track.stop());
      this.cameraStream = null;
    }

    const video = this.profileCameraVideo?.nativeElement;
    if (video) {
      video.pause();
      (video as HTMLVideoElement & { srcObject?: MediaStream | null }).srcObject = null;
    }
  }

  async save(): Promise<void> {
    if (!this.hasAnyChange()) {
      return;
    }

    const firstName = this.profileForm.value.firstName?.trim();
    const lastName = this.profileForm.value.lastName?.trim();
    await this.profileUseCase.saveProfile({
      firstName: firstName || undefined,
      lastName: lastName || undefined,
      photo: this.currentPhotoFile
    });

    if (this.profileUseCase.status() === 'success') {
      this.originalFirstName = firstName || '';
      this.originalLastName = lastName || '';
      this.currentPhotoFile = null;
      this.selectedPhotoName.set(null);
      this.revokeSelectedPreview();
      await this.profileUseCase.loadProfile();
    }
  }

  async removePhoto(): Promise<void> {
    const success = await this.profileUseCase.removeProfilePhoto();
    if (success) {
      this.currentPhotoFile = null;
      this.selectedPhotoName.set(null);
      this.revokeSelectedPreview();
      await this.profileUseCase.loadProfile();
    }
  }

  resetForm(): void {
    this.profileForm.patchValue({
      firstName: this.originalFirstName,
      lastName: this.originalLastName
    });
    this.currentPhotoFile = null;
    this.selectedPhotoName.set(null);
    this.revokeSelectedPreview();
  }

  hasAnyChange(): boolean {
    const firstName = this.profileForm.value.firstName?.trim() || '';
    const lastName = this.profileForm.value.lastName?.trim() || '';
    return firstName !== this.originalFirstName || lastName !== this.originalLastName || !!this.currentPhotoFile;
  }

  initials(firstName: string, lastName: string): string {
    return `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase() || 'U';
  }

  private resolvePreview(profile: { profilePhotoUrl: string | null; updatedAt: string | null } | null): string | null {
    if (!profile?.profilePhotoUrl) {
      return null;
    }

    const cacheBust = profile.updatedAt ? `?v=${encodeURIComponent(profile.updatedAt)}` : '';
    return `${environment.apiUrl}${profile.profilePhotoUrl}${cacheBust}`;
  }

  private revokeSelectedPreview(): void {
    const current = this.selectedPhotoPreview();
    if (current) {
      URL.revokeObjectURL(current);
      this.selectedPhotoPreview.set(null);
    }
  }
}
