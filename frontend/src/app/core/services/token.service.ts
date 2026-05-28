import { Injectable } from '@angular/core';
import { STORAGE_KEYS } from '@core/constants/storage.constants';
import { RolUsuario } from '@core/models/auth.model';

@Injectable({
  providedIn: 'root',
})
export class TokenService {
  // --- Token ---

  getToken(): string | null {
    return localStorage.getItem(STORAGE_KEYS.AUTH_TOKEN);
  }

  setToken(token: string): void {
    localStorage.setItem(STORAGE_KEYS.AUTH_TOKEN, token);
  }

  // --- Datos auxiliares del usuario ---

  getEmail(): string | null {
    return localStorage.getItem(STORAGE_KEYS.AUTH_EMAIL);
  }

  setEmail(email: string): void {
    localStorage.setItem(STORAGE_KEYS.AUTH_EMAIL, email);
  }

  getRol(): RolUsuario | null {
    return localStorage.getItem(STORAGE_KEYS.AUTH_ROL) as RolUsuario | null;
  }

  setRol(rol: RolUsuario): void {
    localStorage.setItem(STORAGE_KEYS.AUTH_ROL, rol);
  }

  // --- Limpieza ---

  clear(): void {
    localStorage.removeItem(STORAGE_KEYS.AUTH_TOKEN);
    localStorage.removeItem(STORAGE_KEYS.AUTH_EMAIL);
    localStorage.removeItem(STORAGE_KEYS.AUTH_ROL);
  }

  // --- Verificación de expiración ---

  /**
   * Decodifica el payload del JWT (sin verificar la firma, eso lo hace el backend)
   * y revisa el claim `exp`. Devuelve true si el token está vencido o es inválido.
   */
  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) {
      return true;
    }

    try {
      const payload = this.decodePayload(token);
      if (!payload.exp) {
        return true;
      }
      // exp viene en segundos (estándar JWT), Date.now() en milisegundos
      const expiresAtMs = payload.exp * 1000;
      return Date.now() >= expiresAtMs;
    } catch {
      // Si no se puede decodificar, lo tratamos como inválido
      return true;
    }
  }

  hasValidToken(): boolean {
    return this.getToken() !== null && !this.isTokenExpired();
  }

  /**
   * Decodifica el payload (segunda parte) de un JWT.
   * NO valida la firma — solo lee los claims. La validación real es del backend.
   */
  private decodePayload(token: string): { exp?: number; sub?: string; [key: string]: unknown } {
    const parts = token.split('.');
    if (parts.length !== 3) {
      throw new Error('Token JWT mal formado');
    }
    // base64url → base64 → JSON
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const decoded = atob(base64);
    return JSON.parse(decoded);
  }
  
}
