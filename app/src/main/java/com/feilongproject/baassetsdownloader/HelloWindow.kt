package com.feilongproject.baassetsdownloader

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HelloWindow(
    onContinueClicked: () -> Unit,
) {
    var showThisWindow by remember { mutableStateOf(true) }
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.welcomeUse))
        Text(stringResource(R.string.app_name))
        Button(
            modifier = Modifier.padding(vertical = 24.dp),
            onClick = onContinueClicked
        ) {
            Text(text = stringResource(R.string.continueDot))
        }

        Row (
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ){
            Checkbox(
                checked = showThisWindow,
                onCheckedChange = {
                    howToShowHelloWindow(context, true, it)
                    showThisWindow = it
                }
            )
            Text(stringResource(R.string.showThisWindow))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Text(
                text = stringResource(R.string.forAndroid11HelloWindow),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.inversePrimary, shape = RoundedCornerShape(10.dp))
                    .padding(15.dp)
            )
    }
}