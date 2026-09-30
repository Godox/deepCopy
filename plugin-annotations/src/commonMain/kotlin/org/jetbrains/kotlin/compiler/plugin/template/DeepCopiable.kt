package org.jetbrains.kotlin.compiler.plugin.template

public interface DeepCopiable<T> {
    public fun deepCopy(): T

    public companion object {
        @Suppress("UNCHECKED_CAST")
        public fun <T> deepCopyOrNull(value: T?) : T? {
            return if (value != null) (value as? DeepCopiable<T>)?.deepCopy() else null
        }
    }

}