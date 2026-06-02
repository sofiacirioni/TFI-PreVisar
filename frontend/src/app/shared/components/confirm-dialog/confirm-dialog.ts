import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export type ConfirmDialogVariant = 'destructive' | 'warning' | 'info';

export interface ConfirmDialogData {
  /** Título del diálogo. Ej: "Eliminar comitente" */
  titulo: string;
  /** Mensaje principal. Soporta texto plano. */
  mensaje: string;
  /** Detalle adicional opcional (segunda línea, en gris). */
  detalle?: string;
  /** Texto del botón de confirmación. Default: "Confirmar". */
  textoConfirmar?: string;
  /** Texto del botón de cancelación. Default: "Cancelar". */
  textoCancelar?: string;
  /** Estilo visual. 'destructive' = botón rojo, 'warning' = naranja, 'info' = cyan. Default: 'info'. */
  variant?: ConfirmDialogVariant;
}

@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './confirm-dialog.html',
  styleUrl: './confirm-dialog.scss',
})
export class ConfirmDialog {
  private readonly dialogRef = inject(MatDialogRef<ConfirmDialog, boolean>);
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);

  readonly variant: ConfirmDialogVariant = this.data.variant ?? 'info';
  readonly textoConfirmar = this.data.textoConfirmar ?? 'Confirmar';
  readonly textoCancelar = this.data.textoCancelar ?? 'Cancelar';

  get iconoVariant(): string {
    switch (this.variant) {
      case 'destructive': return 'delete_forever';
      case 'warning': return 'warning';
      case 'info':
      default: return 'help_outline';
    }
  }

  confirmar(): void {
    this.dialogRef.close(true);
  }

  cancelar(): void {
    this.dialogRef.close(false);
  }

}
