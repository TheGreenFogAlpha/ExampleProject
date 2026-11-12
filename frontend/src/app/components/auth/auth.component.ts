import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auth.component.html'
})
export class AuthComponent {
  private authService = inject(AuthService);
  private cdr = inject(ChangeDetectorRef);

  isLoginMode = true;
  isLoading = false;
  errorMessage = '';

  formData = {
    fullName: '',
    email: '',
    password: '',
    role: 'BUYER'
  };

  toggleMode(): void {
    this.isLoginMode = !this.isLoginMode;
    this.errorMessage = '';
    this.cdr.detectChanges();
  }

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    const request$ = this.isLoginMode
      ? this.authService.login({ email: this.formData.email, password: this.formData.password })
      : this.authService.register(this.formData);

    request$.subscribe({
      next: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.error || 'Errore di autenticazione. Verifica i dati.';
        this.cdr.detectChanges();
      }
    });
  }
}