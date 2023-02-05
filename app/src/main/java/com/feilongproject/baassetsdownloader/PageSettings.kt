package com.feilongproject.baassetsdownloader

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp


@Composable
fun PageSettings(modifier: Modifier = Modifier, padding: PaddingValues) {
//    val names = listOf("app", "res")

    Column(modifier = modifier.padding(paddingValues = padding).padding(vertical = 4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = maxWidth.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column {
                Row(
                    modifier = maxWidth.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.TODO))
                }

            }
        }
    }
}