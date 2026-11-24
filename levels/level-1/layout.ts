export type TileSymbol =
  | "."
  | "G"
  | "D"
  | "S"
  | "M"
  | "C"
  | "^"
  | "B"
  | "*"
  | "P"
  | "F"
  | "E";

export type SlimePatrolSpec = {
  id: number;
  spawnCol: number;
  spawnRow: number;
  minCol: number;
  maxCol: number;
  vx: number;
};

/**
 * Level 1 layout (12 rows x 110 cols):
 * - Floating moss platforms ('M') positioned cleanly in rows 3..6 for generous head clearance.
 * - Coins ('*') arranged above high floating platforms and along ravine jump arcs.
 * - Open ravines ('..' across rows 10..11) and spike pits ('^') punish misjudged jumps.
 */
export const LEVEL_1_ROWS: readonly string[] = [
  "..............................................................................................................",
  "..............................................................................................................",
  "..............................**.....................*........................................................",
  ".............................*..*...................*.*...............*.*.....................................",
  "......***........**..........MMMM...................MMM......**.......MMM........**.................***.......",
  "......MMM.......MMMM..................**..........*..........MM.................MMMM......**........MMM.......",
  ".......................**.............MM..........S.........*...*.........................MM*.................",
  "............*.....................................S.........................C.....................S.....F.....",
  "................................S...........C.....S..................S......C.....................S....SSS....",
  "...B...S.........E..........^^..S...........C.B...S.....P...........SS......C....E.....B........B.S...SDDD....",
  "GGGGGGGGGGG...GGGGGGGG....GGGGGGGGGG......GGGGGGGGGGGGGGGGG.......GGGG^^^GGGGGGGGGGGGGGGG.....GGGGGGGGGGGGGGGG",
  "DDDDDDDDDDD...DDDDDDDD....DDDDDDDDDD......DDDDDDDDDDDDDDDDD.......DDDDDDDDDDDDDDDDDDDDDDD.....DDDDDDDDDDDDDDDD",
];

export const LEVEL_1_SLIMES: readonly SlimePatrolSpec[] = [
  { id: 1, spawnCol: 17, spawnRow: 9, minCol: 14, maxCol: 20, vx: 28 },
  { id: 2, spawnCol: 81, spawnRow: 9, minCol: 78, maxCol: 86, vx: 28 },
];

export type LevelLayout = {
  rows: readonly string[];
  slimes: readonly SlimePatrolSpec[];
};

export function createLevel1Layout(): LevelLayout {
  return {
    rows: LEVEL_1_ROWS,
    slimes: LEVEL_1_SLIMES,
  };
}
