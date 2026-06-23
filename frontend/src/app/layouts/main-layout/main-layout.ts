import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
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

  // Ítem visible solo para revisores
  private readonly navItemRevisor: NavItem = {
    label: 'Gestión de parámetros',
    icon: 'settings',
    route: '/admin/parametros',
  };

  // Listado final según el rol del usuario
  readonly navItems = computed<NavItem[]>(() =>
    this.esRevisor() ? [...this.navItemsBase, this.navItemRevisor] : this.navItemsBase
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

  toggleSidebar(): void {
    this.sidebarExpandido.update((v) => !v);
  }

  logout(): void {
    this.authService.logout();
  }

}
