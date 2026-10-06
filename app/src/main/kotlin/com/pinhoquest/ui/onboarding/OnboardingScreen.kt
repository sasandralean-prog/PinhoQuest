package com.pinhoquest.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import com.pinhoquest.R
import com.pinhoquest.core.tag.SystemTagCatalog
import com.pinhoquest.domain.profile.GardenOwnerName
import com.pinhoquest.ui.reference.ReferenceHotspot
import com.pinhoquest.ui.reference.ReferenceRect

@Composable
fun OnboardingScreen(
    onComplete: (String, Set<String>) -> Unit,
    onInstallCreativeBrain: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }

    Box(modifier = modifier.fillMaxSize()) {
        when (step) {
            0 -> {
                ReferenceBackground(R.drawable.bg_start)
                ReferenceHotspot(
                    rect = ReferenceRect(0.23f, 0.82f, 0.54f, 0.12f),
                    contentDescription = "Começar",
                    onClick = { step = 1 },
                )
            }

            1 -> {
                ReferenceBackground(R.drawable.bg_onboarding_name)

                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    BasicTextField(
                        value = name,
                        onValueChange = { value ->
                            if (value.trim().codePointCount(0, value.trim().length) <= GardenOwnerName.MAX_CHARACTERS) {
                                name = value
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 22.sp,
                        ),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .clickable {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                            .offset(
                                x = maxWidth * 0.29f,
                                y = maxHeight * 0.43f,
                            )
                            .size(
                                width = maxWidth * 0.42f,
                                height = maxHeight * 0.075f,
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .semantics { contentDescription = "Seu nome" },
                    )
                }

                ReferenceHotspot(
                    rect = ReferenceRect(0.03f, 0.02f, 0.12f, 0.08f),
                    contentDescription = "Voltar",
                    onClick = { step = 0 },
                )
                ReferenceHotspot(
                    rect = ReferenceRect(0.27f, 0.72f, 0.46f, 0.11f),
                    contentDescription = "Confirmar nome",
                    onClick = {
                        if (GardenOwnerName.create(name.trim()).isSuccess) step = 2
                    },
                )
            }

            else -> {
                ReferenceBackground(R.drawable.bg_onboarding_tags)
                val rects = listOf(
                    ReferenceRect(0.18f, 0.40f, 0.22f, 0.075f),
                    ReferenceRect(0.40f, 0.40f, 0.22f, 0.075f),
                    ReferenceRect(0.62f, 0.40f, 0.22f, 0.075f),
                    ReferenceRect(0.18f, 0.49f, 0.22f, 0.075f),
                    ReferenceRect(0.40f, 0.49f, 0.22f, 0.075f),
                    ReferenceRect(0.62f, 0.49f, 0.22f, 0.075f),
                    ReferenceRect(0.18f, 0.59f, 0.22f, 0.075f),
                    ReferenceRect(0.40f, 0.59f, 0.22f, 0.075f),
                    ReferenceRect(0.62f, 0.59f, 0.22f, 0.075f),
                    ReferenceRect(0.18f, 0.69f, 0.22f, 0.075f),
                    ReferenceRect(0.40f, 0.69f, 0.22f, 0.075f),
                    ReferenceRect(0.62f, 0.69f, 0.22f, 0.075f),
                )
                SystemTagCatalog.all.take(rects.size).forEachIndexed { index, tag ->
                    ReferenceHotspot(
                        rect = rects[index],
                        contentDescription = tag.label,
                        onClick = {
                            selected = if (tag.id.value in selected) {
                                selected - tag.id.value
                            } else {
                                selected + tag.id.value
                            }
                        },
                    )
                }
                ReferenceHotspot(
                    rect = ReferenceRect(0.30f, 0.80f, 0.40f, 0.11f),
                    contentDescription = "Continuar",
                    onClick = { onComplete(name.trim(), selected) },
                )
                ReferenceHotspot(
                    rect = ReferenceRect(0.03f, 0.02f, 0.12f, 0.08f),
                    contentDescription = "Voltar",
                    onClick = { step = 1 },
                )
            }
        }
    }
}

@Composable
private fun ReferenceBackground(resource: Int) {
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
    )
}
