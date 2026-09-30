// RUN_PIPELINE_TILL: BACKEND

package foo.bar

@org.jetbrains.kotlin.compiler.plugin.template.DeepCopy
data class MyDataClass(val a : String, val b: String, val c: List<String>)