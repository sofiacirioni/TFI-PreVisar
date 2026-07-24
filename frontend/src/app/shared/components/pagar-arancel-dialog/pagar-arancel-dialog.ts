import { Component, inject, signal } from '@angular/core';
import { Clipboard } from '@angular/cdk/clipboard';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExpedienteService } from '../../../core/services/expediente.service';

export interface PagarArancelData {
  expedienteId: number;
  expedienteNombre?: string;
  /** Email del comitente para autocompletar el destinatario. Si no se conoce, queda vacío. */
  comitenteEmail?: string;
}

/**
 * Diálogo único del arancel: primero el profesional elige "Pagar ahora" (redirige a
 * MP) o "Compartir link" (muestra el initPoint para mandárselo al comitente, que paga
 * desde MP sin entrar a la app). Ambas opciones generan la misma preferencia.
 */
@Component({
  selector: 'app-pagar-arancel-dialog',
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './pagar-arancel-dialog.html',
  styleUrl: './pagar-arancel-dialog.scss',
})
export class PagarArancelDialog {
  private readonly expedienteService = inject(ExpedienteService);
  private readonly clipboard = inject(Clipboard);
  readonly data = inject<PagarArancelData>(MAT_DIALOG_DATA);

  readonly modo = signal<'elegir' | 'compartir'>('elegir');
  readonly cargando = signal(false);
  readonly error = signal(false);
  readonly initPoint = signal<string | null>(null);
  readonly copiado = signal(false);

  pagarAhora(): void {
    this.pedirPreferencia((initPoint) => {
      window.location.href = initPoint; // sale de la app hacia MP
    });
  }

  compartir(): void {
    this.pedirPreferencia((initPoint) => {
      this.initPoint.set(initPoint);
      this.modo.set('compartir');
    });
  }

  private pedirPreferencia(onOk: (initPoint: string) => void): void {
    if (this.cargando()) return;
    this.cargando.set(true);
    this.error.set(false);
    this.expedienteService.iniciarPago(this.data.expedienteId).subscribe({
      next: (pref) => {
        this.cargando.set(false);
        onOk(pref.initPoint);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set(true);
      },
    });
  }

  copiar(): void {
    const link = this.initPoint();
    if (link && this.clipboard.copy(link)) {
      this.copiado.set(true);
      setTimeout(() => this.copiado.set(false), 2500);
    }
  }

  get whatsappUrl(): string {
    const suffix = this.data.expedienteNombre ? ` ${this.data.expedienteNombre}` : '';
    const msg = `Hola, te comparto el link para pagar el arancel del expediente${suffix}:\n${this.initPoint()}`;
    return `https://wa.me/?text=${encodeURIComponent(msg)}`;
  }

  /**
   * Abre el cliente de correo con asunto, cuerpo (con el link) y destinatario
   * precargados. Si no se conoce el email del comitente, el "to" queda vacío para
   * que lo complete el profesional.
   */
  get mailtoUrl(): string {
    const suffix = this.data.expedienteNombre ? ` ${this.data.expedienteNombre}` : '';
    const asunto = `Pago del arancel del expediente${suffix}`;
    const cuerpo =
      `Hola,\n\n` +
      `Te comparto el link para pagar el arancel del expediente${suffix} desde Mercado Pago:\n` +
      `${this.initPoint()}\n\n` +
      `No necesitás acceso a la aplicación.\n\nSaludos.`;
    const para = this.data.comitenteEmail ?? '';
    return `mailto:${para}?subject=${encodeURIComponent(asunto)}&body=${encodeURIComponent(cuerpo)}`;
  }
}
