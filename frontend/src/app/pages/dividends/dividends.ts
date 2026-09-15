import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { StockService } from '../../core/stock.service';
import { DividendService } from '../../core/dividend.service';
import { Stock, Dividend } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-dividends',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Dividends</h2>

    <div class="card">
      <div class="stat-label">Lifetime dividend income</div>
      <div class="stat-value badge-green">{{ tzs(total()) }}</div>
    </div>

    <div class="card">
      <h3>Log a dividend</h3>
      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="form-row">
          <div class="field">
            <label>Stock</label>
            <select formControlName="stockId">
              <option [ngValue]="null" disabled>Select a stock</option>
              @for (s of stocks(); track s.id) {
                <option [ngValue]="s.id">{{ s.ticker }} — {{ s.companyName }}</option>
              }
            </select>
          </div>
          <div class="field">
            <label>Amount per share (TZS)</label>
            <input type="number" formControlName="amountPerShare" min="0.01" step="0.01" />
          </div>
        </div>
        <div class="form-row">
          <div class="field">
            <label>Shares held</label>
            <input type="number" formControlName="shares" min="1" />
          </div>
          <div class="field">
            <label>Payment date</label>
            <input type="date" formControlName="paymentDate" />
          </div>
        </div>
        <div class="field">
          <label>Notes (optional)</label>
          <input type="text" formControlName="notes" />
        </div>
        <button class="btn" type="submit" [disabled]="form.invalid || submitting()">
          {{ submitting() ? 'Saving…' : 'Log dividend' }}
        </button>
      </form>
    </div>

    <div class="card">
      <div class="row-header">
        <h3>History</h3>
        <div class="export-btns">
          <button class="btn btn-outline btn-sm" (click)="exportCsv()">Export CSV</button>
          <button class="btn btn-outline btn-sm" (click)="exportPdf()">Export PDF</button>
        </div>
      </div>
      @if (!dividends().length) {
        <div class="empty">No dividends logged yet.</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Stock</th>
                <th>Amount/Share</th>
                <th>Shares</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              @for (d of dividends(); track d.id) {
                <tr>
                  <td class="mono">{{ d.paymentDate }}</td>
                  <td>
                    <strong>{{ d.ticker }}</strong>
                    <div class="hint">{{ d.companyName }}</div>
                  </td>
                  <td class="mono">{{ tzs(d.amountPerShare) }}</td>
                  <td class="mono">{{ d.shares }}</td>
                  <td class="mono">{{ tzs(d.totalAmount) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .row-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
      }
      .export-btns {
        display: flex;
        gap: 8px;
      }
      .hint {
        color: var(--text-3);
        font-size: 12px;
      }
    `,
  ],
})
export class DividendsPage implements OnInit {
  private fb = inject(FormBuilder);
  readonly stocks = signal<Stock[]>([]);
  readonly dividends = signal<Dividend[]>([]);
  readonly total = signal(0);
  readonly submitting = signal(false);

  form = this.fb.group({
    stockId: [null as number | null, Validators.required],
    amountPerShare: [null as number | null, [Validators.required, Validators.min(0.01)]],
    shares: [null as number | null, [Validators.required, Validators.min(1)]],
    paymentDate: [new Date().toISOString().slice(0, 10), Validators.required],
    notes: [''],
  });

  constructor(
    private stockService: StockService,
    private dividendService: DividendService,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    this.stockService.getAll().subscribe({ next: (s) => this.stocks.set(s) });
    this.load();
  }

  load() {
    this.dividendService.getHistory().subscribe({
      next: (d) => this.dividends.set(d),
      error: () => this.toast.err('Failed to load dividends'),
    });
    this.dividendService.getTotal().subscribe({
      next: (r) => this.total.set(r.totalDividends),
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    const v = this.form.getRawValue();
    this.dividendService
      .record({
        stockId: v.stockId!,
        amountPerShare: v.amountPerShare!,
        shares: v.shares!,
        paymentDate: v.paymentDate!,
        notes: v.notes || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.toast.ok('Dividend logged');
          this.form.reset({ paymentDate: new Date().toISOString().slice(0, 10), notes: '' });
          this.load();
        },
        error: (err) => {
          this.submitting.set(false);
          this.toast.err(err?.error?.message || 'Failed to log dividend');
        },
      });
  }

  exportCsv() {
    this.dividendService.downloadCsv().subscribe({
      next: (blob) => this.saveBlob(blob, 'dividends.csv'),
      error: () => this.toast.err('Export failed'),
    });
  }

  exportPdf() {
    this.dividendService.downloadPdf().subscribe({
      next: (blob) => this.saveBlob(blob, 'dividends.pdf'),
      error: () => this.toast.err('Export failed'),
    });
  }

  private saveBlob(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  tzs(v: number): string {
    return 'TZS ' + Math.round(v).toLocaleString('en-US');
  }
}
