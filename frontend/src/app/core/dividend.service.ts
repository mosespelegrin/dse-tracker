import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './config';
import { Dividend } from './models';

export interface DividendRequest {
  stockId: number;
  amountPerShare: number;
  shares: number;
  paymentDate: string;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class DividendService {
  constructor(private http: HttpClient) {}

  getHistory(): Observable<Dividend[]> {
    return this.http.get<Dividend[]>(`${API_URL}/dividends`);
  }

  getTotal(): Observable<{ totalDividends: number }> {
    return this.http.get<{ totalDividends: number }>(`${API_URL}/dividends/total`);
  }

  record(request: DividendRequest): Observable<Dividend> {
    return this.http.post<Dividend>(`${API_URL}/dividends`, request);
  }

  downloadCsv(): Observable<Blob> {
    return this.http.get(`${API_URL}/dividends/export.csv`, { responseType: 'blob' });
  }

  downloadPdf(): Observable<Blob> {
    return this.http.get(`${API_URL}/dividends/export.pdf`, { responseType: 'blob' });
  }
}
