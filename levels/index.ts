import { initLevel1, LEVEL_1_CONFIG, type InitializedLevel, type LevelConfig } from "./level-1";

export { initLevel1, LEVEL_1_CONFIG };
export type { InitializedLevel, LevelConfig };

export type LevelId = "level-1";

export type LevelRegistryEntry = {
  id: LevelId;
  folder: string;
  config: LevelConfig;
  init: () => InitializedLevel;
};

/** Multi-level registry — register new levels from ./level-2, ./level-3, etc. here. */
export const LEVELS: readonly LevelRegistryEntry[] = [
  {
    id: "level-1",
    folder: "./level-1",
    config: LEVEL_1_CONFIG,
    init: initLevel1,
  },
];

export const DEFAULT_LEVEL_ID: LevelId = "level-1";

export function getLevelById(id: LevelId): InitializedLevel {
  const entry = LEVELS.find((l) => l.id === id) ?? LEVELS[0]!;
  return entry.init();
}
