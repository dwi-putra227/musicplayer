package com.example.musicplayer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View

class CustomVisualizerView : View {
    private var colorIndex = 0
    private var bytes: ByteArray? = null
    private var visualizer: Visualizer? = null
    private val paint: Paint = Paint()
    private val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE)

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        paint.strokeWidth = 10f
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun releaseVisualizer() {
        visualizer?.release()
        visualizer = null
    }

    fun setPlayer(audioSessionId: Int) {
        visualizer = Visualizer(audioSessionId)
        visualizer?.apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) {
                        bytes = waveform
                        postInvalidate()
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {

                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            enabled = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bytes != null) {
            val width = width
            val height = height
            val scale = 111f
            val barWidth = width / (bytes!!.size.toFloat() * 0.06)
            val thicknessScale = 10


            val colors = arrayOf(
                Color.RED, Color.GREEN, Color.BLUE,
                Color.YELLOW, Color.CYAN, Color.MAGENTA
            )
            for (i in bytes!!.indices) {
                val barX = i * barWidth * 1.5
                val amplitude = (bytes!![i] + 128).toFloat() / scale
                val barHeight = amplitude * height / 2 * thicknessScale


                if (barHeight < height * 0.9) {
                    paint.color = colors[colorIndex]

                    canvas.drawRect(
                        barX.toFloat(), (height - barHeight).toFloat(),
                        (barX + barWidth).toFloat(), height.toFloat(), paint
                    )
                }
            }


            for (i in bytes!!.indices) {
                if (bytes!![i] < 128) {
                    bytes!![i] = (bytes!![i] + 1).toByte()
                }
            }
            postInvalidateDelayed(0)
            colorIndex = (colorIndex + 1) % colors.size
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        visualizer?.release()
    }
}
