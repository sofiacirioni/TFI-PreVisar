import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';
import { filter, map, startWith } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { ProfesionalService } from '../../core/services/profesional.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatListModule,
    MatMenuModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly profesionalService = inject(ProfesionalService);
  private readonly router = inject(Router);

  // Estado del sidebar: expandido (texto + íconos) o colapsado (solo íconos)
  readonly sidebarExpandido = signal(true);

  /**
   * En pantallas chicas el sidebar no puede quedarse fijo: con `mode="side"`
   * empujaba el contenido fuera del viewport y la app quedaba inutilizable.
   * Debajo de 900px pasa a ser un cajón superpuesto, cerrado por defecto.
   */
  readonly esMovil = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 900px)')
      .pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  /** Solo aplica en móvil: en escritorio el sidebar está siempre visible. */
  readonly cajonAbierto = signal(false);

  /**
   * El cajón superpuesto se muestra siempre completo: colapsarlo a solo íconos
   * no tiene sentido cuando ya tapa la pantalla.
   */
  readonly mostrarEtiquetas = computed(() => this.esMovil() || this.sidebarExpandido());

  // El armado de expediente es la vista compleja: usa todo el ancho (sin el cap
  // de --content-max), el resto de las vistas quedan centradas y legibles.
  readonly anchoCompleto = toSignal(
    this.router.events.pipe(
      filter((e) => e instanceof NavigationEnd),
      map(() => this.router.url.includes('/armado')),
      startWith(this.router.url.includes('/armado')),
    ),
    { initialValue: this.router.url.includes('/armado') },
  );

  // Sesión actual (signal del AuthService)
  readonly sesion = this.authService.sesion;

  // Perfil del profesional logueado: usado para mostrar ítems según el rol
  private readonly perfil = this.profesionalService.perfilActual;
  private readonly esRevisor = computed(() => this.perfil()?.esRevisor ?? false);

  private readonly navItemsBase: NavItem[] = [
    { label: 'Dashboard', icon: 'dashboard', route: '/dashboard' },
    { label: 'Mis comitentes', icon: 'people', route: '/comitentes' },
    { label: 'Mis obras', icon: 'home_work', route: '/obras' },
    { label: 'Mis expedientes', icon: 'description', route: '/expedientes' },
    { label: 'Mi perfil', icon: 'person', route: '/profile' },
  ];

  // Ítems visibles solo para revisores
  private readonly navItemsRevisor: NavItem[] = [
    { label: 'Revisar', icon: 'fact_check', route: '/revisar' },
    { label: 'Gestión de parámetros', icon: 'settings', route: '/admin/parametros' },
  ];

  // Listado final según el rol del usuario
  readonly navItems = computed<NavItem[]>(() =>
    this.esRevisor() ? [...this.navItemsBase, ...this.navItemsRevisor] : this.navItemsBase
  );

  ngOnInit(): void {
    // Cargamos el perfil una sola vez para conocer el rol (revisor o no).
    // Si ya está cacheado, evitamos la llamada.
    if (!this.perfil()) {
      this.profesionalService.cargarPerfil().subscribe({
        error: () => {
          // No bloqueamos el layout si falla: simplemente no se muestran
          // los ítems condicionados al rol.
        },
      });
    }
  }

  constructor() {
    // Al navegar en móvil el cajón se cierra solo: si no, tapa la pantalla a la
    // que acabás de entrar.
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(() => this.cajonAbierto.set(false));
  }

  /**
   * En escritorio alterna expandido/colapsado; en móvil abre y cierra el cajón,
   * donde "colapsado" no tiene sentido porque se muestra superpuesto.
   */
  toggleSidebar(): void {
    if (this.esMovil()) this.cajonAbierto.update((v) => !v);
    else this.sidebarExpandido.update((v) => !v);
  }

  logout(): void {
    this.authService.logout();
  }

}
