import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortfolioService } from '../../core/portfolio.service';
import { Portfolio, SectorAllocation } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h2>Portfolio</h2>

    @if (portfolio(); as p) {
      <div class="grid grid-3" style="margin: 16px 0 20px">
        <div class="card">
          <div class="stat-label">Total invested</div>
          <div class="stat-value">{{ tzs(p.totalInvested) }}</div>
        </div>
        <div class="card">
          <div class="stat-label">Unrealized gain/loss</div>
          <div class="stat-value" [class.badge-green]="gainPositive(p.totalGainLoss)" [class.badge-red]="!gainPositive(p.totalGainLoss) && p.totalGainLoss !== undefined">
            {{ p.totalGainLoss !== undefined ? tzs(p.totalGainLoss) : '—' }}
          </div>
        </div>
        <div class="card">
          <div class="stat-label">Realized gain (from sells)</div>
          <div class="stat-value" [class.badge-green]="gainPositive(p.totalRealizedGain)" [class.badge-red]="!gainPositive(p.totalRealizedGain)">
            {{ tzs(p.totalRealizedGain) }}
          </div>
        </div>
      </div>

      <div class="card">
        <h3>Holdings</h3>
        @if (!p.holdings.length) {
          <div class="empty">No holdings yet — log a buy in Transactions.</div>
        } @else {
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Ticker</th>
                  <th>Company</th>
                  <th>Shares</th>
                  <th>Total Paid</th>
                  <th>Current Value</th>
                  <th>Gain/Loss</th>
                  <th>ROI</th>
                </tr>
              </thead>
              <tbody>
                @for (h of p.holdings; track h.id) {
                  <tr>
                    <td class="mono">{{ h.ticker }}</td>
                    <td>{{ h.companyName }}</td>
                    <td class="mono">{{ h.shares }}</td>
                    <td class="mono">{{ tzs(h.totalPaid) }}</td>
                    <td class="mono">{{ h.currentValue !== undefined ? tzs(h.currentValue) : '—' }}</td>
                    <td class="mono" [class.badge-green]="gainPositive(h.gainLoss)" [class.badge-red]="!gainPositive(h.gainLoss) && h.gainLoss !== undefined">
                      {{ h.gainLoss !== undefined ? tzs(h.gainLoss) : '—' }}
                    </td>
                    <td class="mono">{{ h.roiPercent !== undefined ? h.roiPercent.toFixed(2) + '%' : '—' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    } @else {
      <div class="card"><div class="empty">Loading…</div></div>
    }

    <div class="card" style="margin-top:16px">
      <h3>Sector breakdown</h3>
      @if (!sectors().length) {
        <div class="empty">No holdings to break down yet.</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Sector</th>
                <th>Invested</th>
                <th>% of portfolio</th>
              </tr>
            </thead>
            <tbody>
              @for (s of sectors(); track s.sector) {
                <tr>
                  <td>{{ s.sector }}</td>
                  <td class="mono">{{ tzs(s.invested) }}</td>
                  <td class="mono">{{ s.percentOfPortfolio.toFixed(1) }}%</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class PortfolioPage implements OnInit {
  readonly portfolio = signal<Portfolio | null>(null);
  readonly sectors = signal<SectorAllocation[]>([]);

  constructor(
    private portfolioService: PortfolioService,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    this.portfolioService.get().subscribe({
      next: (p) => this.portfolio.set(p),
      error: () => this.toast.err('Failed to load portfolio'),
    });
    this.portfolioService.sectors().subscribe({
      next: (s) => this.sectors.set(s),
      error: () => this.toast.err('Failed to load sector breakdown'),
    });
  }

  gainPositive(v: number | undefined): boolean {
    return (v ?? 0) >= 0;
  }

  tzs(v: number): string {
    return 'TZS ' + Math.round(v).toLocaleString('en-US');
  }
}
