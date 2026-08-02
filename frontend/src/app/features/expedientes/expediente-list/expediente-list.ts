import { ErrorState } from '../../../shared/components/error-state/error-state';
import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { toSignal } from '@angular/core/rxjs-interop';
import { debounceTime, firstValueFrom } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ExpedienteService } from '../../../core/services/expediente.service';
import {
  ExpedienteResponse,
  rutaRetomarExpediente,
} from '../../../core/models/expediente.model';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-expediente-list',
  imports: [
    ErrorState,
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './expediente-list.html',
  styleUrl: './expediente-list.scss',
})
export class ExpedienteList implements OnInit {
  private readonly expedienteService = inject(ExpedienteService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  // Estado UI
  readonly cargando = signal(true);
  readonly errorCarga = signal<string | null>(null);
  /** La request no llegó al backend (status 0): sin red o servidor caído. */
  readonly sinConexion = signal(false);

  // Datos
  readonly expedientes = signal<ExpedienteResponse[]>([]);

  // Filtro de búsqueda local
  readonly filtroControl = this.fb.nonNullable.control('');
  private readonly filtroSignal = toSignal(
    this.filtroControl.valueChanges.pipe(debounceTime(150)),
    { initialValue: '' }
  );

  // Lista filtrada (computed): filtra por nombre, comitente, obra o tarea
  readonly expedientesFiltrados = computed(() => {
    const filtro = this.filtroSignal().toLowerCase().trim();
    const todos = this.expedientes();
    if (!filtro) return todos;
    return todos.filter((e) =>
      (e.nombre?.toLowerCase().includes(filtro) ?? false) ||
      (e.comitenteNombre?.toLowerCase().includes(filtro) ?? false) ||
      (e.obraDesignacion?.toLowerCase().includes(filtro) ?? false) ||
      (e.tipoTareaNombre?.toLowerCase().includes(filtro) ?? false) ||
      (e.tipoTareaCodigo?.toLowerCase().includes(filtro) ?? false)
    );
  });

  readonly columnasMostradas = [
    'nombre',
    'comitente',
    'tarea',
    'estado',
    'fecha',
    'acciones',
  ];

  ngOnInit(): void {
    this.cargarExpedientes();
  }

  cargarExpedientes(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    this.expedienteService.listarMisExpedientes().subscribe({
      next: (datos) => {
        this.expedientes.set(datos);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.sinConexion.set(err.status === 0);
        this.errorCarga.set('No se pudieron cargar los expedientes. Intentá recargar.');
        this.cargando.set(false);
      },
    });
  }

  irANuevo(): void {
    this.router.navigate(['/expedientes/nuevo']);
  }

  abrir(expediente: ExpedienteResponse): void {
    this.router.navigate(rutaRetomarExpediente(expediente));
  }

  async confirmarEliminar(expediente: ExpedienteResponse): Promise<void> {
    const nombre = expediente.nombre || 'Expediente sin nombre';
    const data: ConfirmDialogData = {
      titulo: 'Eliminar expediente',
      mensaje: `¿Estás seguro de que querés eliminar "${nombre}"?`,
      detalle: 'Esta acción no se puede deshacer.',
      textoConfirmar: 'Eliminar',
      textoCancelar: 'Cancelar',
      variant: 'destructive',
    };

    const ref = this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(
      ConfirmDialog,
      { data, width: '420px' }
    );

    const confirmado = await firstValueFrom(ref.afterClosed());
    if (confirmado) {
      this.eliminar(expediente);
    }
  }

  private eliminar(expediente: ExpedienteResponse): void {
    this.expedienteService.eliminar(expediente.id).subscribe({
      next: () => {
        this.expedientes.update((lista) => lista.filter((e) => e.id !== expediente.id));
        this.snackBar.open(
          `"${expediente.nombre || 'Expediente'}" fue eliminado.`,
          'Cerrar',
          { duration: 3000, panelClass: ['snackbar-success'] }
        );
      },
      error: (err: HttpErrorResponse) => {
        const mensaje = err.error?.mensaje || 'No se pudo eliminar el expediente.';
        this.snackBar.open(mensaje, 'Cerrar', {
          duration: 5000,
          panelClass: ['snackbar-error'],
        });
      },
    });
  }
}
