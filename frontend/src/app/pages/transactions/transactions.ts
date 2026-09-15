import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { StockService } from '../../core/stock.service';
import { TransactionService } from '../../core/transaction.service';
import { Stock, Transaction, OrderImportResult } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Transactions</h2>

    <div class="card">
      <div class="tx-tabs">
        <button class="btn" [class.btn-outline]="mode() !== 'BUY'" (click)="setMode('BUY')">Buy</button>
        <button class="btn" [class.btn-outline]="mode() !== 'SELL'" (click)="setMode('SELL')">Sell</button>
      </div>

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
            <label>Shares</label>
            <input type="number" formControlName="shares" min="1" />
          </div>
        </div>

        <div class="form-row">
          @if (mode() === 'BUY') {
            <div class="field">
              <label>Total paid (TZS)</label>
              <input type="number" formControlName="totalPaid" min="0.01" step="0.01" />
            </div>
          } @else {
            <div class="field">
              <label>Sell price per share (TZS)</label>
              <input type="number" formControlName="sellPrice" min="0.01" step="0.01" />
            </div>
          }
          <div class="field">
            <label>Date</label>
            <input type="date" formControlName="date" />
          </div>
        </div>

        <div class="field">
          <label>Notes (optional)</label>
          <input type="text" formControlName="notes" placeholder="e.g. quarterly top-up" />
        </div>

        <button class="btn" type="submit" [disabled]="form.invalid || submitting()">
          {{ submitting() ? 'Saving…' : mode() === 'BUY' ? 'Log buy' : 'Log sell' }}
        </button>
      </form>
    </div>

    <div class="card">
      <div class="row-header">
        <h3>Import DSE order export</h3>
      </div>
      <p class="hint">Upload a CSV export to reconcile it against what you've logged here.</p>
      <input type="file" accept=".csv" (change)="onFileSelected($event)" />
      @if (importing()) {
        <p class="hint">Reconciling…</p>
      }
      @if (importResult(); as r) {
        <div class="import-summary">
          <span class="chip chip-buy">{{ r.matched }} matched</span>
          <span class="chip chip-sell">{{ r.mismatched }} mismatched</span>
          <span class="chip" style="background:var(--amber-bg);color:var(--amber)">{{ r.notFound }} not found</span>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Row</th>
                <th>Ticker</th>
                <th>Status</th>
                <th>Detail</th>
              </tr>
            </thead>
            <tbody>
              @for (row of r.rows; track row.row) {
                <tr>
                  <td class="mono">{{ row.row }}</td>
                  <td class="mono">{{ row.ticker }}</td>
                  <td>{{ row.status }}</td>
                  <td class="hint">{{ row.message }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <div class="card">
      <div class="row-header">
        <h3>History</h3>
        <div class="export-btns">
          <button class="btn btn-outline btn-sm" (click)="exportCsv()">Export CSV</button>
          <button class="btn btn-outline btn-sm" (click)="exportPdf()">Export PDF</button>
        </div>
      </div>

      @if (!transactions().length) {
        <div class="empty">No transactions yet.</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Stock</th>
                <th>Type</th>
                <th>Shares</th>
                <th>Amount</th>
                <th>Realized</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              @for (t of transactions(); track t.id) {
                <tr>
                  <td class="mono">{{ t.date }}</td>
                  <td>
                    <strong>{{ t.ticker }}</strong>
                    <div class="hint">{{ t.companyName }}</div>
                  </td>
                  <td><span class="chip" [class.chip-buy]="t.type === 'BUY'" [class.chip-sell]="t.type === 'SELL'">{{ t.type }}</span></td>
                  <td class="mono">{{ t.shares }}</td>
                  <td class="mono">{{ tzs(t.totalPaid) }}</td>
                  <td class="mono">{{ t.realizedGain !== undefined && t.realizedGain !== null ? tzs(t.realizedGain) : '—' }}</td>
                  <td class="hint">{{ t.notes || '—' }}</td>
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
      .tx-tabs {
        display: flex;
        gap: 8px;
        margin-bottom: 16px;
      }
      .row-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
      }
      .hint {
        color: var(--text-3);
        font-size: 12px;
      }
      .export-btns {
        display: flex;
        gap: 8px;
      }
      .import-summary {
        display: flex;
        gap: 8px;
        margin: 12px 0;
      }
      input[type='file'] {
        margin-top: 8px;
      }
    `,
  ],
})
export class TransactionsPage implements OnInit {
  private fb = inject(FormBuilder);
  readonly stocks = signal<Stock[]>([]);
  readonly transactions = signal<Transaction[]>([]);
  readonly mode = signal<'BUY' | 'SELL'>('BUY');
  readonly submitting = signal(false);
  readonly importing = signal(false);
  readonly importResult = signal<OrderImportResult | null>(null);

  form = this.fb.group({
    stockId: [null as number | null, Validators.required],
    shares: [null as number | null, [Validators.required, Validators.min(1)]],
    totalPaid: [null as number | null],
    sellPrice: [null as number | null],
    date: [new Date().toISOString().slice(0, 10), Validators.required],
    notes: [''],
  });

  constructor(
    private stockService: StockService,
    private txService: TransactionService,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    this.stockService.getAll().subscribe({
      next: (s) => this.stocks.set(s),
      error: () => this.toast.err('Failed to load stock list'),
    });
    this.loadHistory();
  }

  setMode(mode: 'BUY' | 'SELL') {
    this.mode.set(mode);
  }

  loadHistory() {
    this.txService.getHistory().subscribe({
      next: (t) => this.transactions.set(t),
      error: () => this.toast.err('Failed to load transactions'),
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    const v = this.form.getRawValue();
    const request = {
      stockId: v.stockId!,
      shares: v.shares!,
      totalPaid: v.totalPaid ?? undefined,
      sellPrice: v.sellPrice ?? undefined,
      date: v.date!,
      notes: v.notes || undefined,
    };

    const call = this.mode() === 'BUY' ? this.txService.buy(request) : this.txService.sell(request);
    call.subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.ok(this.mode() === 'BUY' ? 'Buy logged' : 'Sell logged');
        this.form.reset({ date: new Date().toISOString().slice(0, 10), notes: '' });
        this.loadHistory();
      },
      error: (err) => {
        this.submitting.set(false);
        this.toast.err(err?.error?.message || 'Transaction failed');
      },
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.importing.set(true);
    this.importResult.set(null);
    this.txService.importOrders(file).subscribe({
      next: (res) => {
        this.importing.set(false);
        this.importResult.set(res);
      },
      error: (err) => {
        this.importing.set(false);
        this.toast.err(err?.error?.message || 'Import failed');
      },
      complete: () => {
        input.value = '';
      },
    });
  }

  exportCsv() {
    this.txService.downloadCsv().subscribe({
      next: (blob) => this.saveBlob(blob, 'transactions.csv'),
      error: () => this.toast.err('Export failed'),
    });
  }

  exportPdf() {
    this.txService.downloadPdf().subscribe({
      next: (blob) => this.saveBlob(blob, 'transactions.pdf'),
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
