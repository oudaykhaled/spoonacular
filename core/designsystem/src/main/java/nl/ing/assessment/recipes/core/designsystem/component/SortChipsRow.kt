package nl.ing.assessment.recipes.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import nl.ing.assessment.recipes.core.designsystem.R
import nl.ing.assessment.recipes.core.designsystem.theme.spacing
import nl.ing.assessment.recipes.core.domain.model.SortOrder

@StringRes
fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.RELEVANCE -> R.string.sort_relevance
    SortOrder.POPULARITY -> R.string.sort_popularity
    SortOrder.HEALTHINESS -> R.string.sort_healthiness
    SortOrder.TIME -> R.string.sort_time
    SortOrder.PRICE -> R.string.sort_price
}

@Composable
fun SortChipsRow(
    selected: SortOrder,
    onSelect: (SortOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orders = SortOrder.entries
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.extraLarge),
    ) {
        items(orders, key = { it.name }) { order ->
            FilterChip(
                selected = order == selected,
                onClick = { onSelect(order) },
                label = { Text(stringResource(order.labelRes())) },
            )
        }
    }
}
