export interface AuthResponse {
  token?: string;
  refreshToken?: string;
  name?: string;
  email?: string;
  message: string;
}

export interface Stock {
  id: number;
  ticker: string;
  companyName: string;
  sector: string;
  currentPrice: number | null;
  lastUpdated: string | null;

  // Fundamental ratios — null wherever the underlying inputs haven't been
  // entered yet. Always computed fresh server-side from the current price.
  peRatio: number | null;
  earningsYieldPercent: number | null;
  bookValuePerShare: number | null;
  pbRatio: number | null;
  marketCap: number | null;
  dividendYieldPercent: number | null;
  payoutRatioPercent: number | null;
  roePercent: number | null;
  roaPercent: number | null;
  debtToEquity: number | null;
  currentRatio: number | null;
  fundamentalsUpdatedAt: string | null;

  // Raw inputs the ratios above are computed from — used to prefill the edit form
  epsTtm: number | null;
  sharesOutstanding: number | null;
  netIncome: number | null;
  totalEquity: number | null;
  totalAssets: number | null;
  totalLiabilities: number | null;
  currentAssets: number | null;
  currentLiabilities: number | null;
  annualDividendPerShare: number | null;
}

export interface StockFundamentalsInput {
  epsTtm?: number | null;
  sharesOutstanding?: number | null;
  netIncome?: number | null;
  totalEquity?: number | null;
  totalAssets?: number | null;
  totalLiabilities?: number | null;
  currentAssets?: number | null;
  currentLiabilities?: number | null;
  annualDividendPerShare?: number | null;
}

export interface Holding {
  id: number;
  stockId: number;
  ticker: string;
  companyName: string;
  sector: string;
  shares: number;
  totalPaid: number;
  currentPrice?: number | null;
  currentValue?: number;
  gainLoss?: number;
  roiPercent?: number;
}

export interface Portfolio {
  holdings: Holding[];
  totalInvested: number;
  totalCurrentValue?: number;
  totalGainLoss?: number;
  overallRoiPercent?: number;
  totalRealizedGain: number;
}

export interface SectorAllocation {
  sector: string;
  invested: number;
  percentOfPortfolio: number;
}

export interface Transaction {
  id: number;
  ticker: string;
  companyName: string;
  type: 'BUY' | 'SELL';
  shares: number;
  totalPaid: number;
  sellPrice?: number;
  realizedGain?: number;
  date: string;
  notes?: string;
}

export interface Alert {
  id: number;
  stockTicker: string;
  companyName: string;
  conditionType: 'ABOVE' | 'BELOW';
  targetPrice: number;
  triggered: boolean;
  triggeredAt?: string;
  createdAt: string;
}

export interface Dividend {
  id: number;
  ticker: string;
  companyName: string;
  amountPerShare: number;
  shares: number;
  totalAmount: number;
  paymentDate: string;
  notes?: string;
}

export interface OrderMatchResult {
  row: number;
  ticker: string;
  type: string | null;
  shares: number | null;
  amount: number | null;
  date: string | null;
  status: string;
  message: string;
  matchedTransactionId: number | null;
}

export interface OrderImportResult {
  totalRows: number;
  matched: number;
  mismatched: number;
  notFound: number;
  rows: OrderMatchResult[];
  unmatchedAppTransactions: Transaction[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface ApiError {
  status: number;
  message: string;
  timestamp: string;
}
