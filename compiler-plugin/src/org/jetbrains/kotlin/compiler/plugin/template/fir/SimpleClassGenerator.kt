package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.compiler.plugin.template.fir.Constants.DEEP_COPY_ANNOTATION
import org.jetbrains.kotlin.compiler.plugin.template.fir.Constants.DEEP_COPY_FUNCTION
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

/*
 * Generates top level class
 *
 * public final class foo.bar.MyClass {
 *     fun foo(): String = "Hello world"
 * }
 */
class SimpleClassGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        val owner = context?.owner ?: return emptyList()
        val function = createMemberFunction(owner, Key, DEEP_COPY_FUNCTION.callableName, returnType = owner.defaultType())
        function.status.transform {
            isOverride = true
        }
        return listOf(function.symbol)
    }


    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        return if (classSymbol.hasAnnotation(DEEP_COPY_ANNOTATION, session)) {
            setOf(DEEP_COPY_FUNCTION.callableName)
        } else {
            emptySet()
        }
    }

    object Key : GeneratedDeclarationKey()
}
