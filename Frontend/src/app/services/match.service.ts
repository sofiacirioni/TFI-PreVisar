import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { retry } from 'rxjs/operators';
import { Match } from '../models/match.model';
import { StatsResponse } from '../models/stats-response.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MatchService {
  private readonly baseUrl = environment.apiUrl + '/match';

  constructor(private http: HttpClient) {}

  createMatch(characterId: number, opponentId?: number): Observable<Match> {
    const body = opponentId ? { opponent_id: opponentId } : {};
    return this.http.post<Match>(`${this.baseUrl}/${characterId}`, body).pipe(retry(2));
  }

  getStats(): Observable<StatsResponse[]> {
    return this.http.get<StatsResponse[]>(`${this.baseUrl}/stats`);
  }
}

