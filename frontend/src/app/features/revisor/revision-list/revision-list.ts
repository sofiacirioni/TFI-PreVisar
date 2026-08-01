import { ErrorState } from '../../../shared/components/error-state/error-state';
import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs/internal/firstValueFrom';
import { RevisionExternaService } from '../../../core/services/revision-externa.service';
import { RevisionResumen } from '../../../core/models/revision-externa.model';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../shared/components/confirm-dialog/confirm-dialog';

/** Límite práctico del modo inline de Gemini; el backend rechaza igual, pero avisamos antes. */
const MAX_BYTES = 15 * 1024 * 1024;

@Component({
  selector: 'app-revision-list',
  imports: [
    ErrorState,
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './revision-list.html',
  styleUrl: './revision-list.scss',
})
export class RevisionList implements OnInit {
  private readonly service = inject(RevisionExternaService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly cargando = signal(true);
  readonly errorCarga = signal<string | null>(null);
  /** La request no llegó al backend (status 0): sin red o servidor caído. */
  readonly sinConexion = signal(false);
  readonly subiendo = signal(false);
  readonly revisiones = signal<RevisionResumen[]>([]);

  readonly columnas = ['archivo', 'estado', 'fecha', 'acciones'];

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.service.listar().subscribe({
      next: (datos) => {
        this.revisiones.set(datos);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.sinConexion.set(err.status === 0);
        this.errorCarga.set('No se pudieron cargar las revisiones. Intentá recargar.');
        this.cargando.set(false);
      },
    });
  }

  /** Disparado por el input file oculto. */
  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = ''; // permite volver a elegir el mismo archivo
    if (!archivo) return;

    if (archivo.type !== 'application/pdf') {
      this.aviso('El expediente debe ser un único archivo PDF.', 'error');
      return;
    }
    if (archivo.size > MAX_BYTES) {
      this.aviso('El PDF supera el límite de 15 MB.', 'error');
      return;
    }
    this.subir(archivo);
  }

  private subir(archivo: File): void {
    this.subiendo.set(true);
    this.service.crear(archivo).subscribe({
      next: ({ id }) => {
        this.subiendo.set(false);
        // El análisis corre en segundo plano: abrimos el detalle, que hace polling.
        this.router.navigate(['/revisar', id]);
      },
      error: (err: HttpErrorResponse) => {
        this.subiendo.set(false);
        this.aviso(err.error?.mensaje || 'No se pudo subir el expediente.', 'error');
      },
    });
  }

  abrir(r: RevisionResumen): void {
    this.router.navigate(['/revisar', r.id]);
  }

  async confirmarEliminar(r: RevisionResumen): Promise<void> {
    const data: ConfirmDialogData = {
      titulo: 'Eliminar revisión',
      mensaje: `¿Eliminar la revisión de "${r.nombreArchivo}"?`,
      detalle: 'Se borrará el PDF y su resumen. Esta acción no se puede deshacer.',
      textoConfirmar: 'Eliminar',
      textoCancelar: 'Cancelar',
      variant: 'destructive',
    };
    const ref = this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
      data,
      width: '420px',
    });
    if (await firstValueFrom(ref.afterClosed())) {
      this.eliminar(r);
    }
  }

  private eliminar(r: RevisionResumen): void {
    this.service.eliminar(r.id).subscribe({
      next: () => {
        this.revisiones.update((lista) => lista.filter((x) => x.id !== r.id));
        this.aviso(`Revisión de "${r.nombreArchivo}" eliminada.`, 'success');
      },
      error: (err: HttpErrorResponse) => {
        this.aviso(err.error?.mensaje || 'No se pudo eliminar la revisión.', 'error');
      },
    });
  }

  private aviso(mensaje: string, tipo: 'success' | 'error'): void {
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: tipo === 'error' ? 5000 : 3000,
      panelClass: [tipo === 'error' ? 'snackbar-error' : 'snackbar-success'],
    });
  }
}
