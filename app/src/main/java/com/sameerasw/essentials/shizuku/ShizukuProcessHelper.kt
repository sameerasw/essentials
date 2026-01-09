package com.sameerasw.essentials.shizuku

import rikka.shizuku.Shizuku
import java.lang.reflect.Method

object ShizukuProcessHelper {
    private var newProcessMethod: Method? = null

    init {
        try {
            newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod?.isAccessible = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun newProcess(cmd: Array<String>, env: Array<String>? = null, dir: String? = null): Process? {
        return try {
            if (newProcessMethod != null) {
                newProcessMethod?.invoke(null, cmd, env, dir) as? Process
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
