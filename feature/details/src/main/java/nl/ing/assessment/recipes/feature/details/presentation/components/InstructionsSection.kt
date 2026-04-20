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
import nl.ing.assessment.recipes.core.domain.model.RecipeStep
import nl.ing.assessment.recipes.feature.details.R

@Composable
fun InstructionsSection(
    steps: List<RecipeStep>,
    fallbackInstructions: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.details_instructions),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))

        when {
            steps.isNotEmpty() -> InstructionSteps(steps)
            !fallbackInstructions.isNullOrBlank() -> Text(
                text = fallbackInstructions.stripHtml(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            else -> Text(
                text = stringResource(R.string.details_no_instructions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InstructionSteps(steps: List<RecipeStep>) {
    steps.sortedBy { it.number }.forEach { step ->
        Text(
            text = "${step.number}. ${step.step}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(MaterialTheme.spacing.medium))
    }
}

private fun String.stripHtml(): String = replace(HTML_TAG_REGEX, "").trim()

private val HTML_TAG_REGEX = Regex("<[^>]*>")
