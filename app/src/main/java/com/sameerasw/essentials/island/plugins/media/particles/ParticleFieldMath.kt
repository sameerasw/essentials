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

internal fun flowNoise(x: Double,y: Double): Double {
    val ix = floor(x).toInt() and 255
    val iy = floor(y).toInt() and 255
    val xf = x-floor(x)
    val yf = y-floor(y)
    fun fade(t: Double) = t*t*t*(t*(t*6-15)+10)
    fun grad(hash: Int,px: Double,py: Double): Double {
        val h = hash and 7
        val u = if (h < 4) px else py
        val v = if (h < 4) py else px
        return (if (h and 1 != 0) -u else u)+(if (h and 2 != 0) -2*v else 2*v)
    }
    fun lerp(t: Double,a: Double,b: Double) = a+t*(b-a)
    val aa = permutation[permutation[ix]+iy]
    val ab = permutation[permutation[ix]+iy+1]
    val ba = permutation[permutation[ix+1]+iy]
    val bb = permutation[permutation[ix+1]+iy+1]
    val u = fade(xf)
    val v = fade(yf)
    return lerp(v,lerp(u,grad(aa,xf,yf),grad(ba,xf-1,yf)),
        lerp(u,grad(ab,xf,yf-1),grad(bb,xf-1,yf-1)))
}

private fun fbm(x: Double,y: Double): Double =
    flowNoise(x,y)*0.7+flowNoise(x*2.02+1.2,y*2.02+1.2)*0.3

internal fun referenceCurlOffset(x: Float,y: Float,t: Float,frequency: Float,amount: Float): Offset {
    val nx = x.toDouble()*frequency
    val ny = y.toDouble()*frequency
    val time = t.toDouble()
    val n1 = fbm(nx,ny+time*0.4)
    val n2 = fbm(nx+13.5,ny-time*0.3)
    val dx = (fbm(nx+1,ny+time*0.4)-fbm(nx-1,ny+time*0.4))*0.5
    val dy = (fbm(nx,ny+1+time*0.4)-fbm(nx,ny-1+time*0.4))*0.5
    return Offset(((dy*2.2+n1*0.6)*amount).toFloat(),((-dx*2.2+n2*0.6)*amount).toFloat())
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
    val bounds = Rect(origin.x-margin,origin.y-margin,
        origin.x+geometry.viewport.width+margin,origin.y+geometry.viewport.height+margin)
    val t = time*style.speed
    val frequency = 1f/style.noiseScale
    tile.visit(bounds) { bx,by ->
        val local = Offset(bx,by)-origin
        val baseDistance = geometry.signedDistance(local)
        if (baseDistance < -style.innerBand || baseDistance > style.outerBand) return@visit
        val flow = referenceCurlOffset(bx,by,t,frequency,style.displacement)
        val position = local+flow
        if (position.x < -maxExtent || position.y < -maxExtent ||
            position.x > geometry.viewport.width+maxExtent || position.y > geometry.viewport.height+maxExtent) return@visit
        val d = geometry.signedDistance(position)
        val baseOpacity = particleAlphaEnvelope(d,style.innerBand,style.outerBand)
        if (baseOpacity <= 0f) return@visit
        val opacity = baseOpacity.pow(style.falloffExponent)
        if (opacity <= 0.015f) return@visit
        val next = referenceCurlOffset(bx,by,t+0.05f,frequency,style.displacement)
        val velocity = (next-flow).getDistance()/0.05f
        val radius = referenceParticleRadius(velocity,baseOpacity,style.minRadius,style.radius,20f*style.density)
        if (radius > 0.01f) draw(position.x,position.y,radius,opacity)
    }
}
