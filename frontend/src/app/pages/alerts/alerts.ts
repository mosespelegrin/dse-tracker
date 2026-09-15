import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { StockService } from '../../core/stock.service';
import { AlertService } from '../../core/alert.service';
import { Stock, Alert } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <h2>Price alerts</h2>

    <div class="card">
      <h3>New alert</h3>
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
            <label>Condition</label>
            <select formControlName="conditionType">
              <option value="ABOVE">Price goes above</option>
              <option value="BELOW">Price goes below</option>
            </select>
          </div>
          <div class="field">
            <label>Target price (TZS)</label>
            <input type="number" formControlName="targetPrice" min="0.01" step="0.01" />
          </div>
        </div>
        <button class="btn" type="submit" [disabled]="form.invalid || submitting()">
          {{ submitting() ? 'Saving…' : 'Create alert' }}
        </button>
      </form>
    </div>

    <div class="card">
      <h3>Your alerts</h3>
      @if (!alerts().length) {
        <div class="empty">No alerts yet.</div>
      } @else {
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Stock</th>
                <th>Condition</th>
                <th>Target</th>
                <th>Status</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              @for (a of alerts(); track a.id) {
                <tr>
                  <td>
                    <strong>{{ a.stockTicker }}</strong>
                    <div class="hint">{{ a.companyName }}</div>
                  </td>
                  <td>{{ a.conditionType === 'ABOVE' ? 'Above' : 'Below' }}</td>
                  <td class="mono">{{ tzs(a.targetPrice) }}</td>
                  <td>
                    @if (a.triggered) {
                      <span class="chip chip-buy">Triggered</span>
                    } @else {
                      <span class="chip" style="background:var(--surface-2);color:var(--text-2)">Watching</span>
                    }
                  </td>
                  <td><button class="btn btn-outline btn-sm" (click)="remove(a.id)">Delete</button></td>
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
      .hint {
        color: var(--text-3);
        font-size: 12px;
      }
    `,
  ],
})
export class AlertsPage implements OnInit {
  private fb = inject(FormBuilder);
  readonly stocks = signal<Stock[]>([]);
  readonly alerts = signal<Alert[]>([]);
  readonly submitting = signal(false);

  form = this.fb.group({
    stockId: [null as number | null, Validators.required],
    conditionType: ['ABOVE' as 'ABOVE' | 'BELOW', Validators.required],
    targetPrice: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  constructor(
    private stockService: StockService,
    private alertService: AlertService,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    this.stockService.getAll().subscribe({ next: (s) => this.stocks.set(s) });
    this.load();
  }

  load() {
    this.alertService.getAll().subscribe({
      next: (a) => this.alerts.set(a),
      error: () => this.toast.err('Failed to load alerts'),
    });
  }

  submit() {
    if (this.form.invalid) return;
    this.submitting.set(true);
    const v = this.form.getRawValue();
    this.alertService.create({ stockId: v.stockId!, conditionType: v.conditionType!, targetPrice: v.targetPrice! }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.toast.ok('Alert saved');
        this.form.reset({ conditionType: 'ABOVE' });
        this.load();
      },
      error: (err) => {
        this.submitting.set(false);
        this.toast.err(err?.error?.message || 'Failed to save alert');
      },
    });
  }

  remove(id: number) {
    this.alertService.delete(id).subscribe({
      next: () => {
        this.toast.ok('Alert deleted');
        this.load();
      },
      error: () => this.toast.err('Failed to delete alert'),
    });
  }

  tzs(v: number): string {
    return 'TZS ' + Math.round(v).toLocaleString('en-US');
  }
}
