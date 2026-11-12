import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface AuthResponse {
  token: string;
  email: string;
  fullName: string;
  role: 'BUYER' | 'SUPPLIER';
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8081/api/auth';

  // Signal reattivo per lo stato dell'utente loggato
  currentUser = signal<AuthResponse | null>(this.loadUserFromStorage());
  isAuthenticated = computed(() => this.currentUser() !== null);
  userRole = computed(() => this.currentUser()?.role ?? null);

  private loadUserFromStorage(): AuthResponse | null {
    const raw = localStorage.getItem('tenderflow_user');
    return raw ? JSON.parse(raw) : null;
  }

  register(data: { fullName: string; email: string; password: string; role: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, data).pipe(
      tap((res) => this.saveSession(res))
    );
  }

  login(data: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, data).pipe(
      tap((res) => this.saveSession(res))
    );
  }

  private saveSession(res: AuthResponse): void {
    localStorage.setItem('jwt_token', res.token);
    localStorage.setItem('tenderflow_user', JSON.stringify(res));
    this.currentUser.set(res);
  }

  logout(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('tenderflow_user');
    this.currentUser.set(null);
  }
}