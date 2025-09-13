package com.example.posko24.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        interactionSource = interactionSource,
        singleLine = true,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                leadingIcon = if (leadingIcon != null) {
                    { Icon(imageVector = leadingIcon, contentDescription = null) }
                } else {
                    null
                },
                trailingIcon = trailingIcon,
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                )
                // Bagian 'container' yang salah telah dihapus dari sini
            )
        }
    )
}

