import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';
import { AuthComponent } from './components/auth/auth.component';
import { TenderFormComponent } from './components/tender-form/tender-form.component';
import { TenderListComponent } from './components/tender-list/tender-list.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, AuthComponent, TenderFormComponent, TenderListComponent],
  templateUrl: './app.component.html'
})
export class AppComponent {
  authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
  }
}