import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DiceResponse } from '../models/dice-response.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DiceService {
  private readonly baseUrl = environment.apiUrl + '/v1/dice';
  
  // Signals para dados
  playerDice = signal<DiceResponse | null>(null);
  opponentDice = signal<DiceResponse | null>(null);

  constructor(private http: HttpClient) {}

  rollDice(): Observable<DiceResponse> {
    return this.http.get<DiceResponse>(this.baseUrl);
  }

  rollPlayerDice(): void {
    this.rollDice().subscribe({
      next: (dice) => this.playerDice.set(dice),
      error: (error) => console.error('Error rolling player dice:', error)
    });
  }

  rollOpponentDice(): void {
    this.rollDice().subscribe({
      next: (dice) => this.opponentDice.set(dice),
      error: (error) => console.error('Error rolling opponent dice:', error)
    });
  }

  reset(): void {
    this.playerDice.set(null);
    this.opponentDice.set(null);
  }
}

