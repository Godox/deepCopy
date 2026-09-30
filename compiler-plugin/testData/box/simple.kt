@org.jetbrains.kotlin.compiler.plugin.template.DeepCopy
data class MyDataClass(
    val string: String,
    val primitive: Int,
    val stringList: List<String>,
    val primitiveList: List<Int>,
    val deepCopiableClass: MyNestedDataClass,
    val deepCopiableClassList: List<MyNestedDataClass>
)

@org.jetbrains.kotlin.compiler.plugin.template.DeepCopy
data class MyNestedDataClass(val value: String)

fun box(): String {
    val original = MyDataClass(
        "a",
        1,
        listOf("c", "d"),
        listOf(1, 2),
        MyNestedDataClass("nestedA"),
        listOf(MyNestedDataClass("nestedA"), MyNestedDataClass("nestedB"))
    )
    val deepCopied: MyDataClass = original.deepCopy()
    val equality = deepCopied !== original && deepCopied == original
    val nestedClassesEquality = original.deepCopiableClassList.zip(deepCopied.deepCopiableClassList).all { (original, deepCopied) ->
        deepCopied !== original && deepCopied == original
    }
    return if (equality && nestedClassesEquality) "OK" else "Fail: $deepCopied"
}