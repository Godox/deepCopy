package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.compiler.plugin.template.DeepCopiable
import org.jetbrains.kotlin.compiler.plugin.template.DeepCopy
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

internal val KClass<*>.classId: ClassId
    get() = ClassId.topLevel(FqName(qualifiedName!!))

object Constants {
    val DEEP_COPY_ANNOTATION: ClassId = DeepCopy::class.classId
    val DEEP_COPY_INTERFACE: ClassId = DeepCopiable::class.classId
    val DEEP_COPY_FUNCTION: CallableId = CallableId(DEEP_COPY_INTERFACE, Name.identifier("deepCopy"))
}