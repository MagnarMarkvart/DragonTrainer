export const LANE_BG = '/images/pexels-nc-farm-bureau-mark-31125297.jpg';
export const WALL_BG = '/images/pexels-magda-ehlers-pexels-13246806.jpg';
export const PARCHMENT = '/vectors/Parchment--Arvin61r58.svg';

export const HUD_ASSETS = [
  '/vectors/chess-pawn-svgrepo-com.svg',
  '/vectors/level-four-svgrepo-com.svg',
  '/vectors/gold-pot-gold-svgrepo-com.svg',
  '/vectors/credit-cards-svgrepo-com.svg',
];

export function preloadAssets(urls: string[]): Promise<void> {
  return Promise.all(urls.map(preloadAsset)).then(() => undefined);
}

function preloadAsset(url: string): Promise<void> {
  return new Promise((resolve) => {
    const image = new Image();
    const finish = () => resolve();
    image.onload = () => {
      image.decode().then(finish, finish);
    };
    image.onerror = finish;
    image.src = url;
  });
}
