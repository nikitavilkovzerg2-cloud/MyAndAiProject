package com.example.stitch

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private var sourceBitmap: Bitmap? = null
    private var chart: ChartBuilder.Chart? = null
    private var imgAspect = 1f
    private var suppressAspect = false

    private lateinit var chartView: ChartView
    private lateinit var txtStatus: TextView
    private lateinit var txtStats: TextView
    private lateinit var legendLayout: LinearLayout
    private lateinit var etW: EditText
    private lateinit var etH: EditText
    private lateinit var etC: EditText
    private lateinit var spSpeckle: Spinner
    private lateinit var spCpp: Spinner
    private lateinit var btnBuild: Button
    private lateinit var btnPdf: Button
    private lateinit var btnPng: Button

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) loadImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        buildUI()
        btnBuild.setOnClickListener { buildChart() }
        btnPdf.setOnClickListener { exportPdf() }
        btnPng.setOnClickListener { exportPng() }
        etW.doAfter { if (!suppressAspect) syncAspect(true) }
        etH.doAfter { if (!suppressAspect) syncAspect(false) }
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F1115"))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.parseColor("#161A21"))
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Схема вышивки"
            setTextColor(Color.parseColor("#E8ECF4"))
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(Button(this).apply {
            text = "Фото"; isAllCaps = false
            setOnClickListener { pickImage.launch("image/*") }
        })
        root.addView(header)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(60))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        chartView = ChartView(this).apply { setBackgroundColor(Color.parseColor("#0B0D11")) }
        content.addView(chartView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(380)))

        txtStatus = TextView(this).apply {
            text = "Загрузите фото, чтобы начать"
            setTextColor(Color.parseColor("#93A0B5")); textSize = 12f
            setPadding(0, dp(8), 0, 0)
        }
        content.addView(txtStatus)

        content.addView(sectionLabel("РАЗМЕР СХЕМЫ (КРЕСТИКИ)"))
        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, 0) }
        etW = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER; setText("150")
            setTextColor(Color.parseColor("#E8ECF4")); hint = "Ширина"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        etH = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER; setText("150")
            setTextColor(Color.parseColor("#E8ECF4")); hint = "Высота"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) }
        }
        sizeRow.addView(etW); sizeRow.addView(etH)
        content.addView(sizeRow)

        content.addView(sectionLabel("ЧИСЛО ЦВЕТОВ"))
        val cRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, 0) }
        etC = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER; setText("30")
            setTextColor(Color.parseColor("#E8ECF4"))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        spSpeckle = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Без сглаживания","Сглаживание: мягко","Сглаживание: средне","Сглаживание: сильно"))
            setSelection(2)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f).apply { marginStart = dp(8) }
        }
        cRow.addView(etC); cRow.addView(spSpeckle)
        content.addView(cRow)

        btnBuild = Button(this).apply { text = "Создать схему"; isAllCaps = false; isEnabled = false }
        content.addView(btnBuild, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })

        content.addView(sectionLabel("ЭКСПОРТ"))
        val expRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(4), 0, 0) }
        btnPdf = Button(this).apply {
            text = "PDF"; isAllCaps = false; isEnabled = false
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        btnPng = Button(this).apply {
            text = "PNG"; isAllCaps = false; isEnabled = false
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(8) }
        }
        expRow.addView(btnPdf); expRow.addView(btnPng)
        content.addView(expRow)

        val cppRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, 0); gravity = Gravity.CENTER_VERTICAL }
        cppRow.addView(TextView(this).apply {
            text = "Крестиков на страницу PDF: "
            setTextColor(Color.parseColor("#93A0B5")); textSize = 12f
        })
        spCpp = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("40 × 40","50 × 50","60 × 60","70 × 70"))
            setSelection(1)
        }
        cppRow.addView(spCpp)
        content.addView(cppRow)

        txtStats = TextView(this).apply { setTextColor(Color.parseColor("#93A0B5")); textSize = 12f; setPadding(0, dp(12), 0, 0) }
        content.addView(txtStats)

        content.addView(sectionLabel("ЛЕГЕНДА"))
        legendLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(8), 0, 0) }
        content.addView(legendLayout)

        setContentView(root)
    }

    private fun sectionLabel(s: String) = TextView(this).apply {
        text = s; setTextColor(Color.parseColor("#93A0B5")); textSize = 12f
        setTypeface(typeface, Typeface.BOLD); setPadding(0, dp(16), 0, 0)
    }

    private fun syncAspect(fromWidth: Boolean) {
        if (imgAspect <= 0f) return
        suppressAspect = true
        try {
            if (fromWidth) {
                val w = etW.text.toString().toIntOrNull() ?: return
                etH.setText((w / imgAspect).toInt().coerceIn(10, 600).toString())
            } else {
                val h = etH.text.toString().toIntOrNull() ?: return
                etW.setText((h * imgAspect).toInt().coerceIn(10, 600).toString())
            }
        } finally { suppressAspect = false }
    }

    private fun loadImage(uri: Uri) {
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                try { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
            }
            if (bmp == null) { toast("Не удалось загрузить фото"); return@launch }
            sourceBitmap = bmp
            imgAspect = bmp.width.toFloat() / bmp.height.toFloat()
            syncAspect(true)
            txtStatus.text = "Фото: ${bmp.width}×${bmp.height}"
            btnBuild.isEnabled = true
        }
    }

    private fun buildChart() {
        val bmp = sourceBitmap ?: return
        val w = etW.text.toString().toIntOrNull()?.coerceIn(10, 600) ?: 150
        val h = etH.text.toString().toIntOrNull()?.coerceIn(10, 600) ?: 150
        val c = etC.text.toString().toIntOrNull()?.coerceIn(4, 64) ?: 30
        val sp = spSpeckle.selectedItemPosition
        btnBuild.isEnabled = false; btnPdf.isEnabled = false; btnPng.isEnabled = false
        txtStatus.text = "Строю схему..."
        lifecycleScope.launch {
            try {
                val ch = withContext(Dispatchers.Default) { ChartBuilder.build(bmp, w, h, c, sp) }
                val render = withContext(Dispatchers.Default) { ChartRenderer.render(ch, 20) }
                chart = ch
                chartView.setBitmap(render)
                txtStats.text = "Размер: ${ch.W} × ${ch.H} = ${ch.W * ch.H} крестиков\nЦветов: ${ch.palette.size}"
                rebuildLegend(ch)
                txtStatus.text = "Схема готова"
                btnPdf.isEnabled = true; btnPng.isEnabled = true
            } catch (e: Exception) {
                txtStatus.text = "Ошибка: ${e.message}"; e.printStackTrace()
            } finally { btnBuild.isEnabled = true }
        }
    }

    private fun rebuildLegend(ch: ChartBuilder.Chart) {
        legendLayout.removeAllViews()
        for (i in ch.palette.indices) {
            val c = ch.palette[i]
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setBackgroundColor(Color.parseColor("#161A21"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(4) }
            }
            val lum = (0.2126*c.r + 0.7152*c.g + 0.0722*c.b) / 255.0
            row.addView(TextView(this).apply {
                text = ch.symbols[i]; width = dp(40); height = dp(40)
                gravity = Gravity.CENTER
                setBackgroundColor(Color.rgb(c.r, c.g, c.b))
                setTextColor(if (lum > 0.55) Color.BLACK else Color.WHITE)
                textSize = 16f; typeface = Typeface.MONOSPACE
            })
            val info = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            info.addView(TextView(this).apply {
                text = c.code; setTextColor(Color.parseColor("#E8ECF4")); textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
            })
            info.addView(TextView(this).apply {
                text = c.name; setTextColor(Color.parseColor("#93A0B5")); textSize = 12f
            })
            row.addView(info)
            row.addView(TextView(this).apply {
                text = "${ch.counts[i]} шт."; setTextColor(Color.parseColor("#E8ECF4")); textSize = 12f
            })
            legendLayout.addView(row)
        }
    }

    private fun exportPdf() {
        val c = chart ?: return
        txtStatus.text = "PDF..."
        lifecycleScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) { PdfExporter.export(this@MainActivity, c, spCpp.selectedItemPosition) }
                txtStatus.text = "PDF сохранён в Загрузки"
                openFile(uri, "application/pdf")
            } catch (e: Exception) { txtStatus.text = "Ошибка PDF: ${e.message}" }
        }
    }

    private fun exportPng() {
        val c = chart ?: return
        txtStatus.text = "PNG..."
        lifecycleScope.launch {
            try {
                val uri = withContext(Dispatchers.IO) { PngExporter.export(this@MainActivity, c) }
                txtStatus.text = "PNG сохранён в Загрузки"
                openFile(uri, "image/png")
            } catch (e: Exception) { txtStatus.text = "Ошибка PNG: ${e.message}" }
        }
    }

    private fun openFile(uri: Uri, mime: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(i)
        } catch (_: Exception) { toast("Файл в папке Загрузки") }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}

private fun EditText.doAfter(action: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
        override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        override fun afterTextChanged(s: Editable?) { action(s?.toString() ?: "") }
    })
}

class ChartView(context: android.content.Context) : android.view.View(context) {
    private var bmp: Bitmap? = null
    private var scale = 1f; private var tx = 0f; private var ty = 0f
    private var minScale = 0.1f; private var maxScale = 8f
    private val matrix = Matrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    private val gesture = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent) = true
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            tx -= dx; ty -= dy; constrain(); invalidate(); return true
        }
    })
    private val scaleGesture = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean {
            val ns = (scale * d.scaleFactor).coerceIn(minScale, maxScale)
            val f = ns / scale
            tx = d.focusX - (d.focusX - tx) * f
            ty = d.focusY - (d.focusY - ty) * f
            scale = ns; constrain(); invalidate(); return true
        }
    })

    fun setBitmap(b: Bitmap) { bmp = b; reset() }

    private fun reset() {
        val b = bmp ?: return
        if (width == 0 || height == 0) return
        val fit = min(width.toFloat() / b.width, height.toFloat() / b.height)
        scale = fit; minScale = fit * 0.5f; maxScale = fit * 8f
        tx = (width - b.width * scale) / 2f
        ty = (height - b.height * scale) / 2f
        invalidate()
    }

    private fun constrain() {
        val b = bmp ?: return
        val dw = b.width * scale; val dh = b.height * scale
        tx = if (dw <= width) (width - dw) / 2f else tx.coerceIn(width - dw, 0f)
        ty = if (dh <= height) (height - dh) / 2f else ty.coerceIn(height - dh, 0f)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); reset() }
    override fun onTouchEvent(e: MotionEvent): Boolean { gesture.onTouchEvent(e); scaleGesture.onTouchEvent(e); return true }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = bmp ?: return
        matrix.reset(); matrix.postScale(scale, scale); matrix.postTranslate(tx, ty)
        canvas.drawBitmap(b, matrix, paint)
    }
}

object ChartRenderer {
    fun render(ch: ChartBuilder.Chart, cellSize: Int): Bitmap {
        val bmp = Bitmap.createBitmap(ch.W * cellSize, ch.H * cellSize, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp); cv.drawColor(Color.WHITE)
        val fill = Paint()
        val thin = Paint().apply { color = Color.argb(50,0,0,0); style = Paint.Style.STROKE; strokeWidth = 1f }
        val thick = Paint().apply { color = Color.argb(200,0,0,0); style = Paint.Style.STROKE; strokeWidth = 2f }
        val text = Paint().apply {
            textSize = cellSize * 0.62f; textAlign = Paint.Align.CENTER
            isAntiAlias = true; typeface = Typeface.MONOSPACE
        }
        for (y in 0 until ch.H) for (x in 0 until ch.W) {
            val c = ch.palette[ch.cells[y*ch.W + x]]
            fill.color = Color.rgb(c.r, c.g, c.b)
            cv.drawRect((x*cellSize).toFloat(), (y*cellSize).toFloat(),
                ((x+1)*cellSize).toFloat(), ((y+1)*cellSize).toFloat(), fill)
        }
        for (x in 0..ch.W) {
            val px = (x*cellSize).toFloat()
            cv.drawLine(px, 0f, px, (ch.H*cellSize).toFloat(), if (x%10==0) thick else thin)
        }
        for (y in 0..ch.H) {
            val py = (y*cellSize).toFloat()
            cv.drawLine(0f, py, (ch.W*cellSize).toFloat(), py, if (y%10==0) thick else thin)
        }
        if (cellSize >= 12) {
            for (y in 0 until ch.H) for (x in 0 until ch.W) {
                val idx = ch.cells[y*ch.W + x]; val c = ch.palette[idx]
                val lum = (0.2126*c.r + 0.7152*c.g + 0.0722*c.b) / 255.0
                text.color = if (lum > 0.55) Color.BLACK else Color.WHITE
                val baseline = (y + 0.5f) * cellSize - (text.ascent() + text.descent()) / 2f
                cv.drawText(ch.symbols[idx], (x + 0.5f) * cellSize, baseline, text)
            }
        }
        return bmp
    }
}

object ChartBuilder {

    data class Chart(
        val W: Int, val H: Int,
        val cells: IntArray,
        val palette: List<DmcColor>,
        val symbols: List<String>,
        val counts: IntArray
    )

    private val SYMBOLS: List<String> = (
        "ABCDEFGHJKLMNPRSTUVWXYZ" + "23456789" +
        "abcdefghjkmnpqrstuvwxyz" + "@#$%&*=?<>/!~^"
    ).map { it.toString() }

    fun build(src: Bitmap, W: Int, H: Int, nColors: Int, speckle: Int): Chart {
        val pixels = boxResample(src, W, H)
        val n = W * H
        val labs = FloatArray(n * 3)
        for (i in 0 until n) {
            val p = pixels[i]
            val lab = rgb2lab((p shr 16) and 0xFF, (p shr 8) and 0xFF, p and 0xFF)
            labs[i*3] = lab[0]; labs[i*3+1] = lab[1]; labs[i*3+2] = lab[2]
        }
        val sampleN = min(n, 20000)
        val sample = FloatArray(sampleN * 3)
        if (sampleN == n) labs.copyInto(sample) else {
            val step = n.toFloat() / sampleN
            for (i in 0 until sampleN) {
                val idx = (i * step).toInt().coerceAtMost(n - 1)
                sample[i*3] = labs[idx*3]; sample[i*3+1] = labs[idx*3+1]; sample[i*3+2] = labs[idx*3+2]
            }
        }
        val centroids = kmeans(sample, sampleN, nColors, 10)

        val dmcList = mutableListOf<DmcColor>()
        val used = mutableSetOf<String>()
        for (c in 0 until nColors) {
            val lab = floatArrayOf(centroids[c*3], centroids[c*3+1], centroids[c*3+2])
            
            // Сортируем ВСЮ палитру DMC по удаленности от текущего цвета
            val sortedDmc = DmcPalette.all.sortedBy {
                val dx = it.lab[0] - lab[0]
                val dy = it.lab[1] - lab[1]
                val dz = it.lab[2] - lab[2]
                dx*dx + dy*dy + dz*dz
            }
            
            // Берем первый цвет, которого еще нет в нашем списке
            for (dmc in sortedDmc) {
                if (used.add(dmc.code)) {
                    dmcList.add(dmc)
                    break
                }
            }
        }

        val cells = IntArray(n)
        for (i in 0 until n) {
            val lab = floatArrayOf(labs[i*3], labs[i*3+1], labs[i*3+2])
            cells[i] = DmcPalette.nearestIndex(lab, dmcList)
        }
        if (speckle > 0) despeckle(cells, W, H, dmcList.size, speckle)

        val counts = IntArray(dmcList.size)
        for (v in cells) counts[v]++
        val order = (0 until dmcList.size).sortedByDescending { counts[it] }
        val remap = IntArray(dmcList.size)
        val newPal = mutableListOf<DmcColor>(); val newCnt = IntArray(dmcList.size)
        for ((ni, oi) in order.withIndex()) {
            remap[oi] = ni; newPal.add(dmcList[oi]); newCnt[ni] = counts[oi]
        }
        for (i in cells.indices) cells[i] = remap[cells[i]]
        val symbols = newPal.indices.map { SYMBOLS[it % SYMBOLS.size] }
        return Chart(W, H, cells, newPal, symbols, newCnt)
    }

    private fun boxResample(src: Bitmap, W: Int, H: Int): IntArray {
        val sw = src.width; val sh = src.height
        val px = IntArray(sw * sh); src.getPixels(px, 0, sw, 0, 0, sw, sh)
        val out = IntArray(W * H)
        
        // Новая функция подавления пересвета
        fun compress(v: Float): Int {
            return if (v > 220f) (220f + (v - 220f) * 0.5f).toInt().coerceIn(0, 255) 
            else v.toInt().coerceIn(0, 255)
        }

        for (ty in 0 until H) {
            val y0 = (ty * sh) / H; val y1 = maxOf(y0 + 1, ((ty + 1) * sh) / H)
            for (tx in 0 until W) {
                val x0 = (tx * sw) / W; val x1 = maxOf(x0 + 1, ((tx + 1) * sw) / W)
                var r = 0L; var g = 0L; var b = 0L; var a = 0L; var cnt = 0
                for (y in y0 until minOf(y1, sh)) for (x in x0 until minOf(x1, sw)) {
                    val p = px[y * sw + x]
                    a += (p ushr 24) and 0xFF; r += (p ushr 16) and 0xFF
                    g += (p ushr 8) and 0xFF; b += p and 0xFF; cnt++
                }
                if (cnt == 0) cnt = 1
                val af = (a.toFloat() / cnt) / 255f
                val rrRaw = (r.toFloat()/cnt)*af + 255f*(1f-af)
                val ggRaw = (g.toFloat()/cnt)*af + 255f*(1f-af)
                val bbRaw = (b.toFloat()/cnt)*af + 255f*(1f-af)
                
                val rr = compress(rrRaw)
                val gg = compress(ggRaw)
                val bb = compress(bbRaw)
                
                out[ty * W + tx] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
        }
        return out
    }

    private fun kmeans(pts: FloatArray, n: Int, k: Int, iters: Int): FloatArray {
        val centers = FloatArray(k * 3); val dist = FloatArray(n)
        val first = Random.nextInt(n)
        centers[0] = pts[first*3]; centers[1] = pts[first*3+1]; centers[2] = pts[first*3+2]
        for (i in 0 until n) dist[i] = d2(pts, i, centers, 0)
        for (c in 1 until k) {
            var sum = 0f; for (i in 0 until n) sum += dist[i]
            var t = Random.nextFloat() * sum; var pick = n - 1
            for (i in 0 until n) { t -= dist[i]; if (t <= 0f) { pick = i; break } }
            centers[c*3] = pts[pick*3]; centers[c*3+1] = pts[pick*3+1]; centers[c*3+2] = pts[pick*3+2]
            for (i in 0 until n) {
                val d = d2(pts, i, centers, c)
                if (d < dist[i]) dist[i] = d
            }
        }
        val sum = FloatArray(k * 3); val cnt = IntArray(k)
        repeat(iters) {
            sum.fill(0f); cnt.fill(0)
            for (i in 0 until n) {
                var best = 0; var bd = Float.MAX_VALUE
                for (c in 0 until k) {
                    val d = d2(pts, i, centers, c)
                    if (d < bd) { bd = d; best = c }
                }
                cnt[best]++
                sum[best*3] += pts[i*3]; sum[best*3+1] += pts[i*3+1]; sum[best*3+2] += pts[i*3+2]
            }
            for (c in 0 until k) if (cnt[c] > 0) {
                centers[c*3] = sum[c*3]/cnt[c]; centers[c*3+1] = sum[c*3+1]/cnt[c]; centers[c*3+2] = sum[c*3+2]/cnt[c]
            }
        }
        return centers
    }

    private fun d2(pts: FloatArray, i: Int, centers: FloatArray, c: Int): Float {
        val dl = (pts[i*3] - centers[c*3]) * 0.6f
        val da = (pts[i*3+1] - centers[c*3+1]) * 1.4f
        val db = (pts[i*3+2] - centers[c*3+2]) * 1.4f
        return dl*dl + da*da + db*db
    }

    private fun despeckle(cells: IntArray, W: Int, H: Int, k: Int, strength: Int) {
        val thr = when (strength) { 1 -> 7; 2 -> 6; else -> 5 }
        val cnt = IntArray(k); val src = cells.copyOf()
        for (y in 0 until H) for (x in 0 until W) {
            cnt.fill(0)
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx; val ny = y + dy
                if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue
                cnt[src[ny * W + nx]]++
            }
            var bc = src[y * W + x]; var bcn = 0
            for (c in 0 until k) if (cnt[c] > bcn) { bcn = cnt[c]; bc = c }
            cells[y * W + x] = if (bcn >= thr) bc else src[y * W + x]
        }
    }
}

object PdfExporter {
    fun export(ctx: android.content.Context, ch: ChartBuilder.Chart, cppIdx: Int): Uri {
        val cpp = when (cppIdx) { 0 -> 40; 1 -> 50; 2 -> 60; else -> 70 }
        val pageW = 595; val pageH = 842; val margin = 30f
        val doc = PdfDocument(); var pageNum = 0

        val legendTitle = Paint().apply { textSize = 14f; color = Color.BLACK; isFakeBoldText = true }
        val legendPaint = Paint().apply { textSize = 10f; color = Color.BLACK }
        val countPaint = Paint().apply { textSize = 10f; color = Color.GRAY; textAlign = Paint.Align.RIGHT }

        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, ++pageNum).create())
        var cv = page.canvas
        cv.drawText("Легенда схемы", margin, margin + 14f, legendTitle)
        cv.drawText("Размер: ${ch.W} × ${ch.H}, цветов: ${ch.palette.size}", margin, margin + 32f, legendPaint)
        var y = margin + 54f; val lh = 22f
        for (i in ch.palette.indices) {
            if (y + lh > pageH - margin) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, ++pageNum).create())
                cv = page.canvas; y = margin + 14f
            }
            val c = ch.palette[i]
            cv.drawRect(margin, y - 13f, margin + 20f, y + 6f, Paint().apply { color = Color.rgb(c.r,c.g,c.b) })
            val sp = Paint().apply {
                textSize = 11f; textAlign = Paint.Align.CENTER; isFakeBoldText = true
                color = if ((0.2126*c.r + 0.7152*c.g + 0.0722*c.b) > 140) Color.BLACK else Color.WHITE
            }
            cv.drawText(ch.symbols[i], margin + 10f, y, sp)
            cv.drawText("${c.code}  ${c.name}", margin + 28f, y, legendPaint)
            cv.drawText("${ch.counts[i]} шт.", pageW - margin, y, countPaint)
            y += lh
        }
        doc.finishPage(page)

        val cellStroke = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 0.4f; color = Color.argb(90,0,0,0) }
        val gridStroke = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(190,0,0,0) }
        val fillP = Paint()
        val textP = Paint().apply { textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.MONOSPACE }
        val headP = Paint().apply { textSize = 11f; color = Color.BLACK; isFakeBoldText = true }

        val cols = (ch.W + cpp - 1) / cpp; val rows = (ch.H + cpp - 1) / cpp
        for (r in 0 until rows) for (col in 0 until cols) {
            val xS = col * cpp; val yS = r * cpp
            val xE = minOf(xS + cpp, ch.W); val yE = minOf(yS + cpp, ch.H)
            val w = xE - xS; val h = yE - yS
            val cs = minOf((pageW - 2*margin) / w, (pageH - 2*margin - 34f) / h)
            textP.textSize = cs * 0.6f
            val pg = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, ++pageNum).create())
            val c2 = pg.canvas
            c2.drawText("Схема ${r+1}-${col+1} (строки ${yS+1}–$yE, колонки ${xS+1}–$xE)",
                margin, margin + 14f, headP)
            val ox = margin; val oy = margin + 30f
            for (yy in 0 until h) for (xx in 0 until w) {
                val idx = ch.cells[(yS + yy) * ch.W + (xS + xx)]
                val c = ch.palette[idx]
                fillP.color = Color.rgb(c.r, c.g, c.b)
                val px = ox + xx * cs; val py = oy + yy * cs
                c2.drawRect(px, py, px + cs, py + cs, fillP)
            }
            for (xx in 0..w) {
                val p = if ((xS + xx) % 10 == 0) gridStroke else cellStroke
                val px = ox + xx * cs
                c2.drawLine(px, oy, px, oy + h * cs, p)
            }
            for (yy in 0..h) {
                val p = if ((yS + yy) % 10 == 0) gridStroke else cellStroke
                val py = oy + yy * cs
                c2.drawLine(ox, py, ox + w * cs, py, p)
            }
            if (cs > 6f) for (yy in 0 until h) for (xx in 0 until w) {
                val idx = ch.cells[(yS + yy) * ch.W + (xS + xx)]
                val c = ch.palette[idx]
                val light = (0.2126*c.r + 0.7152*c.g + 0.0722*c.b) > 140
                textP.color = if (light) Color.BLACK else Color.WHITE
                val base = oy + (yy + 0.5f) * cs - (textP.ascent() + textP.descent()) / 2f
                c2.drawText(ch.symbols[idx], ox + (xx + 0.5f) * cs, base, textP)
            }
            doc.finishPage(pg)
        }

        val name = "chart_${System.currentTimeMillis()}.pdf"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Не удалось создать файл")
        ctx.contentResolver.openOutputStream(uri)!!.use { doc.writeTo(it) }
        doc.close()
        return uri
    }
}

object PngExporter {
    fun export(ctx: android.content.Context, ch: ChartBuilder.Chart): Uri {
        val bmp = ChartRenderer.render(ch, 20)
        val name = "chart_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Не удалось создать файл")
        ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return uri
    }
}