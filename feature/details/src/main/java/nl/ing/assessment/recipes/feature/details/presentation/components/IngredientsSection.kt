package nl.ing.assessment.recipes.feature.details.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.domain.model.Ingredient
import nl.ing.assessment.recipes.feature.details.R

@Composable
fun IngredientsSection(
    ingredients: List<Ingredient>,
    modifier: Modifier = Modifier,
) {
    if (ingredients.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.details_ingredients),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
        ingredients.forEach { ingredient ->
            Text(
                text = "\u2022 ${ingredient.original}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
        }
    }
}
