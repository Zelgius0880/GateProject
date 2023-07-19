package com.zelgius.gateApp.compose.buttons

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.gateApp.R

@Composable
fun LightTimeDialog(
    time: Long = 0,
    onTimeSet: (Long) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var text by remember {
        mutableStateOf("$time")
    }

    var error by remember {
        mutableStateOf(false)
    }
    AlertDialog(
        shape = RoundedCornerShape(20.dp),
        backgroundColor = LocalElevationOverlay.current?.apply(MaterialTheme.colors.surface, 8.dp)?: MaterialTheme.colors.surface,
        onDismissRequest = onDismiss,
        text = {
            Column() {

                TextField(value = text, onValueChange = { text = it },
                    isError = error,
                    colors = TextFieldDefaults.textFieldColors(focusedLabelColor = MaterialTheme.colors.primaryVariant),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = {
                        Text(
                            text = stringResource(
                                id = R.string.light_on_time
                            )
                        )
                    },
                    trailingIcon = {
                        Text(text = "sec")
                    }
                )

                Text(
                    text = stringResource(id = R.string.light_on_time_description),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.secondaryVariant)
            ) {
                Text(stringResource(id = R.string.dismiss))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                text.toLongOrNull()?.let {
                    onTimeSet(it)
                } ?: run {
                    error = true
                }
            }) {
                Text(stringResource(id = R.string.ok))
            }
        },
    )
}


@Preview
@Composable
fun LightTimeDialogPreview() {
    LightTimeDialog()
}