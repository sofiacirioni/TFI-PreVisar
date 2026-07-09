import { Component, inject, signal } from '@angular/core';
import { Clipboard } from '@angular/cdk/clipboard';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

export interface CompartirPagoData {
  initPoint: string;
  expedienteNombre?: string;
}

/**
 * Muestra el link de pago (initPoint de MP) para que el profesional lo copie o lo
 * mande al comitente. El comitente paga desde la interfaz de MP; no entra a la app.
 */
@Component({
  selector: 'app-compartir-pago-dialog',
  imports: [MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  templateUrl: './compartir-pago-dialog.html',
  styleUrl: './compartir-pago-dialog.scss',
})
export class CompartirPagoDialog {
  private readonly clipboard = inject(Clipboard);
  readonly data = inject<CompartirPagoData>(MAT_DIALOG_DATA);

  readonly copiado = signal(false);

  copiar(): void {
    if (this.clipboard.copy(this.data.initPoint)) {
      this.copiado.set(true);
      setTimeout(() => this.copiado.set(false), 2500);
    }
  }

  get whatsappUrl(): string {
    const suffix = this.data.expedienteNombre ? ` ${this.data.expedienteNombre}` : '';
    const msg = `Hola, te comparto el link para pagar el arancel del expediente${suffix}:\n${this.data.initPoint}`;
    return `https://wa.me/?text=${encodeURIComponent(msg)}`;
  }
}
