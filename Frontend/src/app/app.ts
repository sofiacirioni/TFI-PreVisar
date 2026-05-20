import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CharacterService } from './services/character.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  title = 'Mortal Kombat - Match Game';
  private characterService = inject(CharacterService);
  private router = inject(Router);

  // Exponer señal para la plantilla
  selectedCharacter = this.characterService.selectedCharacter;
  loadingStart = signal<boolean>(false);

  // No route-aware logic here; control moved to CharactersComponent

  // start logic moved to CharactersComponent
}
