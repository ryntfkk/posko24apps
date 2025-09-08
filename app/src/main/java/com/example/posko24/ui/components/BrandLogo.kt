package com.example.posko24.ui.components


import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest


/**
 * Displays the uploaded SVG logo via Coil SVG decoder.
 * Why: Android does not read SVG natively; this avoids manual conversion for in-app usage.
 */
@Composable
fun BrandLogo(
    modifier: Modifier = Modifier.size(64.dp),
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .build()


    val request = ImageRequest.Builder(context)
        .data("android.resource://${'$'}{context.packageName}/${'$'}{com.example.posko24.R.raw.brand_logo}")
        .crossfade(true)
        .build()


    AsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}