import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PRIVACIDAD_VERSION, PRIVACIDAD_FECHA, EMAIL_AUTORA } from '../legal.constants';

/**
 * Página pública de Política de Privacidad.
 *
 * Es pública (fuera de authGuard/guestGuard): la enlazan la casilla del registro y
 * los Términos y Condiciones (de los que forma parte), y también debe poder
 * consultarse ya logueado.
 */
@Component({
  selector: 'app-privacidad',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './privacidad.html',
  styleUrl: './privacidad.scss',
})
export class Privacidad {
  readonly version = PRIVACIDAD_VERSION;
  readonly fecha = PRIVACIDAD_FECHA;
  readonly emailAutora = EMAIL_AUTORA;
}
