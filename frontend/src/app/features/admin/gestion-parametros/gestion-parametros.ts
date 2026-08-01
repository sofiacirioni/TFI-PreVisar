import { ErrorState } from '../../../shared/components/error-state/error-state';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ParametroAporteService } from '@core/services/parametro-aporte.service';
import { ArancelVigente } from '@core/models/aportes.model';
import { GestionEstructura } from '../gestion-estructura/gestion-estructura';

@Component({
  selector: 'app-gestion-parametros',
  imports: [
    ErrorState,
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    GestionEstructura,
  ],
  templateUrl: './gestion-parametros.html',
  styleUrl: './gestion-parametros.scss',
})
export class GestionParametros implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly parametroAporteService = inject(ParametroAporteService);
  private readonly snackBar = inject(MatSnackBar);

  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly errorCarga = signal<string | null>(null);
  /** La request no llegó al backend (status 0): sin red o servidor caído. */
  readonly sinConexion = signal(false);
  readonly arancelVigente = signal<ArancelVigente | null>(null);

  readonly form = this.fb.nonNullable.group({
    valor: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  ngOnInit(): void {
    this.cargarArancel();
  }

  private cargarArancel(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    this.parametroAporteService.obtenerArancelVigente().subscribe({
      next: (arancel) => {
        this.arancelVigente.set(arancel);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.sinConexion.set(err.status === 0);
        this.errorCarga.set('No se pudo cargar el arancel vigente. Recargá la página.');
        this.cargando.set(false);
      },
    });
  }

  guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }

    const valor = this.form.getRawValue().valor!;
    this.guardando.set(true);

    this.parametroAporteService.actualizarArancel({ valor }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.form.reset();
        this.snackBar.open('Arancel actualizado correctamente', 'Cerrar', {
          duration: 3000,
          panelClass: ['snackbar-success'],
        });
        this.cargarArancel();
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        const mensaje = err.error?.mensaje ?? 'No se pudo actualizar el arancel';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      },
    });
  }
}
