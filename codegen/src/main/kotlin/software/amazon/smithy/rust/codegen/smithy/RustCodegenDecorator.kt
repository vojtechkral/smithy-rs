/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.rust.codegen.smithy

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.rust.codegen.smithy.generators.ProtocolConfig
import software.amazon.smithy.rust.codegen.smithy.generators.config.ConfigCustomization
import software.amazon.smithy.rust.codegen.smithy.protocols.ProtocolMap
import java.util.ServiceLoader
import java.util.logging.Logger

/**
 * [RustCodegenDecorator] allows downstream users to customize code generation.
 *
 * For example, AWS-specific code generation generates customizations required to support
 * AWS services. An different downstream customer way wish to add a different set of derive
 * attributes to the generated classes.
 */
interface RustCodegenDecorator {
    /**
     * The name of this [RustCodegenDecorator], used for logging and debug information
     */
    abstract val name: String

    /**
     * Enable a deterministic ordering to be applied, with the lowest numbered integrations being applied first
     */
    abstract val order: Byte
    fun configCustomizations(
        protocolConfig: ProtocolConfig,
        baseCustomizations: List<ConfigCustomization>
    ): List<ConfigCustomization> = baseCustomizations

    fun protocols(serviceId: ShapeId, currentProtocols: ProtocolMap): ProtocolMap = currentProtocols

    fun symbolProvider(baseProvider: RustSymbolProvider): RustSymbolProvider = baseProvider
}

class CombinedCodegenDecorator(decorators: List<RustCodegenDecorator>) : RustCodegenDecorator {
    val orderedDecorators = decorators.sortedBy { it.order }
    override val name: String
        get() = "MetaDecorator"
    override val order: Byte
        get() = 0

    override fun configCustomizations(
        protocolConfig: ProtocolConfig,
        baseCustomizations: List<ConfigCustomization>
    ): List<ConfigCustomization> {
        return orderedDecorators.foldRight(baseCustomizations) { decorator: RustCodegenDecorator, customizations ->
            decorator.configCustomizations(protocolConfig, customizations)
        }
    }

    override fun protocols(serviceId: ShapeId, currentProtocols: ProtocolMap): ProtocolMap {
        return orderedDecorators.foldRight(currentProtocols) { decorator, protocolMap ->
            decorator.protocols(serviceId, protocolMap)
        }
    }

    override fun symbolProvider(baseProvider: RustSymbolProvider): RustSymbolProvider {
        return orderedDecorators.foldRight(baseProvider) { decorator, provider -> decorator.symbolProvider(provider) }
    }

    companion object {
        private val logger = Logger.getLogger("RustCodegenSPILoader")
        fun fromClasspath(context: PluginContext): RustCodegenDecorator {
            val decorators = ServiceLoader.load(
                RustCodegenDecorator::class.java,
                context.pluginClassLoader.orElse(RustCodegenDecorator::class.java.classLoader)
            )
                .also { decorators ->
                    decorators.forEach {
                        logger.info("Adding Codegen Decorator: ${it.javaClass.name}")
                    }
                }.toList()
            return CombinedCodegenDecorator(decorators)
        }
    }
}