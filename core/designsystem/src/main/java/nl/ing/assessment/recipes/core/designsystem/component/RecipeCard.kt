package nl.ing.assessment.recipes.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import nl.ing.assessment.recipes.core.designsystem.R
import nl.ing.assessment.recipes.core.designsystem.theme.sizing
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.designsystem.util.stripHtmlAndDecodeEntities
import nl.ing.assessment.recipes.core.domain.model.Recipe

@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onToggleFavorite: (Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanSummary = remember(recipe.summary) {
        recipe.summary?.stripHtmlAndDecodeEntities().orEmpty()
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(recipe.title)
                    recipe.readyInMinutes?.let { append(", $it minutes") }
                    recipe.healthScore?.let { append(", health score ${it.toInt()}") }
                    if (recipe.isFavorite) append(", favorited")
                }
            }
            .testTag("recipe_card_${recipe.id}"),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.sizing.elevationSmall)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (!recipe.image.isNullOrBlank()) {
                    AsyncImage(
                        model = recipe.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.sizing.recipeImageHeight)
                    )
                }
                FavoriteButton(
                    isFavorite = recipe.isFavorite,
                    onClick = { onToggleFavorite(recipe) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.spacing.medium)
                )
            }

            Column(modifier = Modifier.padding(MaterialTheme.spacing.extraLarge)) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (cleanSummary.isNotEmpty()) {
                    Text(
                        text = cleanSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = MaterialTheme.spacing.small)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.spacing.large),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recipe.readyInMinutes?.let { minutes ->
                        InfoPill(
                            icon = Icons.Outlined.Schedule,
                            text = stringResource(R.string.minutes_format, minutes),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    recipe.healthScore?.let { score ->
                        InfoPill(
                            icon = Icons.Outlined.Favorite,
                            text = stringResource(R.string.health_score_format, score.toInt()),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.sizing.iconSmall)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
