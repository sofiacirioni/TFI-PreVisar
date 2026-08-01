import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Estado de error a página completa: para cuando la pantalla no pudo cargar y
 * no queda nada más que mostrar. Distinto del `.error-banner` en línea, que
 * acompaña a un formulario que sigue siendo usable.
 *
 * Separa el caso "no hay conexión" del error genérico porque son problemas
 * distintos para quien lo lee: uno se resuelve mirando la red, el otro no.
 */
@Component({
  selector: 'app-error-state',
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="pv-error-state">
      @if (sinConexion()) {
        <img src="illustrations/sin-conexion.svg" alt="" />
        <h3>Sin conexión</h3>
        <p>No pudimos comunicarnos con el servidor. Revisá tu conexión y volvé a intentar.</p>
      } @else {
        <img src="illustrations/error.svg" alt="" />
        <h3>{{ titulo() }}</h3>
        <p>{{ mensaje() }}</p>
      }

      <div class="pv-error-state__acciones">
        <button mat-flat-button (click)="reintentar.emit()">
          <mat-icon svgIcon="refresh"></mat-icon>
          Reintentar
        </button>
      </div>
    </div>
  `,
})
export class ErrorState {
  readonly titulo = input('No pudimos cargar esta pantalla');
  readonly mensaje = input('Hubo un problema al traer los datos. Probá de nuevo en un momento.');
  /** True cuando la request nunca llegó al backend (status 0). */
  readonly sinConexion = input(false);

  readonly reintentar = output<void>();
}
