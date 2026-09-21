package com.gps.zazor.utils.export

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.gps.zazor.R
import com.gps.zazor.data.models.CoordinateFormat
import com.gps.zazor.data.models.Photo
import com.gps.zazor.utils.time.PhotoClock
import java.io.File
import java.io.IOException

/**
 * The set of photos as a document someone can print, sign or attach to a case file.
 *
 * A PDF rather than the plain-text sheet: a report that has to be handed to somebody official is
 * expected to look like a page, with the pictures on it rather than beside it. Both survive; the
 * text one pastes into a chat, this one goes into a folder.
 *
 * Built with the platform's own PdfDocument - no library, so nothing here can start sending the
 * pictures anywhere. Every value printed is one already burned onto the picture above it.
 */
class PdfReportWriter(private val context: Context) {

    /**
     * @param includeCoordinates print the position under each frame.
     * @param includeAddress print the address; it is a position in words, so it follows the same
     *        decision a person makes about coordinates but can be dropped on its own.
     * @param includeNotes print what the person wrote about each frame.
     * @return the written file, or null when nothing could be written.
     */
    fun write(
        photos: List<Photo>,
        name: String,
        coordinateFormat: CoordinateFormat,
        includeCoordinates: Boolean,
        includeAddress: Boolean,
        includeNotes: Boolean
    ): File? {
        if (photos.isEmpty()) return null
        val document = PdfDocument()
        val title = Paint().apply {
            color = Color.BLACK
            textSize = TITLE_SIZE
            isFakeBoldText = true
            isAntiAlias = true
        }
        val body = Paint().apply {
            color = Color.DKGRAY
            textSize = BODY_SIZE
            isAntiAlias = true
        }
        try {
            var page = document.startPage(pageInfo(document))
            var canvas = page.canvas
            var y = MARGIN + TITLE_SIZE
            canvas.drawText(context.getString(R.string.report_title), MARGIN, y, title)
            y += LINE
            canvas.drawText(context.getString(R.string.report_count, photos.size), MARGIN, y, body)
            y += LINE * 2

            photos.sortedBy { it.date }.forEachIndexed { index, photo ->
                val lines = describe(photo, coordinateFormat, includeCoordinates, includeAddress, includeNotes)
                val blockHeight = IMAGE_HEIGHT + LINE * (lines.size + 1)
                // A frame and its description stay on one page: a caption on the page after its
                // picture is worse than a shorter page.
                if (y + blockHeight > PAGE_HEIGHT - MARGIN) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo(document))
                    canvas = page.canvas
                    y = MARGIN
                }
                canvas.drawText("${index + 1}. ${PhotoClock.formatDateTime(photo.date)}", MARGIN, y + BODY_SIZE, title)
                y += LINE
                drawPhoto(canvas, photo, y)
                var textY = y + BODY_SIZE
                lines.forEach { line ->
                    canvas.drawText(line, MARGIN + IMAGE_WIDTH + GAP, textY, body)
                    textY += LINE
                }
                y += blockHeight
            }
            drawFooter(canvas, body)
            document.finishPage(page)

            val file = File(reportsDir(), "${name.replace(FILE_UNSAFE, "_")}.pdf")
            file.outputStream().use(document::writeTo)
            return file
        } catch (e: IOException) {
            return null
        } finally {
            document.close()
        }
    }

    private fun pageInfo(document: PdfDocument) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), document.pages.size + 1)
            .create()

    /** Scaled down while decoding: a twelve-megapixel frame is not printable at that size anyway. */
    private fun drawPhoto(canvas: Canvas, photo: Photo, top: Float) {
        val options = BitmapFactory.Options().apply { inSampleSize = SAMPLE_SIZE }
        val bitmap = BitmapFactory.decodeFile(photo.path, options) ?: return
        val scale = minOf(IMAGE_WIDTH / bitmap.width, IMAGE_HEIGHT / bitmap.height)
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        val target = Rect(
            MARGIN.toInt(), top.toInt(), (MARGIN + width).toInt(), (top + height).toInt()
        )
        canvas.drawBitmap(bitmap, null, target, null)
        bitmap.recycle()
    }

    private fun drawFooter(canvas: Canvas, body: Paint) {
        canvas.drawText(
            context.getString(R.string.report_footer), MARGIN, PAGE_HEIGHT - MARGIN, body
        )
    }

    private fun describe(
        photo: Photo,
        format: CoordinateFormat,
        includeCoordinates: Boolean,
        includeAddress: Boolean,
        includeNotes: Boolean
    ): List<String> = buildList {
        if (includeCoordinates && photo.lat != null && photo.lng != null) {
            add("${format.format(photo.lat)}, ${format.format(photo.lng)}")
            photo.accuracyMeters?.let {
                add(context.getString(R.string.report_accuracy, it.toInt()))
            }
        }
        if (includeAddress) {
            photo.address?.takeIf { it.isNotBlank() }?.let(::add)
        }
        if (includeNotes) {
            photo.name.takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    /**
     * Written inside the app's own space, and swept on every run: a report is made to be handed
     * over at once, and last week's copies are somebody's evidence sitting on a phone.
     */
    private fun reportsDir(): File =
        File(context.cacheDir, REPORTS_DIR).apply {
            mkdirs()
            listFiles()?.forEach { it.delete() }
        }

    private companion object {

        const val PAGE_WIDTH = 595F
        const val PAGE_HEIGHT = 842F
        const val MARGIN = 40F
        const val GAP = 16F
        const val IMAGE_WIDTH = 200F
        const val IMAGE_HEIGHT = 150F
        const val TITLE_SIZE = 14F
        const val BODY_SIZE = 11F
        const val LINE = 16F
        const val SAMPLE_SIZE = 4
        const val REPORTS_DIR = "reports"
        val FILE_UNSAFE = Regex("[^\\p{L}\\p{N} .-]")
    }
}
