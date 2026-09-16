import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Holding } from './models';

interface ChartRow {
  ticker: string;
  gainLoss: number;
  roiPercent: number;
  barX: number;
  barWidth: number;
  labelX: number;
  labelAnchor: 'start' | 'end';
  isGain: boolean;
  y: number;
}

// Diverging bar chart: each holding's gain/loss relative to a zero baseline.
// Direction (left = loss, right = gain) is the primary signal, not color —
// the app's existing green/red pair fails CVD-safety for this exact
// distinction (validated via the dataviz skill's palette checker), so color
// here is reinforcement only, backed by bar direction and an explicit
// +/- value label on every bar.
@Component({
  selector: 'app-gain-loss-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (rows().length) {
      <div class="chart-wrap">
        <div class="legend">
          <span class="legend-item"><span class="swatch gain"></span>Gain</span>
          <span class="legend-item"><span class="swatch loss"></span>Loss</span>
        </div>
        <svg [attr.viewBox]="'0 0 ' + width + ' ' + height()" [attr.height]="height()" class="chart">
          <line [attr.x1]="centerX" [attr.x2]="centerX" y1="4" [attr.y2]="height() - 4" class="zero-line" />
          @for (row of rows(); track row.ticker) {
            <text [attr.x]="leftLabelWidth - 10" [attr.y]="row.y + rowHeight / 2" text-anchor="end"
                  dominant-baseline="middle" class="ticker-label">{{ row.ticker }}</text>
            <rect [attr.x]="row.barX" [attr.y]="row.y + (rowHeight - barThickness) / 2"
                  [attr.width]="row.barWidth" [attr.height]="barThickness" rx="4"
                  [class.bar-gain]="row.isGain" [class.bar-loss]="!row.isGain">
              <title>{{ row.ticker }}: {{ row.isGain ? '+' : '' }}{{ fmt(row.gainLoss) }} ({{ row.isGain ? '+' : '' }}{{ row.roiPercent.toFixed(1) }}%)</title>
            </rect>
            <text [attr.x]="row.labelX" [attr.y]="row.y + rowHeight / 2" [attr.text-anchor]="row.labelAnchor"
                  dominant-baseline="middle" class="value-label">
              {{ row.isGain ? '+' : '' }}{{ fmt(row.gainLoss) }}
            </text>
          }
        </svg>
      </div>
    }
  `,
  styles: [
    `
      .chart-wrap {
        margin-top: 8px;
      }
      .legend {
        display: flex;
        gap: 16px;
        margin-bottom: 8px;
        font-size: 12px;
        color: var(--text-2);
      }
      .legend-item {
        display: flex;
        align-items: center;
        gap: 6px;
      }
      .swatch {
        width: 10px;
        height: 10px;
        border-radius: 3px;
        display: inline-block;
      }
      .swatch.gain {
        background: var(--green);
      }
      .swatch.loss {
        background: var(--red);
      }
      .chart {
        width: 100%;
        height: auto;
        display: block;
      }
      .zero-line {
        stroke: var(--border);
        stroke-width: 1;
      }
      .bar-gain {
        fill: var(--green);
      }
      .bar-loss {
        fill: var(--red);
      }
      .ticker-label {
        font-size: 12px;
        font-weight: 600;
        fill: var(--text-2);
      }
      .value-label {
        font-size: 11px;
        font-family: var(--mono);
        fill: var(--text-2);
      }
    `,
  ],
})
export class GainLossChart {
  @Input({ required: true }) set holdings(value: Holding[]) {
    this.holdingsSignal.set(value.filter((h) => h.gainLoss !== undefined));
  }

  private readonly holdingsSignal = signal<Holding[]>([]);

  readonly width = 520;
  readonly leftLabelWidth = 64;
  readonly rowHeight = 36;
  readonly barThickness = 20;
  readonly centerX = this.leftLabelWidth + (this.width - this.leftLabelWidth - 90) / 2;
  private readonly plotHalfWidth = (this.width - this.leftLabelWidth - 90) / 2 - 6;

  readonly height = computed(() => this.holdingsSignal().length * this.rowHeight + 8);

  readonly rows = computed<ChartRow[]>(() => {
    const holdings = this.holdingsSignal();
    const maxAbs = Math.max(1, ...holdings.map((h) => Math.abs(h.gainLoss ?? 0)));

    return holdings.map((h, i) => {
      const gainLoss = h.gainLoss ?? 0;
      const isGain = gainLoss >= 0;
      const barWidth = (Math.abs(gainLoss) / maxAbs) * this.plotHalfWidth;
      const y = i * this.rowHeight + 4;

      return {
        ticker: h.ticker,
        gainLoss,
        roiPercent: h.roiPercent ?? 0,
        isGain,
        y,
        barX: isGain ? this.centerX : this.centerX - barWidth,
        barWidth,
        labelX: isGain ? this.centerX + barWidth + 8 : this.centerX - barWidth - 8,
        labelAnchor: isGain ? 'start' : 'end',
      };
    });
  });

  fmt(v: number): string {
    return 'TZS ' + Math.round(Math.abs(v)).toLocaleString('en-US');
  }
}
