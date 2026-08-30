package com.chirag.arthix

import org.junit.Test
import org.vosk.android.StorageService

class CheckSignatureTest {
    @Test
    fun printSignature() {
        val methods = StorageService::class.java.methods
        val unpackMethods = methods.filter { it.name == "unpack" }
        unpackMethods.forEach { method ->
            println("UNPACK_METHOD_FOUND: ${method.name}")
            method.parameters.forEachIndexed { i, p ->
                println("  PARAM_$i: ${p.type.name}")
            }
        }
    }
}
