import { CommonModule } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProfesionalService } from '../../core/services/profesional.service';

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule, 
    MatIconModule, 
    MatTooltipModule
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly profesionalService = inject(ProfesionalService);

  readonly perfil = this.profesionalService.perfilActual;

  readonly saludo = computed(() => {
    const hora = new Date().getHours();
    if (hora < 12) return 'Buen día';
    if (hora < 19) return 'Buenas tardes';
    return 'Buenas noches';
  });

}
