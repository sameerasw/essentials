package com.sameerasw.essentials.island.plugins.media.particles

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.*

private val permutation = IntArray(512).also { p ->
    var state = 0x6d2b79f5
    for (i in 0 until 256) {
        state = 1664525*state+1013904223
        p[i] = state ushr 24
        p[i+256] = p[i]
    }
}

private fun fastFloor(x: Float): Int { val i = x.toInt(); return if (x < i) i-1 else i }

private fun fade(t: Float) = t*t*t*(t*(t*6f-15f)+10f)

private fun grad(hash: Int,px: Float,py: Float): Float {
    val h = hash and 7
    val u = if (h < 4) px else py
    val v = if (h < 4) py else px
    return (if (h and 1 != 0) -u else u)+(if (h and 2 != 0) -2f*v else 2f*v)
}

internal fun flowNoise(x: Float,y: Float): Float {
    val fx = fastFloor(x)
    val fy = fastFloor(y)
    val ix = fx and 255
    val iy = fy and 255
    val xf = x-fx
    val yf = y-fy
    val px0 = permutation[ix]
    val px1 = permutation[ix+1]
    val aa = permutation[px0+iy]
    val ab = permutation[px0+iy+1]
    val ba = permutation[px1+iy]
    val bb = permutation[px1+iy+1]
    val u = fade(xf)
    val v = fade(yf)
    val x1 = grad(aa,xf,yf).let { it+u*(grad(ba,xf-1f,yf)-it) }
    val x2 = grad(ab,xf,yf-1f).let { it+u*(grad(bb,xf-1f,yf-1f)-it) }
    return x1+v*(x2-x1)
}

private fun fbm(x: Float,y: Float): Float =
    flowNoise(x,y)*0.7f+flowNoise(x*2.02f+1.2f,y*2.02f+1.2f)*0.3f

@PublishedApi internal val curlX = FloatArray(2)

@PublishedApi internal fun curl(x: Float,y: Float,t: Float,frequency: Float,amount: Float) {
    val nx = x*frequency
    val ny = y*frequency
    val ty = ny+t*0.4f
    val n1 = fbm(nx,ty)
    val n2 = fbm(nx+13.5f,ny-t*0.3f)
    val dx = (fbm(nx+1f,ty)-fbm(nx-1f,ty))*0.5f
    val dy = (fbm(nx,ty+1f)-fbm(nx,ty-1f))*0.5f
    curlX[0] = (dy*2.2f+n1*0.6f)*amount
    curlX[1] = (-dx*2.2f+n2*0.6f)*amount
}

internal fun referenceParticleRadius(speed: Float,opacity: Float,minRadius: Float,maxRadius: Float,
                                     speedReference: Float = 20f): Float {
    val normSpeed = (speed/speedReference).coerceIn(0f,1f)
    return (maxRadius*(1f-normSpeed*0.25f)*(0.85f+0.15f*opacity)).coerceIn(minRadius,maxRadius)
}

internal inline fun visitParticles(tile: PoissonTile,geometry: CapsuleParticleGeometry,style: ParticleFieldPixels,
    origin: Offset,time: Float,draw: (Float,Float,Float,Float) -> Unit) {
    val maxOffset = style.displacement*8.4f
    val maxExtent = style.radius*(if (style.shape == ParticleShape.Diamond) 1.25f else 1f)+1f
    val margin = maxOffset+maxExtent
    val reach = style.outerBand+maxOffset+maxExtent
    val bounds = Rect(
        origin.x+max(-margin,geometry.rect.left-reach),origin.y+max(-margin,geometry.rect.top-reach),
        origin.x+min(geometry.viewport.width+margin,geometry.rect.right+reach),
        origin.y+min(geometry.viewport.height+margin,geometry.rect.bottom+reach))
    if (bounds.width <= 0f || bounds.height <= 0f) return
    val t = time*style.speed
    val frequency = 1f/style.noiseScale
    tile.visit(bounds) { bx,by ->
        val local = Offset(bx-origin.x,by-origin.y)
        val baseDistance = geometry.signedDistance(local)
        if (baseDistance < -style.innerBand || baseDistance > style.outerBand) return@visit
        curl(bx,by,t,frequency,style.displacement)
        val fx = curlX[0]
        val fy = curlX[1]
        val position = Offset(local.x+fx,local.y+fy)
        if (position.x < -maxExtent || position.y < -maxExtent ||
            position.x > geometry.viewport.width+maxExtent || position.y > geometry.viewport.height+maxExtent) return@visit
        val d = geometry.signedDistance(position)
        val baseOpacity = particleAlphaEnvelope(d,style.innerBand,style.outerBand)
        if (baseOpacity <= 0f) return@visit
        val opacity = if (style.falloffExponent == 4f) baseOpacity*baseOpacity*baseOpacity*baseOpacity else baseOpacity.pow(style.falloffExponent)
        if (opacity <= 0.015f) return@visit
        curl(bx,by,t+0.05f,frequency,style.displacement)
        val vx = curlX[0]-fx
        val vy = curlX[1]-fy
        val velocity = sqrt(vx*vx+vy*vy)/0.05f
        val radius = referenceParticleRadius(velocity,baseOpacity,style.minRadius,style.radius,20f*style.density)
        if (radius > 0.01f) draw(position.x,position.y,radius,opacity)
    }
}
