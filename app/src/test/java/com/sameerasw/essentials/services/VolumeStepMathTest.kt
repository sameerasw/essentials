package com.sameerasw.essentials.services

import com.sameerasw.essentials.services.handlers.VolumeStepMath
import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeStepMathTest {
    @Test fun raisesBySteps() = assertEquals(9, VolumeStepMath.target(current = 7, min = 0, max = 15, steps = 2, raise = true))

    @Test fun lowersBySteps() = assertEquals(4, VolumeStepMath.target(current = 7, min = 0, max = 15, steps = 3, raise = false))

    @Test fun stopsAtMax() = assertEquals(15, VolumeStepMath.target(current = 14, min = 0, max = 15, steps = 5, raise = true))

    @Test fun stopsAtMin() = assertEquals(0, VolumeStepMath.target(current = 1, min = 0, max = 15, steps = 5, raise = false))

    @Test fun respectsNonZeroMin() = assertEquals(1, VolumeStepMath.target(current = 2, min = 1, max = 15, steps = 4, raise = false))

    @Test fun staysPutAtLimit() = assertEquals(15, VolumeStepMath.target(current = 15, min = 0, max = 15, steps = 2, raise = true))

    @Test fun smallStepsKeepDefaultRepeat() {
        assertEquals(150L, VolumeStepMath.repeatIntervalMs(1))
        assertEquals(200L, VolumeStepMath.repeatIntervalMs(2))
    }

    @Test fun largerStepsRepeatLessOften() {
        assertEquals(300L, VolumeStepMath.repeatIntervalMs(3))
        assertEquals(500L, VolumeStepMath.repeatIntervalMs(5))
    }
}
