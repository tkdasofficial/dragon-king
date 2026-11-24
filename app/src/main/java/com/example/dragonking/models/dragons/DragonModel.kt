package com.example.dragonking.models.dragons

/**
 * Metadata descriptor for a dragon model stored under `models/dragons/<dragon-id>/`.
 * Register additional dragons here as new character models are added to the project.
 */
data class DragonDescriptor(
    val id: String,
    val displayName: String,
    val modelPath: String
)

/**
 * Shared contract for code-driven dragon character models with articulated animation state.
 */
interface DragonCharacterModel {
    val descriptor: DragonDescriptor

    fun reset()

    fun step(
        dt: Float,
        walking: Boolean,
        mouthOpen: Boolean,
        attacking: Boolean,
        walkSpeed: Float = 1f,
        airborne: Boolean = false,
        doubleJumpBoost: Float = 0f
    )

    fun fire(seconds: Float = 1.6f)

    /** Approximate local screen-pixel offset of the mouth emitter relative to hitbox origin. */
    fun mouthOffsetPx(facingRight: Boolean, out: FloatArray)
}

/**
 * Central Kotlin registry mirroring `/models/dragons/index.ts`.
 */
object DragonRegistry {
    val RED_DRAGON = DragonDescriptor(
        id = "red-dragon",
        displayName = "Red Dragon",
        modelPath = "models/dragons/red-dragon"
    )

    val all: List<DragonDescriptor> = listOf(
        RED_DRAGON
    )

    val defaultDragon: DragonDescriptor = RED_DRAGON
}
