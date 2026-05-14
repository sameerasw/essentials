package com.sameerasw.essentials.ui.components.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

@Composable
fun LottieFeatureAnimation(
    resId: Int,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = primaryColor.toArgb(),
            keyPath = arrayOf("**")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = primaryColor.toArgb(),
            keyPath = arrayOf("**")
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            dynamicProperties = dynamicProperties,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
                .graphicsLayer(
                    compositingStrategy = CompositingStrategy.Offscreen,
                    clip = true,
                    shape = androidx.compose.foundation.shape.GenericShape { size, _ ->
                        val trim = size.height * 0.073f
                        addRect(
                            androidx.compose.ui.geometry.Rect(
                                0f,
                                trim,
                                size.width,
                                size.height - trim
                            )
                        )
                    }
                )
        )
    }
}
