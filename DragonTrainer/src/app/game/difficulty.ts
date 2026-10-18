export interface DifficultyStyle {
  color: string;
  label: string;
  ink: string;
}

const DIFFICULTIES: Record<string, DifficultyStyle> = {
  SURE_THING: { color: '#3f8f45', label: 'Sure thing', ink: '#1c140c' },
  WALK_IN_THE_PARK: { color: '#5a9e3a', label: 'Walk in the park', ink: '#1c140c' },
  PIECE_OF_CAKE: { color: '#8aa83a', label: 'Piece of cake', ink: '#1c140c' },
  QUITE_LIKELY: { color: '#c4a035', label: 'Quite likely', ink: '#1c140c' },
  GAMBLE: { color: '#d4892a', label: 'Gamble', ink: '#1c140c' },
  HMM: { color: '#d06a28', label: 'Hmmm....', ink: '#1c140c' },
  RATHER_DETRIMENTAL: { color: '#c44e2a', label: 'Rather detrimental', ink: '#1c140c' },
  RISKY: { color: '#b4332c', label: 'Risky', ink: '#f6efe4' },
  PLAYING_WITH_FIRE: { color: '#8e2428', label: 'Playing with fire', ink: '#f6efe4' },
  SUICIDE_MISSION: { color: '#6a1a22', label: 'Suicide mission', ink: '#f6efe4' },
  IMPOSSIBLE: { color: '#3a1218', label: 'Impossible', ink: '#f6efe4' },
  UNKNOWN: { color: '#6b5b4b', label: 'Unknown', ink: '#f6efe4' },
};

export function difficultyStyle(name: string): DifficultyStyle {
  return DIFFICULTIES[name] ?? DIFFICULTIES['UNKNOWN'];
}
