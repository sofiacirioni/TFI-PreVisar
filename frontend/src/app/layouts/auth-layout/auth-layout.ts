import { Component, inject } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs/operators';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.scss',
})
export class AuthLayout {
  private readonly router = inject(Router);

  /**
   * La ilustración lateral es solo para el login. El registro tiene un
   * formulario largo: al lado de una imagen fija quedaría desbalanceado y
   * obligaría a scrollear con media pantalla vacía.
   */
  readonly esLogin = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url.includes('/login')),
      startWith(this.router.url.includes('/login')),
    ),
    { initialValue: this.router.url.includes('/login') },
  );
}
