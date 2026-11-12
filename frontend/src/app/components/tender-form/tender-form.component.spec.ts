import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TenderForm } from './tender-form.component';

describe('TenderForm', () => {
  let component: TenderForm;
  let fixture: ComponentFixture<TenderForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TenderForm],
    }).compileComponents();

    fixture = TestBed.createComponent(TenderForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
