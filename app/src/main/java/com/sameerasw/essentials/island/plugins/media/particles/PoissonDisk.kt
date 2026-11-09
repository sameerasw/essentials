package com.sameerasw.essentials.island.plugins.media.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.*
import java.util.Random

internal class PoissonTile(val width: Float, val height: Float, points: List<Offset>) {
    val coords = FloatArray(points.size*2).also { a -> points.forEachIndexed { i, p -> a[i*2] = p.x; a[i*2+1] = p.y } }
    inline fun visit(bounds: Rect, point: (Float, Float) -> Unit) {
        val x0 = floor(bounds.left/width).toInt()
        val x1 = floor(bounds.right/width).toInt()
        val y0 = floor(bounds.top/height).toInt()
        val y1 = floor(bounds.bottom/height).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            val ox = x*width
            val oy = y*height
            val c = coords
            var i = 0
            while (i < c.size) {
                val px = c[i]+ox
                val py = c[i+1]+oy
                i += 2
                if (px >= bounds.left && px <= bounds.right && py >= bounds.top && py <= bounds.bottom) point(px, py)
            }
        }
    }
}

internal fun generatePoissonTile(minDistance: Float, seed: Long = 0x49534C414E44,
                                 checkCancelled: () -> Unit = {}): PoissonTile {
    require(minDistance.isFinite() && minDistance > 0f)
    val width = minDistance*80f
    val height = minDistance*40f
    val cell = minDistance/sqrt(2f)
    val nx = ceil(width/cell).toInt()
    val ny = ceil(height/cell).toInt()
    val cw = width/nx
    val ch = height/ny
    val grid = IntArray(nx*ny) { -1 }
    val points = ArrayList<Offset>()
    val active = ArrayList<Int>()
    val random = Random(seed)
    fun wrap(i: Int, size: Int) = (i%size+size)%size
    fun add(p: Offset) {
        grid[(p.y/ch).toInt().coerceAtMost(ny-1)*nx+(p.x/cw).toInt().coerceAtMost(nx-1)] = points.size
        active.add(points.size)
        points.add(p)
    }
    fun valid(p: Offset): Boolean {
        val gx = (p.x/cw).toInt().coerceAtMost(nx-1)
        val gy = (p.y/ch).toInt().coerceAtMost(ny-1)
        for (y in -2..2) for (x in -2..2) {
            val index = grid[wrap(gy+y,ny)*nx+wrap(gx+x,nx)]
            if (index < 0) continue
            val other = points[index]
            val dx = abs(other.x-p.x).let { min(it,width-it) }
            val dy = abs(other.y-p.y).let { min(it,height-it) }
            if (dx*dx+dy*dy < minDistance*minDistance) return false
        }
        return true
    }
    add(Offset(random.nextFloat()*width, random.nextFloat()*height))
    var iteration = 0
    while (active.isNotEmpty()) {
        if (iteration++ and 63 == 0) checkCancelled()
        val slot = random.nextInt(active.size)
        val origin = points[active[slot]]
        var accepted = false
        for (attempt in 0 until 24) {
            val angle = random.nextDouble()*2.0*PI
            val radius = minDistance*sqrt(1f+3f*random.nextFloat())
            val px = ((origin.x+cos(angle).toFloat()*radius)%width+width)%width
            val py = ((origin.y+sin(angle).toFloat()*radius)%height+height)%height
            val p = Offset(px,py)
            if (valid(p)) { add(p); accepted = true; break }
        }
        if (!accepted) {
            active[slot] = active.last()
            active.removeAt(active.lastIndex)
        }
    }
    return PoissonTile(width,height,points)
}
