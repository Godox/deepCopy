package org.jetbrains.kotlin.compiler.plugin.template.fir

import org.jetbrains.kotlin.compiler.plugin.template.fir.Constants.DEEP_COPY_ANNOTATION
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirSupertypeGenerationExtension
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType

class DeepCopiableInterfaceGenerator(session: FirSession) : FirSupertypeGenerationExtension(session) {

    init {
        println("🔥 DeepCopiableInterfaceGenerator CREATED")
    }
    val PREDICATE = DeclarationPredicate.create {
        annotated(DEEP_COPY_ANNOTATION.asSingleFqName())
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    override fun needTransformSupertypes(declaration: FirClassLikeDeclaration): Boolean {
        val result = session.predicateBasedProvider.matches(PREDICATE, declaration)

        println("DeepCopy FIR: ${declaration.symbol} -> $result")

        return result
    }

    override fun computeAdditionalSupertypes(
        classLikeDeclaration: FirClassLikeDeclaration,
        resolvedSupertypes: List<FirResolvedTypeRef>,
        typeResolver: TypeResolveService
    ): List<ConeKotlinType> {
        val symbol = classLikeDeclaration.symbol as? FirClassSymbol<*> ?: return emptyList()

        val def = Constants.DEEP_COPY_INTERFACE.constructClassLikeType(
            arrayOf(symbol.defaultType()),
            false
        )

        return listOf(def)
    }
}