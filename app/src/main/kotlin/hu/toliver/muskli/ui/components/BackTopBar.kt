package hu.toliver.muskli.ui.components

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.muskli.R
import hu.toliver.muskli.ui.theme.MuskliTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.cd_back))
            }
        },
        actions = { actions() },
        modifier = modifier,
    )
}

@Preview(name = "Back top bar", showBackground = true)
@Preview(name = "Back top bar: dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BackTopBarPreview() {
    MuskliTheme {
        BackTopBar(title = "Settings", onBack = {})
    }
}
