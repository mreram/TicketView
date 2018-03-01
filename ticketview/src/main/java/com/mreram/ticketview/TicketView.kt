package com.mreram.ticketview


import android.content.Context
import android.graphics.*
import android.graphics.Paint.Style
import android.graphics.PorterDuff.Mode
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.RelativeLayout
import android.graphics.PorterDuffXfermode
import android.renderscript.Allocation
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.RenderScript
import android.graphics.Bitmap
import android.graphics.Color.BLACK
import android.graphics.Color.TRANSPARENT
import android.os.Build
import android.renderscript.Element
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF


/**
 * Created by Mohammad Reza Eram on 2/23/18.
 */

class TicketView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RelativeLayout(context, attrs, defStyleAttr) {

    companion object {
        private val DEFAULT_RADIUS = 50
        private val NO_VALUE = -1
    }

    private val eraser = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val dashPath = Path()

    private val circlesPath = Path()

    private var anchorViewId1: Int = 0
    private var anchorViewId2: Int = 0
    private var circlePosition1: Float = 0f
    private var circlePosition2: Float = 0f

    private var circleRadius: Float = 0f
    private var circleSpace: Float = 0f
    private var dashColor: Int = 0

    private var dashSize: Float = 0f

    init {


        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val a = context.obtainStyledAttributes(attrs, R.styleable.TicketView)
        try {
            circleRadius = a.getDimension(R.styleable.TicketView_tv_circleRadius, DEFAULT_RADIUS.toFloat())
            anchorViewId1 = a.getResourceId(R.styleable.TicketView_tv_anchor1, NO_VALUE)
            anchorViewId2 = a.getResourceId(R.styleable.TicketView_tv_anchor2, NO_VALUE)
            circleSpace = a.getDimension(R.styleable.TicketView_tv_circleSpace, 15f)
            dashColor = a.getColor(R.styleable.TicketView_tv_dashColor, Color.parseColor("#0085be"))
            dashSize = a.getDimension(R.styleable.TicketView_tv_dashSize, 3f)
        } finally {
            a.recycle()
        }

        eraser.xfermode = PorterDuffXfermode(Mode.CLEAR)

        dashPaint.color = dashColor
        dashPaint.style = Style.STROKE
        dashPaint.strokeWidth = dashSize
        dashPaint.pathEffect = DashPathEffect(floatArrayOf(getDp(3f).toFloat(), getDp(3f).toFloat()), 0f)
    }

    fun setRadius(radius: Float) {
        this.circleRadius = radius
        postInvalidate()
    }

    fun setAnchor(view1: View?, view2: View?) {

        val rect = Rect()
        view1?.getDrawingRect(rect)
        offsetDescendantRectToMyCoords(view1, rect)
        circlePosition1 = rect.bottom.toFloat()

        if (view2 != null) {
            view2.getDrawingRect(rect)
            offsetDescendantRectToMyCoords(view2, rect)
            circlePosition2 = rect.bottom.toFloat()
        }

        postInvalidate()
    }

    override fun drawChild(canvas: Canvas?, child: View?, drawingTime: Long): Boolean {
        val drawChild = super.drawChild(canvas, child, drawingTime)
        drawHoles(canvas!!)
        return drawChild
    }

    override fun dispatchDraw(canvas: Canvas?) {
        canvas?.save()
        super.dispatchDraw(canvas)
        canvas?.restore()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        if (anchorViewId1 != NO_VALUE || anchorViewId2 != NO_VALUE) {
            val anchorView1 = findViewById<View>(anchorViewId1)
            val anchorView2 = findViewById<View>(anchorViewId2)
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
                    setAnchor(anchorView1, anchorView2)
                }
            })
        }
    }

    override fun onDraw(canvas: Canvas) {
        drawHoles(canvas)
        super.onDraw(canvas)
    }

    private fun drawHoles(canvas: Canvas) {
        val w = width
        val radius = circleRadius
        val space = circleSpace
        val circleWidth = radius * 2

        var leftMargin = 0
        if (layoutParams is MarginLayoutParams) {
            val lp = layoutParams as MarginLayoutParams
            leftMargin = lp.leftMargin
        }

        val left = left - leftMargin
        val circleSpace = circleWidth + space
        val count = (w / circleSpace).toInt()
        val offset = w - circleSpace * count
        val sideOffset = offset / 2
        val halfCircleSpace = circleSpace / 2

        for (i in 0..count) {
            var positionCircle = i * circleSpace + sideOffset + left.toFloat() - radius
            if (i == 0) {
                positionCircle = left + sideOffset - radius
            }
            this.circlesPath.addCircle(positionCircle + halfCircleSpace, -circleRadius / 4, radius, Path.Direction.CW)
        }

        // add holes on the ticketView by erasing them
        with(circlesPath) {
            //anchor1
            addCircle(-circleRadius / 4, circlePosition1, circleRadius, Path.Direction.CW) // bottom left hole
            addCircle(w + circleRadius / 4, circlePosition1, circleRadius, Path.Direction.CW)// bottom right hole

            //anchor2
            when {
                anchorViewId2 != NO_VALUE -> {
                    addCircle(-circleRadius / 4, circlePosition2, circleRadius, Path.Direction.CW) // bottom left hole
                    addCircle(w + circleRadius / 4, circlePosition2, circleRadius, Path.Direction.CW) // bottom right hole
                }
            }
        }

        with(dashPath) {
            //anchor1
            moveTo(circleRadius, circlePosition1)
            quadTo(w - circleRadius, circlePosition1, w - circleRadius, circlePosition1)

            //anchor2
            when {
                anchorViewId2 != NO_VALUE -> {
                    moveTo(circleRadius, circlePosition2)
                    quadTo(w - circleRadius, circlePosition2, w - circleRadius, circlePosition2)
                }
            }
        }

        with(canvas) {
            if (dashSize > 0)
                drawPath(dashPath, dashPaint)
            drawPath(circlesPath, eraser)
        }
    }


    private fun getDp(value: Float): Int {
        return when (value) {
            0f -> 0
            else -> {
                val density = resources.displayMetrics.density
                Math.ceil((density * value).toDouble()).toInt()
            }
        }
    }


}
