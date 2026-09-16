package com.nj031.onetask.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nj031.onetask.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CROP_MAX_DIMENSION = 1600
private const val CROP_MIN_USER_SCALE = 1f
private const val CROP_MAX_USER_SCALE = 4f
private val CropFrameSize = 260.dp

/**
 * Full-screen crop step shown right after picking (or re-selecting) a profile photo, and reused
 * to re-crop the already-saved one. Loads the source off the main thread - downsampled so a
 * full-resolution camera photo doesn't blow up memory or make gestures laggy, and EXIF-rotated so
 * a sideways gallery photo doesn't crop sideways - then lets the user pinch-zoom and drag to
 * reposition it behind a fixed circular frame. Cancel dismisses without touching
 * EditProfileScreen's existing photo state; Done hands back a cropped square Bitmap that
 * EditProfileScreen holds in memory until its own "Save Changes" persists it.
 */
@Composable
fun ProfilePhotoCropDialog(
    sourceUri: Uri,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var scale by remember(sourceUri) { mutableStateOf(1f) }
    var offset by remember(sourceUri) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val frameSizePx = with(density) { CropFrameSize.toPx() }

    LaunchedEffect(sourceUri) {
        sourceBitmap = withContext(Dispatchers.IO) {
            runCatching { loadBitmapForCrop(context, sourceUri) }.getOrNull()
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val bitmap = sourceBitmap
            val baseScale = remember(bitmap) {
                bitmap?.let { frameSizePx / min(it.width, it.height).toFloat() } ?: 1f
            }

            if (bitmap != null) {
                CropArea(
                    bitmap = bitmap,
                    baseScale = baseScale,
                    frameSizePx = frameSizePx,
                    scale = scale,
                    offset = offset,
                    onTransform = { newScale, newOffset ->
                        scale = newScale
                        offset = newOffset
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(id = R.string.cancel), color = Color.White)
                }
                Text(
                    text = stringResource(id = R.string.profile_edit_crop_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                TextButton(
                    enabled = bitmap != null,
                    onClick = {
                        if (bitmap != null) {
                            onDone(cropBitmap(bitmap, baseScale * scale, offset, frameSizePx))
                        }
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.profile_edit_crop_done),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CropArea(
    bitmap: Bitmap,
    baseScale: Float,
    frameSizePx: Float,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val nativeWidthDp = with(density) { bitmap.width.toFloat().toDp() }
    val nativeHeightDp = with(density) { bitmap.height.toFloat().toDp() }
    val frameRadius = frameSizePx / 2f
    val currentScale = rememberUpdatedState(scale)
    val currentOffset = rememberUpdatedState(offset)

    Box(
        modifier = modifier
            .pointerInput(bitmap) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (currentScale.value * zoom).coerceIn(CROP_MIN_USER_SCALE, CROP_MAX_USER_SCALE)
                    val displayedWidth = bitmap.width * baseScale * newScale
                    val displayedHeight = bitmap.height * baseScale * newScale
                    val maxOffsetX = max(0f, displayedWidth / 2f - frameRadius)
                    val maxOffsetY = max(0f, displayedHeight / 2f - frameRadius)
                    val newOffset = Offset(
                        (currentOffset.value.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                        (currentOffset.value.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    )
                    onTransform(newScale, newOffset)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(nativeWidthDp, nativeHeightDp)
                .graphicsLayer {
                    scaleX = baseScale * scale
                    scaleY = baseScale * scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        // The dimmed scrim outside the crop circle is punched out via a native saveLayer +
        // PorterDuff CLEAR, rather than Compose's own experimental compositing-strategy API, so
        // this doesn't depend on an unstable opt-in.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val scrimPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb((0.55f * 255).toInt(), 0, 0, 0)
                }
                val clearPaint = android.graphics.Paint().apply {
                    xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                    isAntiAlias = true
                }
                val saveCount = canvas.nativeCanvas.saveLayer(0f, 0f, size.width, size.height, null)
                canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, scrimPaint)
                canvas.nativeCanvas.drawCircle(center.x, center.y, frameRadius, clearPaint)
                canvas.nativeCanvas.restoreToCount(saveCount)
            }
            drawCircle(color = Color.White, radius = frameRadius, center = center, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

/** Extracts the square region of [source] currently visible inside the circular crop frame,
 * given the on-screen transform the user left it in. [totalScale] maps source-bitmap pixels to
 * on-screen pixels (base "cover" scale times the user's pinch-zoom); [offset] is how far the
 * user has panned the image, in on-screen pixels. */
private fun cropBitmap(source: Bitmap, totalScale: Float, offset: Offset, frameSizePx: Float): Bitmap {
    val cropSize = frameSizePx / totalScale
    val centerX = source.width / 2f - offset.x / totalScale
    val centerY = source.height / 2f - offset.y / totalScale
    val maxLeft = max(0, source.width - cropSize.roundToInt())
    val maxTop = max(0, source.height - cropSize.roundToInt())
    val left = (centerX - cropSize / 2f).roundToInt().coerceIn(0, maxLeft)
    val top = (centerY - cropSize / 2f).roundToInt().coerceIn(0, maxTop)
    val size = cropSize.roundToInt()
        .coerceAtMost(min(source.width - left, source.height - top))
        .coerceAtLeast(1)
    return Bitmap.createBitmap(source, left, top, size, size)
}

/** Decodes [uri] downsampled to a sane max dimension for a profile photo (avoids OOM/jank on a
 * full-resolution camera photo) and corrects for EXIF rotation, so a sideways gallery photo
 * doesn't end up cropped sideways. */
private fun loadBitmapForCrop(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
    val originalWidth = bounds.outWidth
    val originalHeight = bounds.outHeight
    if (originalWidth <= 0 || originalHeight <= 0) return null

    var sampleSize = 1
    while (originalWidth / (sampleSize * 2) >= CROP_MAX_DIMENSION && originalHeight / (sampleSize * 2) >= CROP_MAX_DIMENSION) {
        sampleSize *= 2
    }

    val decoded = resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    } ?: return null

    val rotationDegrees = resolver.openInputStream(uri)?.use { stream ->
        runCatching { exifRotationDegrees(ExifInterface(stream)) }.getOrDefault(0)
    } ?: 0

    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    } else {
        decoded
    }
}

private fun exifRotationDegrees(exif: ExifInterface): Int =
    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
