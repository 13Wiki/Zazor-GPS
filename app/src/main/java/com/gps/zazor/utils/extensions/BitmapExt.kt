package com.gps.zazor.utils.extensions

import android.graphics.*
import java.io.ByteArrayOutputStream

fun Bitmap.toBytes(quality: Int = 90): ByteArray =
      ByteArrayOutputStream().use {
          compress(Bitmap.CompressFormat.JPEG, quality, it)
          it.toByteArray()
      }

fun Bitmap.drawText(text: String): Bitmap =
      copy(config, true).let { bmp ->
          Canvas(bmp).let { canvas ->
              Paint(Paint.ANTI_ALIAS_FLAG).apply {
                  color = Color.RED
                  textSize = 387F
                  val bounds = Rect()
                  getTextBounds(text, 0, text.length, bounds);
                  val x = (bmp.width - bounds.width()) / 2F
                  val y = (bmp.height + bounds.height()) / 2F

                  canvas.drawText(text, x, y, this)
              }
          }
          bmp
      }

/*
fun Bitmap.overlayNotes(notes: String): Bitmap =
    copy(config, true).let { bmp ->
            Canvas(bmp).let { canvas ->
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 75F
                    val bounds = Rect()
                    getTextBounds(notes, 0, notes.length, bounds)
                    val x = (bmp.width - bounds.width()) / 2F
                    val y = (bmp.height + bounds.height()) / 2F
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                    }
                    canvas.save()
                    canvas.rotate(270F, (bmp.width / 2).toFloat(), (bmp.height / 2).toFloat())
                    canvas.drawRect(x, y, bounds.width().toFloat(), bounds.height().toFloat(), paint)
                    canvas.drawText(notes, x, y, this)
                    canvas.restore()
                }
            }
            bmp
    }*/
