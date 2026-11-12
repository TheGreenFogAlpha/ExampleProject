import { Component, ViewChild } from '@angular/core';
import { TenderListComponent } from './components/tender-list/tender-list.component';
import { TenderFormComponent } from './components/tender-form/tender-form.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [TenderListComponent, TenderFormComponent],
  template: `
    <div style="max-width: 1200px; margin: 0 auto; padding: 2rem; font-family: system-ui, sans-serif;">
      <header style="margin-bottom: 2rem;">
        <h1 style="color: #0f172a; margin: 0;">TenderFlow Enterprise</h1>
        <p style="color: #64748b; margin-top: 0.25rem;">Gestione Gare d'Appalto e Integrazione Backend Quarkus</p>
      </header>

      <app-tender-form (tenderCreated)="tenderList.loadTenders()"></app-tender-form>
      <app-tender-list #tenderList></app-tender-list>
    </div>
  `
})
export class AppComponent {}