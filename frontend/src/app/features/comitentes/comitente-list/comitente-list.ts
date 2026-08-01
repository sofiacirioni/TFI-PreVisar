import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ComitenteService } from '../../../core/services/comitente.service';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Comitente } from '../../../core/models/comitente.model';
import { debounceTime } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog';
import { firstValueFrom } from 'rxjs/internal/firstValueFrom';

@Component({
  selector: 'app-comitente-list',
  imports: [
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
  templateUrl: './comitente-list.html',
  styleUrl: './comitente-list.scss',
})
export class ComitenteList implements OnInit {
  private readonly comitenteService = inject(ComitenteService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  // Estado UI
  readonly cargando = signal(true);
  readonly errorCarga = signal<string | null>(null);

  // Datos
  readonly comitentes = signal<Comitente[]>([]);

  // Filtro de búsqueda local
  readonly filtroControl = this.fb.nonNullable.control('');
  private readonly filtroSignal = toSignal(
    this.filtroControl.valueChanges.pipe(debounceTime(150)),
    { initialValue: '' }
  );

  // Lista filtrada (computed): filtra localmente por nombre/razón social, DNI/CUIT o email
  readonly comitentesFiltrados = computed(() => {
    const filtro = this.filtroSignal().toLowerCase().trim();
    const todos = this.comitentes();
    if (!filtro) return todos;
    return todos.filter((c) =>
      c.nombreRazonSocial.toLowerCase().includes(filtro) ||
      c.dniCuit.toLowerCase().includes(filtro) ||
      (c.email?.toLowerCase().includes(filtro) ?? false)
    );
  });

  readonly columnasMostradas = [
    'tipo',
    'nombreRazonSocial',
    'dniCuit',
    'email',
    'telefono',
    'acciones',
  ];

  ngOnInit(): void {
    this.cargarComitentes();
  }

  cargarComitentes(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    this.comitenteService.listar().subscribe({
      next: (datos) => {
        this.comitentes.set(datos);
        this.cargando.set(false);
      },
      error: () => {
        this.errorCarga.set('No se pudieron cargar los comitentes. Intentá recargar.');
        this.cargando.set(false);
      },
    });
  }

  irANuevo(): void {
    this.router.navigate(['/comitentes/nuevo']);
  }

  irADetalle(comitente: Comitente): void {
    this.router.navigate(['/comitentes', comitente.id]);
  }

  irAEditar(comitente: Comitente): void {
    this.router.navigate(['/comitentes', comitente.id, 'editar']);
  }

  async confirmarEliminar(comitente: Comitente): Promise<void> {
  const data: ConfirmDialogData = {
    titulo: 'Eliminar comitente',
    mensaje: `¿Estás seguro de que querés eliminar a "${comitente.nombreRazonSocial}"?`,
    detalle: 'Sus obras dejarán de ser visibles. Esta acción no se puede deshacer.',
    textoConfirmar: 'Eliminar',
    textoCancelar: 'Cancelar',
    variant: 'destructive',
  };

  const ref = this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(
    ConfirmDialog,
    {
      data,
      width: '420px',
      disableClose: false, // permite cerrar con Escape o click afuera
    }
  );

  const confirmado = await firstValueFrom(ref.afterClosed());
  if (confirmado) {
    this.eliminar(comitente);
  }
}

  private eliminar(comitente: Comitente): void {
    this.comitenteService.eliminar(comitente.id).subscribe({
      next: () => {
        // Quitamos el comitente eliminado de la lista localmente
        this.comitentes.update((lista) => lista.filter((c) => c.id !== comitente.id));
        this.snackBar.open(
          `"${comitente.nombreRazonSocial}" fue eliminado.`,
          'Cerrar',
          { duration: 3000, panelClass: ['snackbar-success'] }
        );
      },
      error: (err: HttpErrorResponse) => {
        const mensaje = err.error?.mensaje || 'No se pudo eliminar el comitente.';
        this.snackBar.open(mensaje, 'Cerrar', {
          duration: 5000,
          panelClass: ['snackbar-error'],
        });
      },
    });
  }

}
