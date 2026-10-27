package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class CanvasInteractionMode {
    DRAW, // رسم با قلم
    PAN   // اسکرول صفحه با یک انگشت
}

data class BoardTheme(
    val id: String,
    val name: String,
    val bgColor: Color,
    val noteBgHex: String,
    val isDark: Boolean,
    val defaultPenColor: Color,
    val penPalette: List<Color>
)

val BoardThemes = listOf(
    BoardTheme(
        id = "white",
        name = "سفید",
        bgColor = Color(0xFFFFFFFF),
        noteBgHex = "#FEF3C7",
        isDark = false,
        defaultPenColor = Color(0xFF1E293B),
        penPalette = listOf(
            Color(0xFF1E293B), // مشکی تیره
            Color(0xFF2563EB), // آبی
            Color(0xFFDC2626), // قرمز
            Color(0xFF16A34A), // سبز
            Color(0xFFD97706), // کهربایی
            Color(0xFF9333EA)  // بنفش
        )
    ),
    BoardTheme(
        id = "black",
        name = "تخته سیاه",
        bgColor = Color(0xFF1E293B),
        noteBgHex = "#F1F5F9",
        isDark = true,
        defaultPenColor = Color(0xFFF8FAFC),
        penPalette = listOf(
            Color(0xFFF8FAFC), // سفید گچی
            Color(0xFFFDE047), // زرد گچی
            Color(0xFF38BDF8), // آبی روشن
            Color(0xFFFB7185), // صورتی گچی
            Color(0xFF4ADE80), // سبز گچی
            Color(0xFFFB923C)  // نارنجی
        )
    ),
    BoardTheme(
        id = "green",
        name = "تخته سبز",
        bgColor = Color(0xFF143D2B),
        noteBgHex = "#DCFCE7",
        isDark = true,
        defaultPenColor = Color(0xFFFEF08A),
        penPalette = listOf(
            Color(0xFFFEF08A), // زرد ملایم
            Color(0xFFF8FAFC), // سفید
            Color(0xFF67E8F9), // فیروزه‌ای
            Color(0xFFFCA5A5), // گلبهی
            Color(0xFF86EFAC), // سبز روشن
            Color(0xFFFDBA74)  // کهربایی
        )
    ),
    BoardTheme(
        id = "cream",
        name = "کرم کاهی",
        bgColor = Color(0xFFFAF5E9),
        noteBgHex = "#FEF3C7",
        isDark = false,
        defaultPenColor = Color(0xFF78350F),
        penPalette = listOf(
            Color(0xFF78350F), // قهوه‌ای جوهری
            Color(0xFF1E293B), // مشکی
            Color(0xFF0369A1), // آبی تیره
            Color(0xFFB91C1C), // زرشکی
            Color(0xFF15803D), // یشمی
            Color(0xFF6B21A8)  // بنفش
        )
    ),
    BoardTheme(
        id = "navy",
        name = "سرمه‌ای شب",
        bgColor = Color(0xFF0F172A),
        noteBgHex = "#E0F2FE",
        isDark = true,
        defaultPenColor = Color(0xFF38BDF8),
        penPalette = listOf(
            Color(0xFF38BDF8), // آبی نئونی
            Color(0xFFF8FAFC), // سفید ماه
            Color(0xFFFBBF24), // زرد ستاره‌ای
            Color(0xFFF472B6), // صورتی
            Color(0xFF34D399), // زمردی
            Color(0xFFA78BFA)  // بنفش روشن
        )
    )
)

@Composable
fun WhiteboardDrawingDialog(
    initialTitle: String = "",
    initialDrawingPath: String? = null,
    onDismiss: () -> Unit,
    onSaveDrawing: (title: String, imagePath: String, colorHex: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialTitle) }
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    // رنگ و تم بوم تخته
    var selectedBoardTheme by remember { mutableStateOf(BoardThemes[0]) }

    // تصویر قدیمی به عنوان فال‌بک (فقط در صورتی که فایل JSON وجود نداشته باشد)
    var legacyBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var legacyBitmapVersion by remember { mutableIntStateOf(0) }

    // بارگذاری هوشمند اطلاعات خطوط برداری و تم قبلی از فایل JSON
    LaunchedEffect(initialDrawingPath) {
        if (!initialDrawingPath.isNullOrBlank()) {
            val jsonFile = File(initialDrawingPath.replace(".png", ".json"))
            if (jsonFile.exists()) {
                try {
                    val content = jsonFile.readText()
                    val root = JSONObject(content)
                    val themeId = root.optString("themeId", "white")
                    val foundTheme = BoardThemes.find { it.id == themeId }
                    if (foundTheme != null) {
                        selectedBoardTheme = foundTheme
                    }
                    val arr = root.optJSONArray("strokes")
                    if (arr != null) {
                        val loadedStrokes = mutableListOf<DrawStroke>()
                        for (i in 0 until arr.length()) {
                            val sObj = arr.getJSONObject(i)
                            val c = sObj.getInt("c")
                            val w = sObj.getDouble("w").toFloat()
                            val e = sObj.optBoolean("e", false)
                            val ptsArr = sObj.getJSONArray("p")
                            val pts = mutableListOf<Offset>()
                            var j = 0
                            while (j < ptsArr.length() - 1) {
                                val x = ptsArr.getDouble(j).toFloat()
                                val y = ptsArr.getDouble(j + 1).toFloat()
                                pts.add(Offset(x, y))
                                j += 2
                            }
                            loadedStrokes.add(DrawStroke(pts, Color(c), w, e))
                        }
                        strokes.clear()
                        strokes.addAll(loadedStrokes)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // اگر فایل JSON مربوطه یافت نشد، تصویر Bitmap را بارگذاری و پس‌زمینه آن را شفاف می‌کنیم
                val pngFile = File(initialDrawingPath)
                if (pngFile.exists()) {
                    val rawBitmap = BitmapFactory.decodeFile(initialDrawingPath)
                    if (rawBitmap != null) {
                        legacyBitmap = makeBackgroundTransparent(rawBitmap)
                        legacyBitmapVersion += 1
                    }
                }
            }
        }
    }

    // اسکرول عمودی پیوسته و نامحدود به سبک گوگل کیپ
    var scrollY by remember { mutableFloatStateOf(0f) }
    // در حالت ویرایش یادداشت موجود، ابتدا ابزار دست پیش‌فرض فعال باشد تا کاربر بتواند بدون خط‌خطی تصادفی اسکرول کند
    var interactionMode by remember {
        mutableStateOf(
            if (!initialDrawingPath.isNullOrBlank()) CanvasInteractionMode.PAN else CanvasInteractionMode.DRAW
        )
    }

    // تنظیمات قلم
    var selectedColor by remember { mutableStateOf(selectedBoardTheme.defaultPenColor) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var isEraser by remember { mutableStateOf(false) }

    val viewportHeight = canvasSize.height.toFloat().coerceAtLeast(300f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // نوار بالای صفحه (Header & Action Buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // در صورت اسکرول به پایین، دکمه بازگشت به بالای صفحه نمایش داده می‌شود
                        if (scrollY > 50f) {
                            IconButton(onClick = { scrollY = 0f }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.VerticalAlignTop,
                                    contentDescription = "بازگشت به بالا",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // دکمه بازگشت به عقب (Undo)
                        IconButton(
                            onClick = {
                                if (strokes.isNotEmpty()) {
                                    strokes.removeAt(strokes.size - 1)
                                }
                            },
                            enabled = strokes.isNotEmpty(),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "بازگشت یک مرحله",
                                tint = if (strokes.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // پاک کردن کل صفحه (Clear All)
                        IconButton(
                            onClick = {
                                strokes.clear()
                                legacyBitmap = null
                                legacyBitmapVersion += 1
                                scrollY = 0f
                            },
                            enabled = strokes.isNotEmpty() || legacyBitmap != null,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "پاک کردن کل صفحه",
                                tint = if (strokes.isNotEmpty() || legacyBitmap != null) MaterialTheme.colorScheme.error else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // دکمه ذخیره طرح (Save)
                        Button(
                            onClick = {
                                if (strokes.isEmpty() && legacyBitmap == null) {
                                    Toast.makeText(context, "ابتدا چیزی روی تخته بنویسید یا بکشید", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val filePath = saveStrokes(
                                    context = context,
                                    strokes = strokes,
                                    themeId = selectedBoardTheme.id,
                                    bgColor = selectedBoardTheme.bgColor,
                                    initialPath = initialDrawingPath,
                                    legacyBitmap = legacyBitmap
                                )
                                if (filePath != null) {
                                    val finalTitle = title.trim().ifBlank { "طرح ${selectedBoardTheme.name}" }
                                    onSaveDrawing(finalTitle, filePath, selectedBoardTheme.noteBgHex)
                                } else {
                                    Toast.makeText(context, "خطا در ذخیره تصویر تخته", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ذخیره", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // عنوان یادداشت وایت‌برد
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("عنوان طرح یا یادداشت (اختیاری)", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                val currentIsEraser by rememberUpdatedState(isEraser)
                val currentSelectedColor by rememberUpdatedState(selectedColor)
                val currentStrokeWidth by rememberUpdatedState(strokeWidth)
                val currentInteractionMode by rememberUpdatedState(interactionMode)
                val currentBoardBgColor by rememberUpdatedState(selectedBoardTheme.bgColor)

                // تخته ترسیم پیوسته و نامحدود (Infinite Vertical Canvas)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(selectedBoardTheme.bgColor)
                        .border(
                            1.5.dp,
                            if (selectedBoardTheme.isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                            RoundedCornerShape(16.dp)
                        )
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                var isDrawing = false
                                var isTwoFingerScroll = false

                                awaitFirstDown(requireUnconsumed = false)

                                do {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }

                                    if (activePointers.size >= 2) {
                                        // حالت دو انگشتی: اسکرول کاملاً پیوسته، نرم و روان بدون لغو شدن coroutine
                                        if (isDrawing) {
                                            currentPoints = emptyList()
                                            isDrawing = false
                                        }
                                        isTwoFingerScroll = true

                                        val p1 = activePointers[0]
                                        val p2 = activePointers[1]
                                        val deltaY = (p1.position.y - p1.previousPosition.y + p2.position.y - p2.previousPosition.y) / 2f
                                        scrollY = (scrollY - deltaY).coerceAtLeast(0f)

                                        p1.consume()
                                        p2.consume()
                                    } else if (activePointers.size == 1 && !isTwoFingerScroll) {
                                        val change = activePointers[0]

                                        if (currentInteractionMode == CanvasInteractionMode.PAN) {
                                            // حالت دست: اسکرول عمودی با ۱ انگشت
                                            val deltaY = change.position.y - change.previousPosition.y
                                            scrollY = (scrollY - deltaY).coerceAtLeast(0f)
                                            change.consume()
                                        } else {
                                            // چه در حالت قلم و چه در حالت پاک‌کن موضعی: نقطه به stroke جاری اضافه می‌شود
                                            val worldPos = Offset(change.position.x, change.position.y + scrollY)

                                            // پاک کردن تدریجی از روی طرح قدیمی در صورت وجود
                                            if (currentIsEraser && legacyBitmap != null) {
                                                val bm = legacyBitmap!!
                                                val eraserRadius = currentStrokeWidth * 1.8f
                                                if (worldPos.x in 0f..bm.width.toFloat() && worldPos.y in 0f..bm.height.toFloat()) {
                                                    val bmCanvas = android.graphics.Canvas(bm)
                                                    val erasePaint = AndroidPaint().apply {
                                                        isAntiAlias = true
                                                        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
                                                    }
                                                    bmCanvas.drawCircle(worldPos.x, worldPos.y, eraserRadius, erasePaint)
                                                    legacyBitmapVersion += 1
                                                }
                                            }

                                            if (!isDrawing) {
                                                isDrawing = true
                                                currentPoints = listOf(worldPos)
                                            } else {
                                                currentPoints = currentPoints + worldPos
                                            }
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })

                                if (isDrawing && currentPoints.isNotEmpty()) {
                                    val eraser = currentIsEraser
                                    strokes.add(
                                        DrawStroke(
                                            points = currentPoints,
                                            color = if (eraser) currentBoardBgColor else currentSelectedColor,
                                            strokeWidth = if (eraser) currentStrokeWidth * 3.2f else currentStrokeWidth,
                                            isEraser = eraser
                                        )
                                    )
                                    currentPoints = emptyList()
                                }
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        // متغیر نسخه جهت بازطراحی بلادرنگ هنگام پاک کردن محتوای قدیمی
                        val bmVer = legacyBitmapVersion

                        // پس‌زمینه کاملاً خالص، یکدست و بدون هرگونه نقطه سفید مزاحم
                        translate(left = 0f, top = -scrollY) {
                            // در صورت وجود تصویر فال‌بک قدیمی
                            if (legacyBitmap != null) {
                                val imageBitmap = legacyBitmap!!.asImageBitmap()
                                drawImage(
                                    image = imageBitmap,
                                    dstOffset = IntOffset(0, 0)
                                )
                            }

                            // رسم تمام خطوط ذخیره‌شده بر روی رنگ پس‌زمینه زنده
                            strokes.forEach { stroke ->
                                val drawStroke = if (stroke.isEraser) stroke.copy(color = selectedBoardTheme.bgColor) else stroke
                                drawStrokeOnCanvas(this, drawStroke)
                            }

                            // رسم خط جاری در حال کشیدن
                            if (currentPoints.isNotEmpty()) {
                                val activeStroke = DrawStroke(
                                    points = currentPoints,
                                    color = if (isEraser) selectedBoardTheme.bgColor else selectedColor,
                                    strokeWidth = if (isEraser) strokeWidth * 3.2f else strokeWidth,
                                    isEraser = isEraser
                                )
                                drawStrokeOnCanvas(this, activeStroke)
                            }
                        }
                    }

                    // راهنمای اولیه هنگام خالی بودن تخته
                    if (strokes.isEmpty() && currentPoints.isEmpty() && legacyBitmap == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "بوم ${selectedBoardTheme.name}: با ۱ انگشت بنویسید یا نقاشی بکشید",
                                    color = if (selectedBoardTheme.isDark) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "برای اسکرول به پایین و ادامه نوشتن، با دو انگشت صفحه را بالا بکشید",
                                    color = if (selectedBoardTheme.isDark) Color(0xFF94A3B8) else Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // نوار ابزارهای پایین (رنگ بوم، حالت قلم/دست، رنگ‌های قلم، ضخامت، پاک‌کن)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ردیف اول: تغییر حالت (قلم / دست اسکرول)، رنگ‌های قلم و پاک‌کن
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // دکمه‌های سوئیچ بین قلم و ابزار دست (Pan / Scroll)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (interactionMode == CanvasInteractionMode.DRAW && !isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            interactionMode = CanvasInteractionMode.DRAW
                                            isEraser = false
                                        }
                                ) {
                                    Icon(
                                        Icons.Default.Draw,
                                        contentDescription = "قلم",
                                        tint = if (interactionMode == CanvasInteractionMode.DRAW && !isEraser) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(6.dp).size(18.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (interactionMode == CanvasInteractionMode.PAN && !isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            interactionMode = CanvasInteractionMode.PAN
                                            isEraser = false
                                        }
                                ) {
                                    Icon(
                                        Icons.Default.PanTool,
                                        contentDescription = "اسکرول با یک انگشت",
                                        tint = if (interactionMode == CanvasInteractionMode.PAN && !isEraser) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(6.dp).size(18.dp)
                                    )
                                }
                            }

                            // پالت رنگ‌های قلم (سازگار با تم فعلی بوم)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                selectedBoardTheme.penPalette.forEach { color ->
                                    val isSelected = !isEraser && selectedColor == color && interactionMode == CanvasInteractionMode.DRAW
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .clickable {
                                                selectedColor = color
                                                isEraser = false
                                                interactionMode = CanvasInteractionMode.DRAW
                                            }
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                } else {
                                                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.35f), CircleShape)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (color == Color(0xFFF8FAFC) || color == Color(0xFFFEF08A) || color == Color(0xFFFDE047)) Color.Black else Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // دکمه پاک‌کن هوشمند لمسی (Stroke Eraser)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isEraser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isEraser = !isEraser }
                                    .border(
                                        1.dp,
                                        if (isEraser) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoFixHigh,
                                        contentDescription = "پاک‌کن",
                                        tint = if (isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "پاک‌کن",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // ردیف دوم: انتخاب رنگ و جنس تخته (تغییر بلادرنگ و بدون کادر سفید)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "رنگ تخته:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BoardThemes.forEach { theme ->
                                    val isSelected = selectedBoardTheme.id == theme.id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = theme.bgColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedBoardTheme = theme
                                                if (!isEraser && !theme.penPalette.contains(selectedColor)) {
                                                    selectedColor = theme.defaultPenColor
                                                }
                                            }
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (theme.isDark) Color.White else Color(0xFF1E293B),
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                            Text(
                                                text = theme.name,
                                                fontSize = 9.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (theme.isDark) Color.White else Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ردیف سوم: انتخاب ضخامت قلم
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ضخامت خطوط:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val widths = listOf(3f to "باریک", 7f to "متوسط", 15f to "پهن")
                                widths.forEach { (widthVal, label) ->
                                    val isSelected = strokeWidth == widthVal
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { strokeWidth = widthVal }
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun drawStrokeOnCanvas(
    drawScope: DrawScope,
    stroke: DrawStroke
) {
    if (stroke.points.isEmpty()) return

    if (stroke.points.size == 1) {
        drawScope.drawCircle(
            color = stroke.color,
            radius = stroke.strokeWidth / 2f,
            center = stroke.points[0]
        )
        return
    }

    val path = Path().apply {
        moveTo(stroke.points[0].x, stroke.points[0].y)
        for (i in 1 until stroke.points.size) {
            val p0 = stroke.points[i - 1]
            val p1 = stroke.points[i]
            quadraticTo(p0.x, p0.y, (p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
        }
        lineTo(stroke.points.last().x, stroke.points.last().y)
    }

    drawScope.drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(
            width = stroke.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

/**
 * ذخیره هوشمند خطوط وکتور و تصویر PNG:
 * ذخیره همزمان فایل تصویر PNG (برای نمایش سریع در کارت‌ها) و فایل وکتور JSON (جهت ویرایش بی‌نقص در آینده)
 */
private fun saveStrokes(
    context: Context,
    strokes: List<DrawStroke>,
    themeId: String,
    bgColor: Color,
    initialPath: String?,
    legacyBitmap: Bitmap?
): String? {
    if (strokes.isEmpty() && legacyBitmap == null) return null

    return try {
        var minX = 0f
        var minY = 0f
        var maxX = (legacyBitmap?.width?.toFloat() ?: 0f)
        var maxY = (legacyBitmap?.height?.toFloat() ?: 0f)

        if (strokes.isNotEmpty()) {
            var sMinX = Float.MAX_VALUE
            var sMinY = Float.MAX_VALUE
            var sMaxX = Float.MIN_VALUE
            var sMaxY = Float.MIN_VALUE

            for (stroke in strokes) {
                for (p in stroke.points) {
                    if (p.x < sMinX) sMinX = p.x
                    if (p.y < sMinY) sMinY = p.y
                    if (p.x > sMaxX) sMaxX = p.x
                    if (p.y > sMaxY) sMaxY = p.y
                }
            }

            minX = if (legacyBitmap != null) minOf(0f, sMinX) else sMinX
            minY = if (legacyBitmap != null) minOf(0f, sMinY) else sMinY
            maxX = maxOf(maxX, sMaxX)
            maxY = maxOf(maxY, sMaxY)
        }

        val padding = 36f
        val calculatedWidth = (maxX - minX + padding * 2).toInt().coerceAtLeast(300)
        val calculatedHeight = (maxY - minY + padding * 2).toInt().coerceAtLeast(300)

        val finalWidth = calculatedWidth.coerceAtMost(2400)
        val finalHeight = calculatedHeight.coerceAtMost(6000)

        val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(bgColor.toArgb())

        val shiftX = -minX + padding
        val shiftY = -minY + padding

        if (legacyBitmap != null) {
            canvas.drawBitmap(legacyBitmap, shiftX, shiftY, null)
        }

        val paint = AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
        }

        strokes.forEach { stroke ->
            val strokeColor = if (stroke.isEraser) bgColor else stroke.color
            paint.color = strokeColor.toArgb()
            paint.strokeWidth = stroke.strokeWidth

            if (stroke.points.size == 1) {
                val pt = stroke.points[0]
                val pointPaint = AndroidPaint(paint).apply {
                    style = AndroidPaint.Style.FILL
                }
                canvas.drawCircle(pt.x + shiftX, pt.y + shiftY, stroke.strokeWidth / 2f, pointPaint)
            } else if (stroke.points.size > 1) {
                val androidPath = AndroidPath().apply {
                    moveTo(stroke.points[0].x + shiftX, stroke.points[0].y + shiftY)
                    for (i in 1 until stroke.points.size) {
                        val p0 = stroke.points[i - 1]
                        val p1 = stroke.points[i]
                        quadTo(
                            p0.x + shiftX,
                            p0.y + shiftY,
                            (p0.x + p1.x) / 2f + shiftX,
                            (p0.y + p1.y) / 2f + shiftY
                        )
                    }
                    lineTo(stroke.points.last().x + shiftX, stroke.points.last().y + shiftY)
                }
                canvas.drawPath(androidPath, paint)
            }
        }

        val dir = File(context.filesDir, "drawings")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        val pngFile = if (!initialPath.isNullOrBlank()) {
            File(initialPath)
        } else {
            File(dir, "drawing_${UUID.randomUUID()}.png")
        }

        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }

        // ذخیره ساختار وکتور خطوط در فایل JSON برای ویرایش بدون نقص در آینده
        val jsonFile = File(pngFile.absolutePath.replace(".png", ".json"))
        val root = JSONObject()
        root.put("themeId", themeId)
        val arr = JSONArray()
        for (stroke in strokes) {
            val sObj = JSONObject()
            sObj.put("c", stroke.color.toArgb())
            sObj.put("w", stroke.strokeWidth.toDouble())
            sObj.put("e", stroke.isEraser)
            val ptsArr = JSONArray()
            for (p in stroke.points) {
                ptsArr.put(p.x.toDouble())
                ptsArr.put(p.y.toDouble())
            }
            sObj.put("p", ptsArr)
            arr.put(sObj)
        }
        root.put("strokes", arr)
        jsonFile.writeText(root.toString())

        pngFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * تبدیل پس‌زمینه تک‌رنگ طرح‌های قدیمی به حالت شفاف تا با تغییر تم تخته، رنگ جدید به طور زنده نمایش داده شود
 */
private fun makeBackgroundTransparent(src: Bitmap, bgTolerance: Int = 30): Bitmap {
    val mutableBitmap = src.copy(Bitmap.Config.ARGB_8888, true)
    val width = mutableBitmap.width
    val height = mutableBitmap.height
    val pixels = IntArray(width * height)
    mutableBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val cornerSample = pixels[0]
    val bgR = if (android.graphics.Color.alpha(cornerSample) > 0) android.graphics.Color.red(cornerSample) else 255
    val bgG = if (android.graphics.Color.alpha(cornerSample) > 0) android.graphics.Color.green(cornerSample) else 255
    val bgB = if (android.graphics.Color.alpha(cornerSample) > 0) android.graphics.Color.blue(cornerSample) else 255

    for (i in pixels.indices) {
        val p = pixels[i]
        val r = android.graphics.Color.red(p)
        val g = android.graphics.Color.green(p)
        val b = android.graphics.Color.blue(p)

        if (Math.abs(r - bgR) <= bgTolerance &&
            Math.abs(g - bgG) <= bgTolerance &&
            Math.abs(b - bgB) <= bgTolerance
        ) {
            pixels[i] = 0 // تبدیل پس‌زمینه به شفاف
        }
    }
    mutableBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return mutableBitmap
}
