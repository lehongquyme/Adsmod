package com.leking.ads.util
import android.os.Handler
import android.os.Looper
class CountdownManager(private var duration: Long, private val listener: CountdownListener?) {
    interface CountdownListener { fun onTick(millisLeft: Long); fun onFinished() }
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private val interval = 1000L
    private var timeLeft = duration
    fun start() { cancel(); runnable = object : Runnable { override fun run() { timeLeft -= interval; listener?.onTick(timeLeft); if (timeLeft <= 0) listener?.onFinished() else handler.postDelayed(this, interval) } }; handler.postDelayed(runnable!!, interval) }
    fun cancel() { runnable?.let(handler::removeCallbacks); runnable = null }
    fun reset(newMillis: Long) { cancel(); duration = newMillis; timeLeft = newMillis }
    fun isRunning() = runnable != null
}
