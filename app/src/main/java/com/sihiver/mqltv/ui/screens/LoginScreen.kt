package com.sihiver.mqltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.BuildConfig
import com.sihiver.mqltv.presentation.viewmodel.LoginUiState
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.BgDark
import com.sihiver.mqltv.ui.theme.TextMuted

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onUseDemo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1028),
                        BgDark,
                        Color(0xFF050508),
                    ),
                    radius = 1200f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xE612121C))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 40.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            LoginBrand()
            Text(
                text = "Masuk untuk menonton channel dan EPG dari server.",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            if (state.error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x22FC8181))
                        .border(1.dp, Color(0x55FC8181), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        text = state.error,
                        color = Color(0xFFFC8181),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            LoginTextField(
                label = "EMAIL",
                value = state.email,
                onValueChange = onEmailChange,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )

            LoginTextField(
                label = "PASSWORD",
                value = state.password,
                onValueChange = onPasswordChange,
                isPassword = true,
                imeAction = ImeAction.Done,
                onDone = onLogin,
            )

            TvFocusableBox(
                onClick = { if (!state.isLoading) onLogin() },
                modifier = Modifier.fillMaxWidth(),
                accentColor = AccentOrange,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = AccentOrange,
                focusedBackgroundColor = AccentOrange.copy(alpha = 0.85f),
                unfocusedBorderWidth = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.isLoading) "Memuat…" else "Masuk",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            Text(
                text = "Server: ${BuildConfig.API_BASE_URL}\n",
                fontSize = 10.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
            )
        }
    }
}

@Composable
private fun LoginBrand() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "MQL",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp,
            )
            Text(
                text = "TV",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = AccentOrange,
                letterSpacing = 4.sp,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "ANDROID TV",
            fontSize = 10.sp,
            color = TextMuted,
            letterSpacing = 3.sp,
        )
    }
}

@Composable
private fun LoginTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x14FFFFFF))
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) AccentOrange else Color(0x22FFFFFF),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                ),
                singleLine = true,
                cursorBrush = SolidColor(AccentOrange),
                visualTransformation = if (isPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onDone?.invoke() },
                ),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = if (isPassword) "••••••••" else "ketik di sini…",
                            color = Color(0xFF666666),
                            fontSize = 14.sp,
                        )
                    }
                    inner()
                },
            )
        }
    }
}
