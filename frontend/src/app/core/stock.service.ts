import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './config';
import { Stock, StockFundamentalsInput } from './models';

@Injectable({ providedIn: 'root' })
export class StockService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<Stock[]> {
    return this.http.get<Stock[]>(`${API_URL}/stocks`);
  }

  getById(id: number): Observable<Stock> {
    return this.http.get<Stock>(`${API_URL}/stocks/${id}`);
  }

  updateFundamentals(id: number, input: StockFundamentalsInput): Observable<Stock> {
    return this.http.put<Stock>(`${API_URL}/stocks/${id}/fundamentals`, input);
  }
}
