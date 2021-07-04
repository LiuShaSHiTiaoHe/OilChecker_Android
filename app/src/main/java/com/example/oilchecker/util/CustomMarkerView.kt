package com.example.oilchecker.util

import android.content.Context
import android.graphics.Canvas
import android.view.LayoutInflater
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.example.oilchecker.databinding.CustomMakerViewLayoutBinding

class CustomMarkerView(ctx: Context?, resource: Int) : MarkerView(ctx, resource) {

    private var binding: CustomMakerViewLayoutBinding = CustomMakerViewLayoutBinding.inflate(
        LayoutInflater.from(context),this,true)

    override fun refreshContent(entry: Entry, highlight: Highlight) {

        binding.tvContent.text = entry.y.toString()
        super.refreshContent(entry, highlight)
    }

    private var customOffset: MPPointF? = null

    override fun getOffset(): MPPointF {

        if (customOffset == null) {
            customOffset = MPPointF(-(width / 2).toFloat(), -height.toFloat())
        }

        return customOffset as MPPointF
    }
    private val screenWidthInPx = resources.displayMetrics.widthPixels
    override fun draw(canvas: Canvas, posX: Float, posY: Float) {

        var newPosX = posX

        val width = width
        if (screenWidthInPx - newPosX - width < width) {
            newPosX -= width.toFloat()
        }

        canvas.translate(newPosX, posY)
        draw(canvas)
        canvas.translate(-newPosX, -posY)
    }
}