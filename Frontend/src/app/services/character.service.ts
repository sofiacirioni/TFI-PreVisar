import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { retry, map } from 'rxjs/operators';
import { Character } from '../models/character.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CharacterService {
  private readonly baseUrl = environment.apiUrl + '/characters';
  
  // Signals para estado
  characters = signal<Character[]>([]);
  selectedCharacter = signal<Character | null>(null);
  opponent = signal<Character | null>(null);

  constructor(private http: HttpClient) {}

  getAllCharacters(): Observable<Character[]> {
    return this.http.get<any[]>(this.baseUrl).pipe(
      retry(3),
      map(arr => arr.map((r: any) => this.mapCharacter(r)))
    );
  }

  getRandomCharacter(): Observable<Character> {
    return this.http.get<any>(`${this.baseUrl}/random`).pipe(
      retry(3),
      map(obj => this.mapCharacter(obj))
    );
  }

  private mapCharacter(raw: any): Character {
    return {
      id: raw.id,
      nombre: raw.name ?? raw.nombre,
      descripcion: raw.description ?? raw.descripcion ?? '',
      ataque: raw.attack ?? raw.ataque ?? 0,
      defensa: raw.defence ?? raw.defensa ?? raw.defense ?? 0,
      velocidad: raw.velocity ?? raw.velocidad ?? 0,
      imagen_url: raw.image_url ?? raw.imagen_url ?? raw.imageUrl ?? ''
    } as Character;
  }

  loadCharacters(): void {
    this.getAllCharacters().subscribe({
      next: (characters) => this.characters.set(characters),
      error: (error) => console.error('Error loading characters:', error)
    });
  }

  selectCharacter(character: Character): void {
    this.selectedCharacter.set(character);
  }

  loadOpponent(): Observable<Character> {
    const maxAttempts = 5;
    const selectedId = this.selectedCharacter()?.id ?? null;

    return new Observable(observer => {
      const tryFetch = (attempt: number) => {
        this.getRandomCharacter().subscribe({
          next: (opponent) => {
            // Si el random devuelve el mismo personaje que eligió el jugador, reintentar
            if (opponent && opponent.id !== selectedId) {
              this.opponent.set(opponent);
              observer.next(opponent);
              observer.complete();
            } else if (attempt < maxAttempts) {
              tryFetch(attempt + 1);
            } else {
              // Fallback: obtener la lista completa y elegir una distinta
              this.getAllCharacters().subscribe({
                next: (list) => {
                  const alternative = list.find(c => c.id !== selectedId);
                  if (alternative) {
                    this.opponent.set(alternative);
                    observer.next(alternative);
                    observer.complete();
                  } else {
                    observer.error(new Error('No se encontró un oponente alternativo distinto al jugador'));
                  }
                },
                error: (err) => observer.error(err)
              });
            }
          },
          error: (err) => {
            if (attempt < maxAttempts) {
              tryFetch(attempt + 1);
            } else {
              observer.error(err);
            }
          }
        });
      };

      tryFetch(1);
    });
  }

  reset(): void {
    this.selectedCharacter.set(null);
    this.opponent.set(null);
  }
}

