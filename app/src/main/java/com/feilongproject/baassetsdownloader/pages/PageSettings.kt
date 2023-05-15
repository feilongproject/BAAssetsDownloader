package com.feilongproject.baassetsdownloader.pages

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.feilongproject.baassetsdownloader.*
import com.feilongproject.baassetsdownloader.R
import com.feilongproject.baassetsdownloader.util.retrofitBuild
import com.microsoft.appcenter.distribute.Distribute


@Composable
fun PageSettings(modifier: Modifier, padding: PaddingValues) {
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(paddingValues = padding).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(modifier = maxWidth.padding(10.dp)) {
//            Text("123456")
            SettingCustomURL(context)
            Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))

            SettingCustomDownloadThread(context)
            Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.checkUpdate), modifier = Modifier.padding(end = 5.dp).clickable {
                    context.showToastResId(R.string.checkingUpdate)
                    Log.d("AppCenter", "开始版本检查")
                    Distribute.checkForUpdate()
                })
            }
            Divider(modifier = maxWidth.padding(top = 5.dp, bottom = 5.dp))

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingCustomURL(context: Context) {
    val options = listOf("default", "custom")
    var showOptions by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf(customApiUrl(context, "getConfigType", "")) }
    Log.d("FLP_DEBUG", "selectedOption: $selectedOption")
    var textValue by remember { mutableStateOf(TextFieldValue(customApiUrl(context, "getWithoutDefault", ""))) }

    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.selectApiUrl), modifier = Modifier.padding(end = 5.dp))

        if (selectedOption == "custom") com.feilongproject.baassetsdownloader.util.OutlinedTextField(
            value = textValue,
            onValueChange = {
                textValue = it
                customApiUrl(context, "custom", it.text)
            },
            leadingIcon = { Icon(Icons.Default.Link, null) },
            label = { Text(stringResource(R.string.typeApiUrl)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedLabelColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
//                    focusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth().padding(0.dp),
            shape = RoundedCornerShape(0.dp),
            singleLine = true,
        )
        else Row(
            modifier = Modifier.clickable { showOptions = true }.fillMaxHeight()
                .border(2.dp, MaterialTheme.colorScheme.onPrimary).padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (selectedOption) {
                "default" -> Text(stringResource(R.string.defaultApiUrl))
                "custom" -> Text(stringResource(R.string.customApiUrl))
            }
            Icon(Icons.Default.ArrowDropDown, null)
            DropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.onPrimary),
                offset = DpOffset(x = (-5).dp, y = 6.dp),
            ) {
                options.filterNot { it == "custom" }.forEach { option ->
                    DropdownMenuItem(onClick = {
                        selectedOption = option
                        showOptions = false
                        customApiUrl(context, option, option)
                    }, text = {
                        when (option) {
                            "default" -> Text(stringResource(R.string.defaultApiUrl))
                        }
                    })
                }

                DropdownMenuItem(onClick = {
                    selectedOption = "custom"
                    showOptions = false
                }, text = {
                    Text(stringResource(R.string.customApiUrl))
                })
            }
        }


    }
}

@Composable
fun SettingCustomDownloadThread(context: Context) {

    val pref = Pref(context, "config")
    var textValue by remember { mutableStateOf(TextFieldValue(pref.getValue("downloadThreadNum", 8).toString())) }
    var notNumber by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.threadNumber), modifier = Modifier.padding(end = 5.dp))
        OutlinedTextField(
            isError = notNumber,
            value = textValue,
            onValueChange = { v ->
                Log.d("FLP_DEBUG", v.toString())
                val setNum = v.text.toIntOrNull()
                if (setNum == null && v.text != "") context.showToastResId(R.string.notNumberWarn)
                else {
                    pref.putValue("downloadThreadNum", setNum.let {
                        notNumber = setNum == null
                        it ?: 8
                    })
                    textValue = v
                }
//                Log.d("FLP_DEBUG", "v: $v downloadThreadNum: ${pref.getValue("downloadThreadNum", 8)}")
            },
            leadingIcon = { if (notNumber) Icon(Icons.Default.Error, null) },
//            label = { Text(stringResource(R.string.typeApiUrl)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedLabelColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primaryContainer,
                errorContainerColor = MaterialTheme.colorScheme.errorContainer,
                errorBorderColor = MaterialTheme.colorScheme.errorContainer,
//                    focusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.fillMaxWidth(),//.padding(0.dp),
            shape = RoundedCornerShape(0.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

    }
}


fun customApiUrl(context: Context, type: String, value: String): String {
    Log.d("FLP_DEBUG", "setApiUrl $type $value")
    val perf = context.getSharedPreferences("config", Context.MODE_PRIVATE)
    val configApiUrl = perf.getString("customApiUrl", null)
    val editor = perf.edit()
    when (type) {
        "default" -> {
            editor.putString("customApiUrl", null)
        }

        "custom" -> {
            try {
                retrofitBuild(value)
                editor.putString("customApiUrl", value)
            } catch (err: Exception) {
                editor.putString("customApiUrl", null)
                Log.e("FLP_DEBUG", err.toString())
            }
        }

        "getConfigType" -> {
            return if (configApiUrl != null) "custom" else "default"
        }

        "getWithoutDefault" -> {
            return configApiUrl ?: ""
        }

    }
    editor.apply()
    configApiUrl.let {
        if (it != null) return it
        return context.getString(R.string.defaultApiUrlLink)
    }
}