package nl.ing.assessment.recipes.core.designsystem.util

/**
 * Number of items from the end of a lazy list at which the next page should be prefetched.
 * Used by both the ViewModel (to guard against redundant fetches) and the Screen
 * (to detect scroll-to-end events). Centralised here to prevent divergence.
 */
const val PrefetchThreshold = 4
