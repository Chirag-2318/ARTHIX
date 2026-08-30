import org.vosk.android.StorageService

fun main() {
    val methods = StorageService::class.java.methods
    val unpackMethods = methods.filter { it.name == "unpack" }
    unpackMethods.forEach { method ->
        println("Method: ${method.name}")
        method.parameters.forEachIndexed { i, p ->
            println("  p$i: ${p.type.name}")
        }
    }
}
