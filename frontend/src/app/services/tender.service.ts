import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Tender } from '../models/tender.model';

@Injectable({
  providedIn: 'root'
})
export class TenderService {
  private http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8081/api/tenders';

  getTenders(): Observable<Tender[]> {
    return this.http.get<Tender[]>(this.apiUrl);
  }

  getTenderById(id: number): Observable<Tender> {
    return this.http.get<Tender>(`${this.apiUrl}/${id}`);
  }

  createTender(tender: Tender): Observable<Tender> {
    return this.http.post<Tender>(this.apiUrl, tender);
  }

  updateStatus(id: number, status: string): Observable<Tender> {
    return this.http.patch<Tender>(`${this.apiUrl}/${id}/status`, { status });
  }
}