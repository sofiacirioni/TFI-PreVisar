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
import { ObraService } from '../../../core/services/obra.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Obra } from '../../../core/models';
import { toSignal } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs/internal/operators/debounceTime';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { firstValueFrom } from 'rxjs/internal/firstValueFrom';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-obra-list',
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
  templateUrl: './obra-list.html',
  styleUrl: './obra-list.scss',
})
export class ObraList implements OnInit {
  private readonly obraService = inject(ObraService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly cargando = signal(true);
  readonly errorCarga = signal<string | null>(null);
  /** La request no llegó al backend (status 0): sin red o servidor caído. */
  readonly sinConexion = signal(false);
  readonly obras = signal<Obra[]>([]);

  readonly filtroControl = this.fb.nonNullable.control('');
  private readonly filtroSignal = toSignal(
    this.filtroControl.valueChanges.pipe(debounceTime(150)),
    { initialValue: '' }
  );

  readonly obrasFiltradas = computed(() => {
    const filtro = this.filtroSignal().toLowerCase().trim();
    const todas = this.obras();
    if (!filtro) return todas;
    return todas.filter((o) =>
      o.designacion.toLowerCase().includes(filtro) ||
      o.comitenteNombre.toLowerCase().includes(filtro) ||
      o.localidad.toLowerCase().includes(filtro) ||
      `${o.calle} ${o.numero}`.toLowerCase().includes(filtro)
    );
  });

  readonly columnasMostradas = [
    'designacion',
    'comitente',
    'ubicacion',
    'provincia',
    'acciones',
  ];

  ngOnInit(): void {
    this.cargarObras();
  }

  cargarObras(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    this.obraService.listarTodas().subscribe({
      next: (datos) => {
        this.obras.set(datos);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.sinConexion.set(err.status === 0);
        this.errorCarga.set('No se pudieron cargar las obras. Intentá recargar.');
        this.cargando.set(false);
      },
    });
  }

  irANueva(): void {
    this.router.navigate(['/obras/nueva']);
  }

  irAEditar(obra: Obra): void {
    this.router.navigate(['/obras', obra.id, 'editar']);
  }

  async confirmarEliminar(obra: Obra): Promise<void> {
    const data: ConfirmDialogData = {
      titulo: 'Eliminar obra',
      mensaje: `¿Eliminar la obra "${obra.designacion}"?`,
      detalle: `Asociada a ${obra.comitenteNombre}. Esta acción no se puede deshacer.`,
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
      this.eliminar(obra);
    }
  }

  private eliminar(obra: Obra): void {
    this.obraService.eliminar(obra.id).subscribe({
      next: () => {
        this.obras.update((lista) => lista.filter((o) => o.id !== obra.id));
        this.snackBar.open(
          `"${obra.designacion}" fue eliminada.`,
          'Cerrar',
          { duration: 3000, panelClass: ['snackbar-success'] }
        );
      },
      error: (err: HttpErrorResponse) => {
        const mensaje = err.error?.mensaje || 'No se pudo eliminar la obra.';
        this.snackBar.open(mensaje, 'Cerrar', {
          duration: 5000,
          panelClass: ['snackbar-error'],
        });
      },
    });
  }
}
