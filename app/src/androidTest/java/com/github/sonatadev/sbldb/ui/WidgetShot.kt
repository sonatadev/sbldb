package com.github.sonatadev.sbldb.ui

import android.graphics.Bitmap
import android.view.View
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sonatadev.sbldb.widget.SbldbWidget
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Draws the widget's content off-screen and saves it next to the tour's pictures. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
class WidgetShot {
    @Test
    fun widget() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val widget = SbldbWidget()
        val views = runBlocking {
            val data = widget.load(context)
            GlanceRemoteViews().compose(context, DpSize(320.dp, 150.dp)) { widget.Content(context, data) }.remoteViews
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = views.apply(context, null)
            val w = (320 * context.resources.displayMetrics.density).toInt()
            val h = (150 * context.resources.displayMetrics.density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap).apply { drawColor(0xFF5A6B7D.toInt()) })
            val out = File(context.getExternalFilesDir(null), "screens").apply { mkdirs() }
            File(out, "20-widget.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
