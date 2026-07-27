import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs/internal/Observable';
import { forkJoin } from 'rxjs/internal/observable/forkJoin';
import { firstValueFrom } from 'rxjs/internal/firstValueFrom';
import { EstructuraService } from '../../../core/services/estructura.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import {
  DocumentoRequerido,
  DocumentoRequeridoRequest,
  SeccionEstructura,
} from '../../../core/models/estructura.model';
import { TipoTarea } from '../../../core/models/catalogos.model';
import {
  ConfirmDialog,
  ConfirmDialogData,
} from '../../../shared/components/confirm-dialog/confirm-dialog';
import { DocumentoDialog, DocumentoDialogData } from './documento-dialog';

@Component({
  selector: 'app-gestion-estructura',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './gestion-estructura.html',
  styleUrl: './gestion-estructura.scss',
})
export class GestionEstructura implements OnInit {
  private readonly estructuraService = inject(EstructuraService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly cargando = signal(true);
  readonly errorCarga = signal<string | null>(null);
  readonly trabajando = signal(false); // bloquea acciones mientras hay una operación en curso
  readonly tiposTarea = signal<TipoTarea[]>([]);
  readonly secciones = signal<SeccionEstructura[]>([]);

  readonly tipoTareaCtrl = new FormControl<number | null>(null);

  ngOnInit(): void {
    this.catalogoService.listarTiposTarea().subscribe({
      next: (tipos) => {
        this.tiposTarea.set(tipos);
        if (tipos.length) {
          this.tipoTareaCtrl.setValue(tipos[0].id);
          this.cargarEstructura();
        } else {
          this.cargando.set(false);
          this.errorCarga.set('No hay tipos de tarea configurados.');
        }
      },
      error: () => {
        this.cargando.set(false);
        this.errorCarga.set('No se pudieron cargar los tipos de tarea. Recargá la página.');
      },
    });

    this.tipoTareaCtrl.valueChanges.subscribe(() => this.cargarEstructura());
  }

  private tipoTareaId(): number | null {
    return this.tipoTareaCtrl.value;
  }

  cargarEstructura(): void {
    const id = this.tipoTareaId();
    if (id == null) return;
    this.cargando.set(true);
    this.errorCarga.set(null);
    this.estructuraService.getEstructuraMia(id).subscribe({
      next: (est) => {
        this.secciones.set(est.secciones);
        this.cargando.set(false);
      },
      error: () => {
        this.errorCarga.set('No se pudo cargar la estructura. Recargá la página.');
        this.cargando.set(false);
      },
    });
  }

  async agregar(seccion: SeccionEstructura): Promise<void> {
    const data: DocumentoDialogData = { modo: 'crear', seccionNombre: seccion.nombre };
    const body = await this.abrirDialogoDocumento(data);
    if (!body) return;
    this.ejecutar(this.estructuraService.crearDocumento(seccion.id, body), 'Documento agregado');
  }

  async editar(seccion: SeccionEstructura, doc: DocumentoRequerido): Promise<void> {
    const data: DocumentoDialogData = {
      modo: 'editar',
      seccionNombre: seccion.nombre,
      documento: { codigo: doc.codigo, nombre: doc.nombre, obligatorio: doc.obligatorio },
    };
    const body = await this.abrirDialogoDocumento(data);
    if (!body) return;
    this.ejecutar(this.estructuraService.actualizarDocumento(doc.id, body), 'Documento actualizado');
  }

  async quitar(doc: DocumentoRequerido): Promise<void> {
    const data: ConfirmDialogData = {
      titulo: 'Quitar documento',
      mensaje: `¿Quitar "${doc.nombre}" de la estructura?`,
      detalle:
        'Este cambio afecta a los expedientes de toda la provincia. Los archivos ya cargados se conservan.',
      textoConfirmar: 'Quitar',
      textoCancelar: 'Cancelar',
      variant: 'destructive',
    };
    const ref = this.dialog.open<ConfirmDialog, ConfirmDialogData, boolean>(ConfirmDialog, {
      data,
      width: '440px',
    });
    if (await firstValueFrom(ref.afterClosed())) {
      this.ejecutar(this.estructuraService.eliminarDocumento(doc.id), 'Documento quitado');
    }
  }

  /** Reordena por flechas: intercambia el orden con el vecino (dos updates). */
  mover(seccion: SeccionEstructura, index: number, dir: -1 | 1): void {
    const docs = seccion.documentos;
    const j = index + dir;
    if (j < 0 || j >= docs.length || this.trabajando()) return;
    const a = docs[index];
    const b = docs[j];
    this.trabajando.set(true);
    forkJoin([
      this.estructuraService.actualizarDocumento(a.id, this.toReq(a, b.orden)),
      this.estructuraService.actualizarDocumento(b.id, this.toReq(b, a.orden)),
    ]).subscribe({
      next: () => {
        this.trabajando.set(false);
        this.cargarEstructura();
      },
      error: (err: HttpErrorResponse) => {
        this.trabajando.set(false);
        this.aviso(err.error?.mensaje ?? 'No se pudo reordenar.');
      },
    });
  }

  private toReq(doc: DocumentoRequerido, orden: number): DocumentoRequeridoRequest {
    return { codigo: doc.codigo, nombre: doc.nombre, obligatorio: doc.obligatorio, orden };
  }

  private async abrirDialogoDocumento(
    data: DocumentoDialogData,
  ): Promise<DocumentoRequeridoRequest | undefined> {
    const ref = this.dialog.open<DocumentoDialog, DocumentoDialogData, DocumentoRequeridoRequest>(
      DocumentoDialog,
      { data, width: '460px' },
    );
    return firstValueFrom(ref.afterClosed());
  }

  private ejecutar(obs: Observable<unknown>, exito: string): void {
    this.trabajando.set(true);
    obs.subscribe({
      next: () => {
        this.trabajando.set(false);
        this.aviso(exito, 'success');
        this.cargarEstructura();
      },
      error: (err: HttpErrorResponse) => {
        this.trabajando.set(false);
        this.aviso(err.error?.mensaje ?? 'No se pudo completar la operación.');
      },
    });
  }

  private aviso(mensaje: string, tipo: 'success' | 'error' = 'error'): void {
    this.snackBar.open(mensaje, 'Cerrar', {
      duration: tipo === 'success' ? 3000 : 5000,
      panelClass: [tipo === 'success' ? 'snackbar-success' : 'snackbar-error'],
    });
  }
}
