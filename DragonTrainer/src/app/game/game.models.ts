export interface Reputation {
  people: number;
  state: number;
  underworld: number;
}

export interface GameState {
  id: string;
  gameId: string;
  lives: number;
  gold: number;
  level: number;
  score: number;
  highScore: number;
  turn: number;
  reputation?: Reputation | null;
}

export interface TaskAd {
  adId: string;
  message: string;
  reward: number;
  expiresIn: number;
  difficulty: string;
}

export interface ShopItem {
  id: string;
  name: string;
  cost: number;
}

export interface SolveResult {
  adId: string;
  message: string;
  success: boolean;
  lives: number;
  gold: number;
  score: number;
  highScore: number;
  turn: number;
}

export interface PurchaseResult {
  itemId: string;
  success: boolean;
  lives: number;
  gold: number;
  level: number;
  turn: number;
}

export interface BotStep {
  action: string;
  text: string;
  taskMessage?: string | null;
  success?: boolean | null;
  lives: number;
  gold: number;
  level: number;
  score: number;
  highScore: number;
  turn: number;
  reputation?: Reputation | null;
  finished: boolean;
}
