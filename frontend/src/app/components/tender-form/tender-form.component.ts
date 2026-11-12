import { Component, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TenderService } from '../../services/tender.service';
import { Tender } from '../../models/tender.model';

@Component({
  selector: 'app-tender-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tender-form.component.html',
  styleUrl: './tender-form.component.scss'
})
export class TenderFormComponent {
  private tenderService = inject(TenderService);
  private cdr = inject(ChangeDetectorRef);

  newTender: Partial<Tender> = {
    title: '',
    description: '',
    budget: undefined,
    status: 'DRAFT'
  };

  isSubmitting = false;

  onSubmit(): void {
    if (!this.newTender.title || !this.newTender.description || !this.newTender.budget) {
      return;
    }

    this.isSubmitting = true;
    this.cdr.detectChanges();

    this.tenderService.createTender(this.newTender as Tender).subscribe({
      next: () => {
        this.newTender = {
          title: '',
          description: '',
          budget: undefined,
          status: 'DRAFT'
        };
        this.isSubmitting = false;
        this.cdr.detectChanges();
        window.location.reload();
      },
      error: (err) => {
        console.error('Errore creazione gara:', err);
        alert('Errore ' + err.status + ': verifica che il token JWT sia valido.');
        this.isSubmitting = false;
        this.cdr.detectChanges();
      }
    });
  }
}