import { LEVEL_1_CONFIG, type LevelConfig } from "./config";
import {
  createLevel1Layout,
  LEVEL_1_ROWS,
  LEVEL_1_SLIMES,
  type LevelLayout,
  type SlimePatrolSpec,
  type TileSymbol,
} from "./layout";

export { LEVEL_1_CONFIG, LEVEL_1_ROWS, LEVEL_1_SLIMES, createLevel1Layout };
export type { LevelConfig, LevelLayout, SlimePatrolSpec, TileSymbol };

export type InitializedLevel = {
  config: LevelConfig;
  layout: LevelLayout;
};

export function initLevel1(): InitializedLevel {
  return {
    config: LEVEL_1_CONFIG,
    layout: createLevel1Layout(),
  };
}
