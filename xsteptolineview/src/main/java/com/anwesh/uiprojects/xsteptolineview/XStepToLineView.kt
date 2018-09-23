package com.anwesh.uiprojects.xsteptolineview

/**
 * Created by anweshmishra on 23/09/18.
 */

import android.app.Activity
import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Context
import android.graphics.Color

val nodes : Int = 5

fun Canvas.drawXSTLNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = w / (nodes + 1)
    val size : Float = gap / 3
    paint.strokeWidth = Math.min(w, h) / 60
    paint.strokeCap = Paint.Cap.ROUND
    paint.color = Color.parseColor("#ef5350")
    save()
    translate(w/2, gap + i * gap)
    for (j in 0..1) {
        val sf : Float = 1f - 2 * (j % 2)
        val sc : Float = Math.min(0.5f, Math.max(0f, scale - j * 0.5f)) * 2
        save()
        rotate(45f * sf * (1 - sc))
        drawLine(0f, -size, 0f, size, paint)
        restore()
    }
    restore()
}

class XStepToLineView(ctx : Context) : View(ctx) {

    private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer : Renderer = Renderer(this)

    override fun onDraw(canvas : Canvas) {
        renderer.render(canvas, paint)
    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                renderer.handleTap()
            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {
        fun update(cb : (Float) -> Unit) {
            scale += 0.05f * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(50)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class XSTLNode(var i : Int, val state : State = State()) {

        private var next : XSTLNode? = null
        private var prev : XSTLNode? = null

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawXSTLNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        init {
            addNeighbor()
        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = XSTLNode(i + 1)
                next?.prev = this
            }
        }

        fun update(cb : (Int, Float) -> Unit) {
            state.update {
                cb(i, it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : XSTLNode {
            var curr : XSTLNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class XStepToLine(var i : Int) {
        private var curr : XSTLNode = XSTLNode(0)
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            curr.draw(canvas, paint)
        }

        fun update(cb : (Int, Float) -> Unit) {
            curr.update {i, scl ->
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(i, scl)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : XStepToLineView) {
        private val xstl : XStepToLine = XStepToLine(0)
        private val animator : Animator = Animator(view)

        fun render(canvas : Canvas, paint : Paint) {
            canvas.drawColor(Color.parseColor("#BDBDBD"))
            xstl.draw(canvas, paint)
            animator.animate {
                xstl.update {i, scl ->
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            xstl.startUpdating {
                animator.start()
            }
        }
    }
    companion object {
        fun create(activity : Activity) : XStepToLineView {
            val view : XStepToLineView = XStepToLineView(activity)
            activity.setContentView(view)
            return view
        }
    }
}