import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './config';
import { Portfolio, SectorAllocation } from './models';

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  constructor(private http: HttpClient) {}

  get(): Observable<Portfolio> {
    return this.http.get<Portfolio>(`${API_URL}/portfolio`);
  }

  calculate(currentPrices: Record<number, number>): Observable<Portfolio> {
    return this.http.post<Portfolio>(`${API_URL}/portfolio/calculate`, currentPrices);
  }

  sectors(): Observable<SectorAllocation[]> {
    return this.http.get<SectorAllocation[]>(`${API_URL}/portfolio/sectors`);
  }
}
