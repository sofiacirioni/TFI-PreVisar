import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { AuthResponse, LoginRequest, RegisterRequest, RolUsuario } from '@core/models';
import { TokenService } from './token.service';
import { Router } from '@angular/router';
import { Observable } from 'rxjs/internal/Observable';
import { API } from '@core/constants/api.constants';
import { tap } from 'rxjs/internal/operators/tap';


/** Snapshot mínimo del usuario autenticado, derivado del token + storage. */
export interface SesionUsuario {
  email: string;
  rol: RolUsuario;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokenService = inject(TokenService);
  private readonly router = inject(Router);

  // Estado reactivo del usuario actual (signal privado, expuesto readonly)
  private readonly _sesion = signal<SesionUsuario | null>(this.restaurarSesion());

  /** Señal de solo lectura con el usuario actual (o null). */
  readonly sesion = this._sesion.asReadonly();

  /** Derivados convenientes para usar en templates con @if. */
  readonly estaAutenticado = computed(() => this._sesion() !== null);

  // --- Operaciones ---

  login(credenciales: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(API.AUTH_LOGIN, credenciales).pipe(
      tap((res) => this.guardarSesion(res))
    );
  }

  register(datos: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(API.AUTH_REGISTER, datos).pipe(
      tap((res) => this.guardarSesion(res))
    );
  }

  /** Limpia la sesión (storage + signal) SIN navegar. Útil para guards. */
  limpiarSesion(): void {
    this.tokenService.clear();
    this._sesion.set(null);
  }

  logout(): void {
    this.limpiarSesion();
    this.router.navigate(['/auth/login']);
  }

  // --- Internos ---

  private guardarSesion(res: AuthResponse): void {
    this.tokenService.setToken(res.token);
    this.tokenService.setEmail(res.email);
    this.tokenService.setRol(res.rol);
    this._sesion.set({ email: res.email, rol: res.rol });
  }

  /** Al iniciar la app, reconstruye la sesión desde el storage si el token sigue válido. */
  private restaurarSesion(): SesionUsuario | null {
    if (!this.tokenService.hasValidToken()) {
      this.tokenService.clear();
      return null;
    }
    const email = this.tokenService.getEmail();
    const rol = this.tokenService.getRol();
    if (!email || !rol) {
      this.tokenService.clear();
      return null;
    }
    return { email, rol };
  }
  
}
