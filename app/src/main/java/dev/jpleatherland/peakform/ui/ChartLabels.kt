package dev.jpleatherland.peakform.ui

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import dev.jpleatherland.peakform.R

class ChartLabels(
    context: Context,
    layoutResource: Int,
    private val labelRes: Int,
) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvContent)

    override fun refreshContent(
        e: Entry?,
        highlight: Highlight?,
    ) {
        if (e != null) {
            var date = (e.data as? String) ?: ""
            date = date.split("|").getOrNull(1) ?: ""
            if (labelRes == R.string.marker_calories_date) {
                val kcals = e.y.toInt()
                tvContent.text =
                    context.getString(
                        labelRes,
                        kcals,
                        date,
                    )
            } else {
                val weight = e.y
                tvContent.text =
                    context.getString(
                        R.string.marker_weight_date,
                        weight,
                        date,
                    )
            }
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Centers the MarkerView above the point
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }
}
