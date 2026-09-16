import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./pages/landing/landing').then((m) => m.Landing),
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register').then((m) => m.Register),
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./pages/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./pages/reset-password/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/dashboard-shell/dashboard-shell').then((m) => m.DashboardShell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'portfolio' },
      {
        path: 'portfolio',
        loadComponent: () => import('./pages/portfolio/portfolio').then((m) => m.PortfolioPage),
      },
      {
        path: 'stocks',
        loadComponent: () => import('./pages/stocks/stocks').then((m) => m.StocksPage),
      },
      {
        path: 'transactions',
        loadComponent: () => import('./pages/transactions/transactions').then((m) => m.TransactionsPage),
      },
      {
        path: 'alerts',
        loadComponent: () => import('./pages/alerts/alerts').then((m) => m.AlertsPage),
      },
      {
        path: 'dividends',
        loadComponent: () => import('./pages/dividends/dividends').then((m) => m.DividendsPage),
      },
      {
        path: 'calculator',
        loadComponent: () => import('./pages/calculator/calculator').then((m) => m.CalculatorPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
