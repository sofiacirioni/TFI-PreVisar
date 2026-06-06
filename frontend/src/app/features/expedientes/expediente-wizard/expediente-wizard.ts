import { Component, inject, OnInit, signal } from '@angular/core';
import { ExpedienteService } from '../../../core/services/expediente.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { switchMap, tap } from 'rxjs/operators';
import { Observable, of, throwError } from 'rxjs';
import { ExpedienteRequest, ExpedienteResponse } from '../../../core/models/expediente.model';

@Component({
  selector: 'app-expediente-wizard',
  imports: [
    ReactiveFormsModule,
    MatStepperModule,
    MatButtonModule,
    MatProgressBarModule,
  ],
  templateUrl: './expediente-wizard.html',
  styleUrl: './expediente-wizard.scss',
})
export class ExpedienteWizard implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly expedienteService = inject(ExpedienteService);

  // Estado del wizard (vive en el componente, no en el service stateless)
  readonly expedienteId = signal<number | null>(null);
  readonly expediente = signal<ExpedienteResponse | null>(null);
  readonly guardando = signal(false);
  readonly cargando = signal(false);

  // Un FormGroup por paso
  readonly tareaForm = this.fb.group({
    tipoTareaId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly comitenteForm = this.fb.group({
    comitenteId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly obraForm = this.fb.group({
    obraId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly economicoForm = this.fb.group({
    honorariosReferenciales: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return; // modo "nuevo": el borrador se crea recien al primer guardado

    // modo "retomar": cargo el borrador existente y precargo los forms
    this.cargando.set(true);
    this.expedienteService.obtener(Number(idParam)).subscribe({
      next: (exp) => {
        this.expedienteId.set(exp.id);
        this.expediente.set(exp);
        this.tareaForm.patchValue({ tipoTareaId: exp.tipoTareaId });
        this.obraForm.patchValue({ obraId: exp.obraId });
        this.economicoForm.patchValue({ honorariosReferenciales: exp.honorariosReferenciales });
        // comitenteId se resolvera al armar el paso de obra (obra -> comitente)
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.snackBar.open('No se encontró el expediente', 'Cerrar', { duration: 4000 });
        this.router.navigate(['/expedientes']);
      },
    });
  }

  onStepChange(_event: StepperSelectionEvent): void {
    this.guardando.set(true);
    this.guardarParcial().subscribe({
      next: () => this.guardando.set(false),
      error: () => {
        this.guardando.set(false);
        this.snackBar.open('No se pudo guardar el progreso', 'Cerrar', { duration: 4000 });
      },
    });
  }

  finalizar(): void {
    this.guardando.set(true);
    // Garantizo que lo ultimo este guardado ANTES de completar
    this.guardarParcial()
      .pipe(
        switchMap((resp) => {
          const id = resp?.id ?? this.expedienteId();
          if (id === null) return throwError(() => new Error('SIN_DATOS'));
          return this.expedienteService.completar(id);
        }),
      )
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.snackBar.open('Expediente finalizado', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/expedientes']);
        },
        error: (err) => {
          this.guardando.set(false);
          if (err?.message === 'SIN_DATOS') {
            this.snackBar.open('Cargá al menos un dato antes de finalizar', 'Cerrar', { duration: 4000 });
            return;
          }
          // El back devuelve 400 con el detalle de que falta
          const msg = err?.error?.message ?? 'No se pudo finalizar el expediente';
          this.snackBar.open(msg, 'Cerrar', { duration: 6000 });
        },
      });
  }

  /** Crea el borrador (primera vez) o lo actualiza parcial. Devuelve el expediente persistido. */
  private guardarParcial(): Observable<ExpedienteResponse | null> {
    const req = this.construirRequest();
    if (this.expedienteId() === null && this.estaVacio(req)) {
      return of(null); // nada que guardar todavia, no creo un borrador vacio
    }
    const id = this.expedienteId();
    const obs$ = id === null
      ? this.expedienteService.crear(req)
      : this.expedienteService.actualizarParcial(id, req);

    return obs$.pipe(
      tap((resp) => {
        this.expedienteId.set(resp.id);
        this.expediente.set(resp);
      }),
    );
  }

  private construirRequest(): ExpedienteRequest {
    return {
      tipoTareaId: this.tareaForm.controls.tipoTareaId.value,
      obraId: this.obraForm.controls.obraId.value,
      honorariosReferenciales: this.economicoForm.controls.honorariosReferenciales.value,
    };
  }

  private estaVacio(req: ExpedienteRequest): boolean {
    return req.tipoTareaId == null && req.obraId == null && req.honorariosReferenciales == null;
  }
}
