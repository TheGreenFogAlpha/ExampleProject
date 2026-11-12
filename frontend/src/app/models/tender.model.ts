export interface Tender {
  id?: number;
  title: string;
  description: string;
  budget: number;
  closingDate?: string;
  status: 'DRAFT' | 'OPEN' | 'CLOSED' | 'AWARDED';
}