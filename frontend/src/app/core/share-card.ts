import { AfterViewInit, Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Holding, Portfolio } from './models';

// Renders a branded, shareable portfolio-performance card as a downloadable
// PNG (1080x1080 — works as an Instagram/WhatsApp-status/X post). Every
// share is a small ad for the app, and every number on it is real —
// deliberately no absolute TZS amounts, just percentages and counts, so
// people are comfortable actually posting it.
@Component({
  selector: 'app-share-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="backdrop" (click)="close.emit()">
      <div class="modal card" (click)="$event.stopPropagation()">
        <h3>Share your performance</h3>
        <p class="hint">Only your % return and holding count are shown — no amounts.</p>
        <canvas #canvas width="1080" height="1080" class="preview"></canvas>
        <div class="actions">
          <button class="btn" (click)="download()">Download image</button>
          <button class="btn btn-outline" (click)="close.emit()">Close</button>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.6);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 16px;
        z-index: 100;
      }
      .modal {
        width: 100%;
        max-width: 380px;
        text-align: center;
      }
      .hint {
        color: var(--text-3);
        font-size: 12px;
        margin: 4px 0 14px;
      }
      .preview {
        width: 100%;
        max-width: 320px;
        height: auto;
        border-radius: var(--radius-md);
        margin: 0 auto;
        display: block;
      }
      .actions {
        display: flex;
        gap: 8px;
        margin-top: 16px;
      }
      .actions .btn {
        flex: 1;
      }
    `,
  ],
})
export class ShareCard implements AfterViewInit {
  @Input({ required: true }) portfolio!: Portfolio;
  @Output() close = new EventEmitter<void>();

  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  ngAfterViewInit() {
    this.draw();
  }

  download() {
    this.canvasRef.nativeElement.toBlob((blob) => {
      if (!blob) return;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'dse-track-portfolio.png';
      a.click();
      URL.revokeObjectURL(url);
    }, 'image/png');
  }

  private draw() {
    const canvas = this.canvasRef.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    const W = canvas.width;
    const H = canvas.height;

    const gradient = ctx.createLinearGradient(0, 0, W, H);
    gradient.addColorStop(0, '#4b8eff');
    gradient.addColorStop(1, '#6366f1');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, W, H);
    ctx.fillStyle = 'rgba(13,15,20,0.32)';
    ctx.fillRect(0, 0, W, H);

    const logoImg = new Image();
    logoImg.src = 'logo-mark.png';
    logoImg.onload = () => {
      ctx.save();
      this.roundRect(ctx, 64, 64, 64, 64, 14);
      ctx.clip();
      ctx.drawImage(logoImg, 64, 64, 64, 64);
      ctx.restore();
    };
    this.drawLogo(ctx, 64, 64, 64);

    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    ctx.fillStyle = '#ffffff';
    ctx.font = '700 34px system-ui, sans-serif';
    ctx.fillText('DSE Tracker', 144, 96);

    const roi = this.portfolio.overallRoiPercent ?? 0;
    const sign = roi >= 0 ? '+' : '';
    ctx.textAlign = 'center';
    ctx.font = '700 150px system-ui, sans-serif';
    ctx.fillStyle = '#ffffff';
    ctx.fillText(`${sign}${roi.toFixed(1)}%`, W / 2, 460);

    ctx.font = '500 32px system-ui, sans-serif';
    ctx.fillStyle = 'rgba(255,255,255,0.85)';
    ctx.fillText('Overall portfolio return', W / 2, 550);

    const holdingsCount = this.portfolio.holdings.length;
    const best = this.bestHolding();
    ctx.font = '600 36px system-ui, sans-serif';
    ctx.fillStyle = '#ffffff';
    ctx.fillText(`${holdingsCount} holding${holdingsCount === 1 ? '' : 's'} tracked`, W / 2, 700);
    if (best && best.roiPercent !== undefined) {
      const bestSign = best.roiPercent >= 0 ? '+' : '';
      ctx.font = '600 34px system-ui, sans-serif';
      ctx.fillStyle = 'rgba(255,255,255,0.9)';
      ctx.fillText(`Top performer: ${best.ticker} ${bestSign}${best.roiPercent.toFixed(1)}%`, W / 2, 764);
    }

    ctx.font = '500 26px system-ui, sans-serif';
    ctx.fillStyle = 'rgba(255,255,255,0.7)';
    ctx.fillText('Track your DSE portfolio — DSE Tracker', W / 2, H - 60);
  }

  private bestHolding(): Holding | undefined {
    return [...this.portfolio.holdings]
      .filter((h) => h.roiPercent !== undefined)
      .sort((a, b) => (b.roiPercent ?? 0) - (a.roiPercent ?? 0))[0];
  }

  private drawLogo(ctx: CanvasRenderingContext2D, x: number, y: number, size: number) {
    ctx.fillStyle = 'rgba(255,255,255,0.18)';
    this.roundRect(ctx, x, y, size, size, size * 0.22);
    ctx.fill();

    ctx.fillStyle = '#ffffff';
    const barWidth = size * 0.16;
    const baseY = y + size * 0.78;
    this.bar(ctx, x + size * 0.22, baseY, barWidth, size * 0.26);
    this.bar(ctx, x + size * 0.42, baseY, barWidth, size * 0.4);
    this.bar(ctx, x + size * 0.62, baseY, barWidth, size * 0.54);
  }

  private bar(ctx: CanvasRenderingContext2D, x: number, baseY: number, w: number, h: number) {
    this.roundRect(ctx, x, baseY - h, w, h, w * 0.25);
    ctx.fill();
  }

  private roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }
}
