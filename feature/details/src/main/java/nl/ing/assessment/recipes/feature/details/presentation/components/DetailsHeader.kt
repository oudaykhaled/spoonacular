package nl.ing.assessment.recipes.feature.details.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import nl.ing.assessment.recipes.core.designsystem.theme.sizing
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.domain.model.RecipeDetails
import nl.ing.assessment.recipes.feature.details.R

@Composable
fun DetailsHeader(
    details: RecipeDetails,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val imageUrl = details.recipe.image
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.sizing.recipeImageHeight)
                    .clip(RoundedCornerShape(MaterialTheme.spacing.large))
            )
            Spacer(Modifier.height(MaterialTheme.spacing.extraLarge))
        }

        Text(
            text = details.recipe.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        val servings = details.servings
        if (servings != null && servings > 0) {
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = stringResource(R.string.details_servings_format, servings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val summary = details.recipe.summary?.stripHtml()?.takeIf { it.isNotBlank() }
        if (summary != null) {
            Spacer(Modifier.height(MaterialTheme.spacing.extraLarge))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = MaterialTheme.spacing.none),
            )
        }
    }
}

private fun String.stripHtml(): String = replace(HTML_TAG_REGEX, "").trim()

private val HTML_TAG_REGEX = Regex("<[^>]*>")
