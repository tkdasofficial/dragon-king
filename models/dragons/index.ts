import {
  ATTACK_TIME,
  JAW_OPEN,
  LEG_SWING,
  RedDragon,
  STEP_RATE,
  type RedDragonHandle,
  type RedDragonProps,
} from "./red-dragon";

export { RedDragon, ATTACK_TIME, JAW_OPEN, LEG_SWING, STEP_RATE };
export type { RedDragonHandle, RedDragonProps };

export type DragonId = "red-dragon";

export type DragonModelEntry = {
  id: DragonId;
  name: string;
  folder: string;
  component: typeof RedDragon;
};

/** Multi-dragon registry — register new dragons from ./models/dragons/<dragon-name> here. */
export const DRAGON_MODELS: readonly DragonModelEntry[] = [
  {
    id: "red-dragon",
    name: "Red Dragon",
    folder: "./red-dragon",
    component: RedDragon,
  },
];

export const DEFAULT_DRAGON_ID: DragonId = "red-dragon";
