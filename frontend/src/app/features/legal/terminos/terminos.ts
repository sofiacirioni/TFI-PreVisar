import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TERMINOS_VERSION, TERMINOS_FECHA, EMAIL_AUTORA, EMAIL_CIEC } from '../legal.constants';

/**
 * Página pública de Términos y Condiciones de uso.
 *
 * Es pública (fuera de authGuard/guestGuard): la enlaza la casilla del registro,
 * cuando el usuario aún no tiene sesión, y también debe poder consultarse ya logueado.
 *
 * La versión vigente ({@link TERMINOS_VERSION}) es la que se le muestra al usuario
 * al aceptar. Cuando se implemente la persistencia de la aceptación, guardar esa
 * misma constante junto al usuario permite auditar qué versión aceptó cada uno.
 */
@Component({
  selector: 'app-terminos',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  templateUrl: './terminos.html',
  styleUrl: './terminos.scss',
})
export class Terminos {
  readonly version = TERMINOS_VERSION;
  readonly fecha = TERMINOS_FECHA;
  readonly emailAutora = EMAIL_AUTORA;
  readonly emailCiec = EMAIL_CIEC;
}
