import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard-shell',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-mark">DT</div>
          <div>
            <div class="brand-name">DSE Track</div>
            <div class="brand-sub">{{ auth.user()?.name || 'Investor' }}</div>
          </div>
        </div>

        <nav>
          <a routerLink="/dashboard/portfolio" routerLinkActive="active" class="nav-item">Portfolio</a>
          <a routerLink="/dashboard/stocks" routerLinkActive="active" class="nav-item">Fundamentals</a>
          <a routerLink="/dashboard/transactions" routerLinkActive="active" class="nav-item">Transactions</a>
          <a routerLink="/dashboard/alerts" routerLinkActive="active" class="nav-item">Alerts</a>
          <a routerLink="/dashboard/dividends" routerLinkActive="active" class="nav-item">Dividends</a>
          <a routerLink="/dashboard/calculator" routerLinkActive="active" class="nav-item">Calculator</a>
        </nav>

        <button class="btn btn-outline logout-btn" (click)="logout()">Log out</button>
      </aside>

      <main class="content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [
    `
      .shell {
        display: flex;
        min-height: 100vh;
      }
      .sidebar {
        width: 220px;
        flex-shrink: 0;
        background: var(--surface);
        border-right: 1px solid var(--border);
        display: flex;
        flex-direction: column;
        padding: 20px 14px;
        gap: 24px;
      }
      .brand {
        display: flex;
        align-items: center;
        gap: 10px;
      }
      .brand-mark {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        background: linear-gradient(135deg, var(--accent) 0%, #6366f1 100%);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 13px;
      }
      .brand-name {
        font-weight: 700;
        font-size: 14px;
      }
      .brand-sub {
        font-size: 11px;
        color: var(--text-3);
      }
      nav {
        display: flex;
        flex-direction: column;
        gap: 2px;
        flex: 1;
      }
      .nav-item {
        padding: 10px 12px;
        border-radius: var(--radius-sm);
        color: var(--text-2);
        text-decoration: none;
        font-size: 13px;
        font-weight: 600;
      }
      .nav-item:hover {
        background: var(--surface-2);
        color: var(--text-1);
      }
      .nav-item.active {
        background: var(--accent-bg);
        color: var(--accent);
      }
      .logout-btn {
        width: 100%;
      }
      .content {
        flex: 1;
        padding: 28px 32px;
        max-width: 1100px;
      }
      @media (max-width: 720px) {
        .shell {
          flex-direction: column;
        }
        .sidebar {
          width: 100%;
          flex-direction: row;
          align-items: center;
          overflow-x: auto;
        }
        nav {
          flex-direction: row;
        }
        .content {
          padding: 20px 16px;
        }
      }
    `,
  ],
})
export class DashboardShell {
  constructor(
    public auth: AuthService,
    private router: Router,
  ) {}

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
