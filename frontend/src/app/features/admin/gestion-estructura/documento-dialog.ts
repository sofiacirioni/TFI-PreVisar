import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { DocumentoRequeridoRequest } from '../../../core/models/estructura.model';

export interface DocumentoDialogData {
  modo: 'crear' | 'editar';
  seccionNombre: string;
  documento?: {
    codigo: string;
    nombre: string;
    obligatorio: boolean;
    permiteMultiples: boolean;
    generable: boolean;
    validaA4: boolean;
  };
}

/** Alta/edición de un documento requerido. El código es clave estable: al editar no se cambia. */
@Component({
  selector: 'app-documento-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>
      {{ data.modo === 'crear' ? 'Agregar documento' : 'Editar documento' }}
    </h2>
    <mat-dialog-content>
      <p class="dlg__seccion">Sección: <strong>{{ data.seccionNombre }}</strong></p>
      <form [formGroup]="form" class="dlg__form">
        <mat-form-field appearance="outline" class="dlg__campo">
          <mat-label>Código</mat-label>
          <input matInput formControlName="codigo" maxlength="40" />
          @if (data.modo === 'editar') {
            <mat-hint>El código no se modifica.</mat-hint>
          } @else {
            <mat-hint>Identificador estable (ej. CONTRATO_LOCACION).</mat-hint>
          }
          @if (form.controls.codigo.touched && form.controls.codigo.errors?.['required']) {
            <mat-error>Obligatorio</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="dlg__campo">
          <mat-label>Nombre</mat-label>
          <input matInput formControlName="nombre" maxlength="160" />
          @if (form.controls.nombre.touched && form.controls.nombre.errors?.['required']) {
            <mat-error>Obligatorio</mat-error>
          }
        </mat-form-field>

        <div class="dlg__flags">
          <mat-checkbox formControlName="obligatorio">Documento obligatorio</mat-checkbox>

          <mat-checkbox formControlName="permiteMultiples">
            Admite varios archivos
            <span class="dlg__ayuda">— comprobantes, planos, anexos.</span>
          </mat-checkbox>

          <mat-checkbox formControlName="generable">
            El sistema genera el PDF
            <span class="dlg__ayuda">— solo para ranuras con plantilla propia (carátula, contrato).</span>
          </mat-checkbox>

          <mat-checkbox formControlName="validaA4">
            Se espera en A4
            <span class="dlg__ayuda">— desmarcalo en planos de gran formato (A1/A3).</span>
          </mat-checkbox>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="primary" (click)="guardar()">
        {{ data.modo === 'crear' ? 'Agregar' : 'Guardar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .dlg__seccion {
        margin: 0 0 12px;
        color: #5a6b78;
        font-size: 0.85rem;
      }
      .dlg__form {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 340px;
      }
      .dlg__campo {
        width: 100%;
      }
      .dlg__flags {
        display: flex;
        flex-direction: column;
        gap: 10px;
        margin-top: 4px;
      }
      .dlg__ayuda {
        color: #5a6b78;
        font-size: 0.8rem;
      }
    `,
  ],
})
export class DocumentoDialog {
  private readonly fb = inject(FormBuilder);
  private readonly ref = inject(MatDialogRef<DocumentoDialog, DocumentoRequeridoRequest>);
  readonly data = inject<DocumentoDialogData>(MAT_DIALOG_DATA);

  readonly form = this.fb.nonNullable.group({
    codigo: [
      { value: this.data.documento?.codigo ?? '', disabled: this.data.modo === 'editar' },
      [Validators.required, Validators.maxLength(40)],
    ],
    nombre: [this.data.documento?.nombre ?? '', [Validators.required, Validators.maxLength(160)]],
    obligatorio: [this.data.documento?.obligatorio ?? true],
    // Defaults del alta espejados del backend (ver DocumentoRequeridoRequestDto).
    permiteMultiples: [this.data.documento?.permiteMultiples ?? false],
    generable: [this.data.documento?.generable ?? false],
    validaA4: [this.data.documento?.validaA4 ?? true],
  });

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // getRawValue incluye el código aunque esté disabled (en edición).
    const { codigo, nombre, obligatorio, permiteMultiples, generable, validaA4 } =
      this.form.getRawValue();
    this.ref.close({
      codigo: codigo.trim(),
      nombre: nombre.trim(),
      obligatorio,
      permiteMultiples,
      generable,
      validaA4,
    });
  }
}
