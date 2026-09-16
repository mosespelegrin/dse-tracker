import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RevealOnScrollDirective } from '../../core/reveal-on-scroll.directive';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, RevealOnScrollDirective],
  template: `
    <header class="site-header">
      <div class="brand">
        <img src="logo-mark.png" alt="DSE Tracker" class="brand-logo" />
        <span class="brand-title">DSE Tracker</span>
      </div>
      <nav>
        <a routerLink="/login">Log in</a>
        <a routerLink="/register" class="btn btn-sm">Sign up</a>
      </nav>
    </header>

    <main>
      <section class="hero">
        <div class="hero-glow" aria-hidden="true"></div>
        <h1 class="enter enter-1">Your DSE portfolio, finally in one place</h1>
        <p class="enter enter-2">
          Track holdings, P&amp;L, and sector exposure across the Dar es Salaam Stock Exchange.
          Built for retail investors who want clarity, not clutter.
        </p>
        <a routerLink="/register" class="btn enter enter-3">Get started free →</a>

        <div class="preview card enter enter-4">
          <div class="preview-row preview-head">
            <div>Ticker</div><div>Price</div><div>Today</div><div>Div Yield</div>
          </div>
          @for (row of preview; track row.ticker; let i = $index) {
            <div class="preview-row" [style.animation-delay.ms]="500 + i * 80">
              <div><strong>{{ row.ticker }}</strong><span class="hint">{{ row.name }}</span></div>
              <div class="mono">{{ row.price }}</div>
              <div class="mono" [class.badge-green]="row.up" [class.badge-red]="!row.up">{{ row.change }}</div>
              <div class="mono badge-green">{{ row.yield }}</div>
            </div>
          }
        </div>
      </section>

      <section class="features">
        <p class="eyebrow" appReveal>Built for DSE investors</p>
        <h2 appReveal>Less spreadsheets. More signal.</h2>
        <div class="grid grid-3">
          <div class="card lift" appReveal style="transition-delay: 0s">
            <h3>Live holdings</h3>
            <p>Every ticker, every lot, every average cost. See unrealized P&amp;L the moment prices move on the DSE.</p>
          </div>
          <div class="card lift" appReveal style="transition-delay: 0.1s">
            <h3>Sector exposure</h3>
            <p>Know how concentrated you are in banks, telecom, or cement before the market reminds you.</p>
          </div>
          <div class="card lift" appReveal style="transition-delay: 0.2s">
            <h3>Fundamental analysis</h3>
            <p>P/E, P/B, ROE, dividend yield — computed live from current price, right alongside your holdings.</p>
          </div>
        </div>
      </section>

      <section class="quote" appReveal>
        <blockquote>
          "I used to maintain three spreadsheets to track my DSE positions. Now I open one tab and see everything —
          cost basis, today's move, sector mix."
        </blockquote>
      </section>

      <section class="final" appReveal>
        <h2>Your portfolio isn't going to track itself.</h2>
        <p>Two minutes to set up. No brokerage login. Just clarity on your DSE positions, starting now.</p>
        <a routerLink="/register" class="btn">Start tracking now →</a>
      </section>
    </main>

    <footer class="site-footer">
      <div class="footer-brand">
        <img src="logo-mark.png" alt="DSE Tracker" class="footer-logo" />
        <span>DSE Tracker</span>
      </div>
      <span>© 2026</span>
    </footer>
  `,
  styles: [
    `
      :host {
        display: block;
      }
      .site-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 20px clamp(16px, 4vw, 48px);
        border-bottom: 1px solid var(--border);
      }
      .brand {
        display: flex;
        align-items: center;
        gap: 10px;
        font-weight: 700;
        font-size: 16px;
      }
      .brand-logo {
        width: 36px;
        height: 36px;
        border-radius: 9px;
        object-fit: cover;
        box-shadow: 0 0 12px rgba(0, 190, 255, 0.4);
        border: 1px solid rgba(0, 210, 255, 0.3);
      }
      .footer-brand {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
      }
      .footer-logo {
        width: 22px;
        height: 22px;
        border-radius: 5px;
        object-fit: cover;
      }
      nav {
        display: flex;
        align-items: center;
        gap: 16px;
      }
      nav a {
        color: var(--text-2);
        text-decoration: none;
        font-weight: 600;
        font-size: 13px;
      }
      main {
        max-width: 1000px;
        margin: 0 auto;
        padding: 0 clamp(16px, 4vw, 48px);
      }
      .hero {
        position: relative;
        text-align: center;
        padding: 64px 0 40px;
        overflow: hidden;
      }
      .hero-glow {
        position: absolute;
        top: -180px;
        left: 50%;
        width: 640px;
        height: 480px;
        transform: translateX(-50%);
        background: radial-gradient(closest-side, rgba(75, 142, 255, 0.35), transparent 70%);
        filter: blur(10px);
        animation: glowPulse 8s ease-in-out infinite;
        pointer-events: none;
        z-index: 0;
      }
      @keyframes glowPulse {
        0%, 100% {
          opacity: 0.6;
          transform: translateX(-50%) scale(1);
        }
        50% {
          opacity: 1;
          transform: translateX(-50%) scale(1.08);
        }
      }
      .hero > * {
        position: relative;
        z-index: 1;
      }
      .enter {
        opacity: 0;
        animation: fadeInUp 0.7s ease forwards;
      }
      .enter-1 {
        animation-delay: 0.05s;
      }
      .enter-2 {
        animation-delay: 0.15s;
      }
      .enter-3 {
        animation-delay: 0.25s;
      }
      .enter-4 {
        animation-delay: 0.35s;
      }
      .hero h1 {
        font-size: clamp(28px, 5vw, 44px);
        line-height: 1.15;
        max-width: 720px;
        margin: 0 auto 16px;
      }
      .hero p {
        color: var(--text-2);
        max-width: 560px;
        margin: 0 auto 28px;
        font-size: 15px;
      }
      .preview {
        margin: 48px auto 0;
        max-width: 640px;
        text-align: left;
      }
      .preview-row {
        display: grid;
        grid-template-columns: 2fr 1fr 1fr 1fr;
        gap: 12px;
        padding: 10px 4px;
        border-bottom: 1px solid var(--border-2);
        opacity: 0;
        animation: fadeInUp 0.5s ease forwards;
        font-size: 13px;
        align-items: center;
      }
      .preview-row:last-child {
        border-bottom: none;
      }
      .preview-head {
        color: var(--text-3);
        font-size: 11px;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }
      .hint {
        display: block;
        color: var(--text-3);
        font-size: 11px;
      }
      .features {
        padding: 56px 0;
        text-align: center;
      }
      .eyebrow {
        color: var(--accent);
        font-size: 12px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        margin-bottom: 8px;
      }
      .features h2 {
        font-size: 28px;
        margin-bottom: 32px;
      }
      .features .card {
        text-align: left;
      }
      .lift {
        transition: transform 0.2s ease, border-color 0.2s ease;
      }
      .lift:hover {
        transform: translateY(-4px);
        border-color: var(--accent);
      }
      .quote {
        padding: 40px 0;
        text-align: center;
      }
      .quote blockquote {
        max-width: 620px;
        margin: 0 auto;
        font-size: 17px;
        font-style: italic;
        color: var(--text-2);
      }
      .final {
        text-align: center;
        padding: 56px 0 80px;
      }
      .final h2 {
        font-size: 26px;
        margin-bottom: 10px;
      }
      .final p {
        color: var(--text-2);
        margin-bottom: 24px;
      }
      .site-footer {
        display: flex;
        justify-content: space-between;
        padding: 20px clamp(16px, 4vw, 48px);
        border-top: 1px solid var(--border);
        color: var(--text-3);
        font-size: 12px;
      }
    `,
  ],
})
export class Landing {
  readonly preview = [
    { ticker: 'CRDB', name: 'CRDB Bank', price: 'TZS 720', change: '+2.13%', up: true, yield: '5.10%' },
    { ticker: 'NMB', name: 'NMB Bank', price: 'TZS 5,420', change: '-0.37%', up: false, yield: '6.84%' },
    { ticker: 'TBL', name: 'Tanzania Breweries', price: 'TZS 10,800', change: '-0.92%', up: false, yield: '3.20%' },
    { ticker: 'VODA', name: 'Vodacom Tanzania', price: 'TZS 885', change: '+0.57%', up: true, yield: '4.72%' },
  ];
}
