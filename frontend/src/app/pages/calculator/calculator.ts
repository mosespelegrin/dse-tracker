import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PortfolioService } from '../../core/portfolio.service';
import { Holding, Portfolio } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { ShareCard } from '../../core/share-card';
import { GainLossChart } from '../../core/gain-loss-chart';

@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [CommonModule, FormsModule, ShareCard, GainLossChart],
  template: `
    <h2>P&amp;L Calculator</h2>
    <p class="hint">P&amp;L and charts are calculated from live DSE market data. You can also test hypothetical market moves by editing prices below.</p>

    <div class="card">
      @if (!holdings().length) {
        <div class="empty">No holdings to calculate. Add transactions first.</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Stock</th>
                <th>Shares</th>
                <th>Price (TZS)</th>
              </tr>
            </thead>
            <tbody>
              @for (h of holdings(); track h.id) {
                <tr>
                  <td><strong>{{ h.ticker }}</strong></td>
                  <td class="mono">{{ h.shares }}</td>
                  <td>
                    <input type="number" min="0" step="0.01" [(ngModel)]="prices[h.stockId]" style="max-width:140px" />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <div style="display:flex;gap:8px;margin-top:14px">
          <button class="btn" [disabled]="calculating()" (click)="calculate()">
            {{ calculating() ? 'Calculating…' : 'Recalculate' }}
          </button>
          <button class="btn btn-outline" [disabled]="calculating()" (click)="resetToDsePrices()">
            Reset to DSE prices
          </button>
        </div>
      }
    </div>

    @if (result(); as r) {
      <div class="grid grid-3" style="margin-top:16px">
        <div class="card">
          <div class="stat-label">Total current value</div>
          <div class="stat-value">{{ tzs(r.totalCurrentValue || 0) }}</div>
        </div>
        <div class="card">
          <div class="stat-label">Total gain/loss</div>
          <div class="stat-value" [class.badge-green]="(r.totalGainLoss || 0) >= 0" [class.badge-red]="(r.totalGainLoss || 0) < 0">
            {{ tzs(r.totalGainLoss || 0) }}
          </div>
        </div>
        <div class="card">
          <div class="stat-label">Overall ROI</div>
          <div class="stat-value" [class.badge-green]="(r.overallRoiPercent || 0) >= 0" [class.badge-red]="(r.overallRoiPercent || 0) < 0">
            {{ (r.overallRoiPercent || 0).toFixed(2) }}%
          </div>
        </div>
      </div>

      <div class="card" style="margin-top:16px">
        <h3>Gain/loss by holding</h3>
        <app-gain-loss-chart [holdings]="r.holdings"></app-gain-loss-chart>
      </div>

      <button class="btn btn-outline" style="margin-top:16px" (click)="showShare.set(true)">
        Share your return
      </button>

      @if (showShare()) {
        <app-share-card [portfolio]="r" (close)="showShare.set(false)"></app-share-card>
      }
    }
  `,
  styles: [
    `
      .hint {
        color: var(--text-2);
        font-size: 13px;
        margin: 4px 0 16px;
      }
    `,
  ],
})
export class CalculatorPage implements OnInit {
  readonly holdings = signal<Holding[]>([]);
  readonly calculating = signal(false);
  readonly result = signal<Portfolio | null>(null);
  readonly showShare = signal(false);
  prices: Record<number, number> = {};

  constructor(
    private portfolioService: PortfolioService,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    this.portfolioService.get().subscribe({
      next: (p) => {
        this.holdings.set(p.holdings);
        this.initPrices(p.holdings);
        if (p.totalCurrentValue !== undefined && p.totalCurrentValue !== null) {
          this.result.set(p);
        }
      },
      error: () => this.toast.err('Failed to load portfolio'),
    });
  }

  private initPrices(holdings: Holding[]) {
    this.prices = {};
    for (const h of holdings) {
      if (h.currentPrice !== undefined && h.currentPrice !== null) {
        this.prices[h.stockId] = h.currentPrice;
      }
    }
  }

  resetToDsePrices() {
    this.portfolioService.get().subscribe({
      next: (p) => {
        this.holdings.set(p.holdings);
        this.initPrices(p.holdings);
        this.result.set(p);
        this.toast.ok('Reset to live DSE prices');
      },
      error: () => this.toast.err('Failed to reload portfolio'),
    });
  }

  calculate() {
    const entered = Object.entries(this.prices).filter(([, v]) => v !== undefined && v !== null && `${v}` !== '');
    if (!entered.length) {
      this.toast.err('Enter at least one price to calculate');
      return;
    }
    const payload: Record<number, number> = {};
    for (const [stockId, price] of entered) {
      payload[Number(stockId)] = Number(price);
    }

    this.calculating.set(true);
    this.portfolioService.calculate(payload).subscribe({
      next: (r) => {
        this.calculating.set(false);
        this.result.set(r);
      },
      error: () => {
        this.calculating.set(false);
        this.toast.err('Calculation failed');
      },
    });
  }

  tzs(v: number): string {
    return 'TZS ' + Math.round(v).toLocaleString('en-US');
  }
}
