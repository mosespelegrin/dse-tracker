import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './config';
import { OrderImportResult, Page, Transaction } from './models';

export interface TransactionRequest {
  stockId: number;
  shares: number;
  totalPaid?: number;
  sellPrice?: number;
  date: string;
  notes?: string;
}

@Injectable({ providedIn: 'root' })
export class TransactionService {
  constructor(private http: HttpClient) {}

  getHistory(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${API_URL}/transactions`);
  }

  getHistoryPaged(page: number, size = 20): Observable<Page<Transaction>> {
    return this.http.get<Page<Transaction>>(`${API_URL}/transactions/page`, {
      params: { page, size },
    });
  }

  buy(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${API_URL}/transactions/buy`, request);
  }

  sell(request: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${API_URL}/transactions/sell`, request);
  }

  importOrders(file: File): Observable<OrderImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<OrderImportResult>(`${API_URL}/transactions/import`, formData);
  }

  exportCsvUrl(): string {
    return `${API_URL}/transactions/export.csv`;
  }

  exportPdfUrl(): string {
    return `${API_URL}/transactions/export.pdf`;
  }

  downloadCsv(): Observable<Blob> {
    return this.http.get(`${API_URL}/transactions/export.csv`, { responseType: 'blob' });
  }

  downloadPdf(): Observable<Blob> {
    return this.http.get(`${API_URL}/transactions/export.pdf`, { responseType: 'blob' });
  }
}
