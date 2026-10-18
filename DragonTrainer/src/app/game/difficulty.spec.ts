import { difficultyStyle } from './difficulty';

describe('difficultyStyle', () => {
  it('gives a known rank its label and a light ink on the dark parchments', () => {
    expect(difficultyStyle('PIECE_OF_CAKE').label).toBe('Piece of cake');
    expect(difficultyStyle('PIECE_OF_CAKE').ink).toBe('#1c140c');
    expect(difficultyStyle('IMPOSSIBLE').ink).toBe('#f6efe4');
  });

  it('treats a name the board does not know as unknown', () => {
    expect(difficultyStyle('NOT_A_RANK').label).toBe('Unknown');
    expect(difficultyStyle('NOT_A_RANK').color).toBe(difficultyStyle('UNKNOWN').color);
  });
});
