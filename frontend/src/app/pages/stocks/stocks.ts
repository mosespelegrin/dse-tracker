import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { StockService } from '../../core/stock.service';
import { Stock } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-stocks',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Fundamental analysis</h2>
    <p class="hint">
      P/E, P/B, dividend yield, ROE, ROA, etc. are computed live from each stock's current price and the
      financial inputs below — refreshed automatically the instant the scheduled price update runs.
    </p>

    <div class="card">
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Ticker</th>
              <th>Price</th>
              <th>P/E</th>
              <th>P/B</th>
              <th>Div Yield</th>
              <th>ROE</th>
              <th>ROA</th>
              <th>D/E</th>
              <th>Market Cap</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (s of stocks(); track s.id) {
              <tr [class.selected]="selected()?.id === s.id">
                <td><strong>{{ s.ticker }}</strong><div class="hint">{{ s.companyName }}</div></td>
                <td class="mono">{{ s.currentPrice !== null ? tzs(s.currentPrice) : '—' }}</td>
                <td class="mono">{{ fmt(s.peRatio) }}</td>
                <td class="mono">{{ fmt(s.pbRatio) }}</td>
                <td class="mono">{{ pct(s.dividendYieldPercent) }}</td>
                <td class="mono">{{ pct(s.roePercent) }}</td>
                <td class="mono">{{ pct(s.roaPercent) }}</td>
                <td class="mono">{{ fmt(s.debtToEquity) }}</td>
                <td class="mono">{{ s.marketCap !== null ? tzs(s.marketCap) : '—' }}</td>
                <td><button class="btn btn-outline btn-sm" (click)="select(s)">Edit inputs</button></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    @if (selected(); as s) {
      <div class="card">
        <h3>Financial inputs — {{ s.ticker }}</h3>
        <p class="hint">
          These are the raw figures ratios are computed from (from the company's annual/quarterly report).
          Leave a field blank if you don't have that figure yet — its dependent ratios just show as "—".
        </p>

        <form [formGroup]="form" (ngSubmit)="submit()">
          <div class="form-row">
            <div class="field">
              <label>EPS (TTM, TZS)</label>
              <input type="number" step="0.0001" formControlName="epsTtm" />
            </div>
            <div class="field">
              <label>Shares outstanding</label>
              <input type="number" formControlName="sharesOutstanding" />
            </div>
          </div>
          <div class="form-row">
            <div class="field">
              <label>Net income (TTM, TZS)</label>
              <input type="number" formControlName="netIncome" />
            </div>
            <div class="field">
              <label>Total equity (TZS)</label>
              <input type="number" formControlName="totalEquity" />
            </div>
          </div>
          <div class="form-row">
            <div class="field">
              <label>Total assets (TZS)</label>
              <input type="number" formControlName="totalAssets" />
            </div>
            <div class="field">
              <label>Total liabilities (TZS)</label>
              <input type="number" formControlName="totalLiabilities" />
            </div>
          </div>
          <div class="form-row">
            <div class="field">
              <label>Current assets (TZS)</label>
              <input type="number" formControlName="currentAssets" />
            </div>
            <div class="field">
              <label>Current liabilities (TZS)</label>
              <input type="number" formControlName="currentLiabilities" />
            </div>
          </div>
          <div class="field">
            <label>Annual dividend per share (TZS)</label>
            <input type="number" step="0.0001" formControlName="annualDividendPerShare" />
          </div>

          <div class="actions">
            <button class="btn" type="submit" [disabled]="saving()">{{ saving() ? 'Saving…' : 'Save' }}</button>
            <button class="btn btn-outline" type="button" (click)="cancel()">Cancel</button>
          </div>
        </form>
      </div>
    }
  `,
  styles: [
    `
      .hint {
        color: var(--text-3);
        font-size: 12px;
      }
      tr.selected {
        background: var(--surface-2);
      }
      .actions {
        display: flex;
        gap: 8px;
        margin-top: 8px;
      }
    `,
  ],
})
export class StocksPage implements OnInit {
  private fb = inject(FormBuilder);
  readonly stocks = signal<Stock[]>([]);
  readonly selected = signal<Stock | null>(null);
  readonly saving = signal(false);

  form = this.fb.group({
    epsTtm: [null as number | null],
    sharesOutstanding: [null as number | null],
    netIncome: [null as number | null],
    totalEquity: [null as number | null],
    totalAssets: [null as number | null],
    totalLiabilities: [null as number | null],
    currentAssets: [null as number | null],
    currentLiabilities: [null as number | null],
    annualDividendPerShare: [null as number | null],
  });

  constructor(
    private stockService: StockService,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    this.load();
  }

  load() {
    this.stockService.getAll().subscribe({
      next: (s) => this.stocks.set(s),
      error: () => this.toast.err('Failed to load stocks'),
    });
  }

  // Prefill from whatever's already saved — the update endpoint replaces
  // every field on save, so starting blank would wipe out existing data
  // for anything you don't happen to re-enter.
  select(stock: Stock) {
    this.selected.set(stock);
    this.form.reset({
      epsTtm: stock.epsTtm,
      sharesOutstanding: stock.sharesOutstanding,
      netIncome: stock.netIncome,
      totalEquity: stock.totalEquity,
      totalAssets: stock.totalAssets,
      totalLiabilities: stock.totalLiabilities,
      currentAssets: stock.currentAssets,
      currentLiabilities: stock.currentLiabilities,
      annualDividendPerShare: stock.annualDividendPerShare,
    });
  }

  cancel() {
    this.selected.set(null);
  }

  submit() {
    const s = this.selected();
    if (!s) return;
    this.saving.set(true);
    this.stockService.updateFundamentals(s.id, this.form.getRawValue()).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.ok('Fundamentals updated');
        this.selected.set(null);
        this.load();
      },
      error: (err) => {
        this.saving.set(false);
        this.toast.err(err?.error?.message || 'Failed to update fundamentals');
      },
    });
  }

  fmt(v: number | null): string {
    return v === null || v === undefined ? '—' : v.toFixed(2);
  }

  pct(v: number | null): string {
    return v === null || v === undefined ? '—' : v.toFixed(2) + '%';
  }

  tzs(v: number): string {
    return 'TZS ' + Math.round(v).toLocaleString('en-US');
  }
}
