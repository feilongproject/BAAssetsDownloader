package com.feilongproject.baassetsdownloader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HelloWindow(
    onContinueClicked: (type: String) -> Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var notShowPermission by remember { mutableStateOf(true) }
        var showNotice by remember { mutableStateOf(false) }
        Text(stringResource(R.string.welcomeUse))
        Text(stringResource(R.string.app_name))
        Button(
            modifier = Modifier.padding(vertical = 24.dp),
            onClick = {
                notShowPermission = onContinueClicked(if (showNotice) "request" else "")
                showNotice = true
            }
        ) {
            Text(text = stringResource(R.string.continueDot))
        }

        if (!notShowPermission) {
            Text(
                text = stringResource(R.string.notGetAllPermissions),
                textAlign = TextAlign.Center,
            )
            Button(
                modifier = Modifier.padding(vertical = 24.dp),
                onClick = { onContinueClicked("force") }
            ) {
                Text(text = stringResource(R.string.forceContinue))
            }

        }

    }
}