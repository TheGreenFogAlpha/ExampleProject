import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenderService } from '../../services/tender.service';
import { Tender } from '../../models/tender.model';

@Component({
  selector: 'app-tender-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tender-list.component.html',
  styleUrl: './tender-list.component.scss'
})
export class TenderListComponent implements OnInit {
  private tenderService = inject(TenderService);
  private cdr = inject(ChangeDetectorRef);

  tenders: Tender[] = [];
  isLoading = true;
  errorMessage = '';

  ngOnInit(): void {
    this.loadTenders();
  }

  loadTenders(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.tenderService.getTenders().subscribe({
      next: (data) => {
        this.tenders = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Errore recupero gare:', err);
        this.errorMessage = 'Errore nel caricamento delle gare.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }
}