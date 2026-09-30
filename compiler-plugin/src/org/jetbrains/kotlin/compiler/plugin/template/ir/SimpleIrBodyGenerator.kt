package org.jetbrains.kotlin.compiler.plugin.template.ir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.compiler.plugin.template.DeepCopiable
import org.jetbrains.kotlin.compiler.plugin.template.fir.Constants.DEEP_COPY_FUNCTION
import org.jetbrains.kotlin.compiler.plugin.template.fir.SimpleClassGenerator
import org.jetbrains.kotlin.compiler.plugin.template.fir.classId
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.collections.forEach

class SimpleIrBodyGenerator(val pluginContext: IrPluginContext) : AbstractTransformerForGenerator(pluginContext) {

    val deepCopiableInterface = pluginContext.finderForBuiltins().findClass(DeepCopiable::class.classId)
        ?: error("could not find deep copiable interface")
    val deepCopyFunction = deepCopiableInterface.functionByName("deepCopy")

    val mapFunctions = pluginContext.finderForBuiltins().findFunctions(
        CallableId(
            packageName = FqName("kotlin.collections"),
            callableName = Name.identifier("map")
        )
    )

    private fun getIterableCloneFunction(string: String): IrSimpleFunctionSymbol =
        pluginContext.finderForBuiltins().findFunctions(
            CallableId(
                packageName = FqName("kotlin.collections"),
                callableName = Name.identifier(string)
            )
        ).first { it.getReceiverClass()?.symbol == irBuiltIns.iterableClass }

    override fun interestedIn(key: GeneratedDeclarationKey?): Boolean {
        return key == SimpleClassGenerator.Key
    }

    override fun generateBodyForFunction(
        function: IrSimpleFunction,
        key: GeneratedDeclarationKey?
    ): IrBody {
        require(function.name == DEEP_COPY_FUNCTION.callableName)

        val parentClass = function.parentAsClass

        val constructor = parentClass.primaryConstructor!!
        val properties =
            constructor.parameters.associateWith { parentClass.getPropertyGetter(it.name.asString())?.owner }


        return DeclarationIrBuilder(context, function.symbol).irBlockBody {
            +irReturn(
                irCallConstructor(constructor.symbol, emptyList()).apply {
                    properties.forEach { (param, getter) ->
                        val getterCall = irCall(getter!!).apply {
                            arguments[0] = irGet(function.dispatchReceiverParameter!!)
                        }
                        arguments[param] = argumentFor(getterCall, function)
                    }
                }
            )
        }.also { function.body = it }
    }

    private fun IrBlockBodyBuilder.argumentFor(
        getterCall: IrFunctionAccessExpression,
        parentFunction: IrSimpleFunction
    ): IrExpression? {
        val paramType = getterCall.type as? IrSimpleType ?: return null

        return when {
            paramType.isPrimitiveType() || paramType.isString() -> getterCall
            paramType.isIterable() -> iterableMapExpression(
                receiverExpression = getterCall,
                paramType = paramType,
                parentFunction = parentFunction
            )

            paramType.isSubtypeOfClass(pluginContext.irBuiltIns.mapClass) -> {
                TODO()
            }

            paramType.isSubtypeOfClass(deepCopiableInterface) -> {
                irCall(deepCopyFunction).apply {
                    arguments[0] = getterCall
                }
            }

            else -> error("Unsupported deepCopy type")
        }
    }

    private fun IrType.isIterable(): Boolean = context.irBuiltIns.run {
        isSubtypeOfClass(arrayClass) || isSubtypeOfClass(this.iterableClass)
    }

    private fun IrBuilderWithScope.iterableMapExpression(
        receiverExpression: IrExpression,
        paramType: IrSimpleType,
        parentFunction: IrSimpleFunction,
    ): IrExpression {
        val elementType = paramType.arguments.first().typeOrFail

        return if (elementType.isPrimitiveType() || elementType.isString()) {
            cloneCollection(receiverExpression)
        } else {
            mapAllElements(paramType, elementType, receiverExpression, parentFunction)
        }
    }

    private fun IrBuilderWithScope.mapAllElements(
        paramType: IrSimpleType,
        elementType: IrType,
        receiverExpression: IrExpression,
        parentFun: IrSimpleFunction
    ): IrCall {
        val mapFunction = mapFunctions.findMatchingMapFunctionFor(paramType) ?: error("map function is not found")
        return irCall(mapFunction).apply {
            val (receiver, transform) = mapFunction.owner.parameters
            typeArguments[0] = elementType
            typeArguments[1] = elementType
            arguments[receiver] = receiverExpression
            arguments[transform] = mapFunction(elementType, parentFun)
        }
    }

    private fun IrBuilderWithScope.cloneCollection(receiverExpression: IrExpression): IrCall {
        val cloneFun = when (val receiverClass = receiverExpression.type.classOrFail) {
            irBuiltIns.listClass -> getIterableCloneFunction("toList")
            irBuiltIns.setClass -> getIterableCloneFunction("toSet")
            irBuiltIns.mutableListClass -> getIterableCloneFunction("toMutableList")
            irBuiltIns.mutableSetClass -> getIterableCloneFunction("toMutableSet")
            else -> error("Cannot fun cloning function for ${receiverClass.owner.name.asString()}")
        }

        return irCall(cloneFun).apply {
            arguments[0] = receiverExpression
        }
    }

    private fun Collection<IrSimpleFunctionSymbol>.findMatchingMapFunctionFor(
        paramType: IrSimpleType
    ): IrSimpleFunctionSymbol? {
        return paramType.getClass()?.let { paramClass ->
            find { mapFunction ->
                mapFunction.getReceiverClass()?.let { receiverClass ->
                    paramClass.isSubclassOf(receiverClass)
                } ?: false
            }
        }
    }

    private fun IrBuilderWithScope.mapFunction(type: IrType, parentFun: IrSimpleFunction): IrFunctionExpression {
        val lambda = irFactory.buildFun {
            name = Name.special("<anonymous>")
            returnType = type
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            parent = parentFun

            val itParameter = irFactory.createValueParameter(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                name = Name.identifier("params"),
                type = type,
                isAssignable = false,
                symbol = IrValueParameterSymbolImpl(),
                kind = IrParameterKind.Regular,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false,
                isHidden = false
            )
            itParameter.parent = this
            parameters = listOf(itParameter)

            val lambdaBuilder = DeclarationIrBuilder(context, symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            body = lambdaBuilder.irBlockBody(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
            ) {
                +irReturn(
                    irCall(deepCopyFunction).apply {
                        dispatchReceiver = irGet(itParameter)
                    }
                )
            }
        }

        val functionType = context.irBuiltIns.functionN(1)
            .typeWith(type, type)
        return IrFunctionExpressionImpl(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            type = functionType,
            origin = IrStatementOrigin.LAMBDA,
            function = lambda
        )
    }

    private fun IrSimpleFunctionSymbol.getReceiverClass(): IrClass? =
        owner.parameters.find { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classifierOrNull?.owner as? IrClass

}
