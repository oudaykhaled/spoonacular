package nl.ing.assessment.recipes.core.designsystem.util

import androidx.core.text.HtmlCompat

/**
 * Strips HTML tags and decodes entities (e.g. `&amp;` → `&`, `&lt;` → `<`).
 * Uses [HtmlCompat.fromHtml] for proper entity decoding, unlike a plain regex approach.
 */
fun String.stripHtmlAndDecodeEntities(): String =
    HtmlCompat.fromHtml(this, HtmlCompat.FROM_HTML_MODE_LEGACY)
        .toString()
        .trim()
