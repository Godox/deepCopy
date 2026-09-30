import org.jetbrains.kotlin.compiler.plugin.template.DeepCopiable
import org.jetbrains.kotlin.compiler.plugin.template.DeepCopy

@DeepCopy
data class TestDeepCopiable(
    val address: String,
    val nested: List<Nested>,
)


@DeepCopy
data class Nested(val value: String)


fun testDeepCopiable(): Pair<TestDeepCopiable, TestDeepCopiable> {
    val testDeepCopiable = TestDeepCopiable("Test Deep Copiable", listOf(Nested("")))

    return testDeepCopiable to DeepCopiable.deepCopyOrNull(testDeepCopiable)!!
}


fun main() {
    val (first, second) = testDeepCopiable()
    println(first !== second)
}