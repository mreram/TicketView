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


/**
 * Created by Mohammad Reza Eram on 2/23/18.
 */

class TicketView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : RelativeLayout(context, attrs, defStyleAttr) {
    private  var mContext:Context = context;
    companion object {
        private val DEFAULT_RADIUS: Float = 9f
    }

    private val eraser = Paint(Paint.ANTI_ALIAS_FLAG)
    private var circlesPath = Path()
    private var circleRadius: Float = 0f
    private var circleSpace: Float = 0f
    private var enableSemiCircleOnTop:Boolean;
    private var anchorIdsString:String?= null
    private var circlePositions = mutableListOf<Float>()
    private var dashColor: Int = 0
    private var dashSize: Float = 0f
    private val dashPath = Path()
    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val a = context.obtainStyledAttributes(attrs, R.styleable.TicketView)
        try {
            enableSemiCircleOnTop = a.getBoolean(R.styleable.TicketView_tv_enableSemiCircleOnTop,true)
            circleRadius = a.getDimension(R.styleable.TicketView_tv_circleRadius, getDp(DEFAULT_RADIUS).toFloat())
            anchorIdsString = a.getString(R.styleable.TicketView_tv_anchors)
            circleSpace = a.getDimension(R.styleable.TicketView_tv_circleSpace, getDp(15f).toFloat())
            dashColor = a.getColor(R.styleable.TicketView_tv_dashColor, Color.parseColor("#0085be"))
            dashSize = a.getDimension(R.styleable.TicketView_tv_dashSize, getDp(1.5f).toFloat())
        } finally {
            a.recycle()
        }

        eraser.xfermode = PorterDuffXfermode(Mode.CLEAR)

        dashPaint.color = dashColor
        dashPaint.style = Style.STROKE
        dashPaint.strokeWidth = dashSize
        dashPaint.pathEffect = DashPathEffect(floatArrayOf(getDp(3f).toFloat(), getDp(3f).toFloat()), 0f)
    }
     // parse views matching the names provided in the user applications under app:tv_anchors
    private fun parseViews() {
             val  rect = Rect()
             anchorIdsString!!.split(",").iterator().forEach {
                 val view = this.findViewById<View>(getResources().getIdentifier(it, "id", this.mContext.packageName))
                 addCirclePosition(view, rect)

             }
     }


    fun setRadius(radius: Float) {
        this.circleRadius = radius
        postInvalidate()
    }

    fun setAnchor() {
        parseViews()
        postInvalidate()
    }

    // adds circlePosition to a  list.
    private fun addCirclePosition(view: View?, rect: Rect) {
        if (view == null) {
            return
        }
            view.getDrawingRect(rect)
            offsetDescendantRectToMyCoords(view, rect)
            circlePositions.add(rect.bottom.toFloat());


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
            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    } else {
                        viewTreeObserver.removeGlobalOnLayoutListener(this)
                    }
                    setAnchor()
                }
            })

    }

    override fun onDraw(canvas: Canvas) {
        drawHoles(canvas)
        super.onDraw(canvas)
    }

    private fun drawHoles(canvas: Canvas) {
        this.circlesPath = Path()
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


        if (enableSemiCircleOnTop){
            for (i in 0 until count) {
                var positionCircle = i * circleSpace + sideOffset + left.toFloat() - radius
                if (i == 0) {
                    positionCircle = left + sideOffset - radius
                }
                this.circlesPath.addCircle(positionCircle + halfCircleSpace, -circleRadius / 4, radius, Path.Direction.CW)
            }

        }
        createCircleAndDashPath(w)

        with(canvas) {
            if (dashSize > 0)
                drawPath(dashPath, dashPaint)
            drawPath(circlesPath, eraser)
        }
    }

    private fun createCircleAndDashPath(width: Int) {

        circlePositions.iterator().forEach {
            with(circlesPath) {
                // add path for the holes  on the ticketView by erasing them at an anchor
                addCircle(-circleRadius / 4, it, circleRadius, Path.Direction.CW) // bottom left hole
                addCircle(width + circleRadius / 4, it, circleRadius, Path.Direction.CW)// bottom right hole
            }

            with(dashPath) {
                //adds path for the  dash  connecting the holes on left and right of an anchor .
                moveTo(circleRadius, it)
                quadTo(width - circleRadius, it, width - circleRadius, it)

            }

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
