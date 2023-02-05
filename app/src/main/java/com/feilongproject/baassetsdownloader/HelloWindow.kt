package com.feilongproject.baassetsdownloader

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HelloWindow(
    onContinueClicked: () -> Unit,
) {
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