package com.chirag.arthix

import org.junit.Test
import org.vosk.android.StorageService
import java.io.File

class CheckSignatureTest2 {
    @Test
    fun printSignature() {
        val methods = StorageService::class.java.methods
        val unpackMethods = methods.filter { it.name == "unpack" }
        val sb = StringBuilder()
        unpackMethods.forEach { method ->
            sb.append("UNPACK_METHOD_FOUND: ${method.name}\n")
            method.parameters.forEachIndexed { i, p ->
                sb.append("  PARAM_$i: ${p.type.name}\n")
            }
        }
        File("signature_output.txt").writeText(sb.toString())
    }
}
