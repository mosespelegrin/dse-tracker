import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './config';
import { Alert } from './models';

export interface AlertRequest {
  stockId: number;
  conditionType: 'ABOVE' | 'BELOW';
  targetPrice: number;
}

@Injectable({ providedIn: 'root' })
export class AlertService {
  constructor(private http: HttpClient) {}

  getAll(): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${API_URL}/alerts`);
  }

  create(request: AlertRequest): Observable<Alert> {
    return this.http.post<Alert>(`${API_URL}/alerts`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/alerts/${id}`);
  }
}
