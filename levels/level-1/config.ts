export type LevelConfig = {
  id: string;
  name: string;
  tileSize: number;
  rows: number;
  cols: number;
  spawnX: number;
  spawnRow: number;
  floatingPlatformMinRow: number;
  floatingPlatformMaxRow: number;
  ravineColumns: number;
  defaultDragonId: string;
};

export const LEVEL_1_CONFIG: LevelConfig = {
  id: "level-1",
  name: "Emerald Foothills",
  tileSize: 16,
  rows: 12,
  cols: 110,
  spawnX: 28,
  spawnRow: 10,
  floatingPlatformMinRow: 4,
  floatingPlatformMaxRow: 6,
  ravineColumns: 25,
  defaultDragonId: "red-dragon",
};
