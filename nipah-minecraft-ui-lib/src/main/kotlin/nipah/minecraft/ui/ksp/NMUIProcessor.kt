package nipah.minecraft.ui.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import nipah.minecraft.ui.ksp.generators.BlockAsContainer
import nipah.minecraft.ui.ksp.generators.Generator
import nipah.minecraft.ui.ksp.generators.LibGenerator

class NMUIProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private var priorityGeneratorsRun = false

    private val priorityGenerators = arrayOf(
        LibGenerator()
    )
    private val generators = arrayOf<Generator>(
        BlockAsContainer()
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if(!priorityGeneratorsRun) {
            generators.forEach { it.firstRoundRun(resolver, codeGenerator, logger) }
            priorityGenerators.flatMap { it.run(resolver, codeGenerator, logger) }
            priorityGeneratorsRun = true
            return listOf(resolver.builtIns.anyType.declaration)
        }

        return generators.flatMap { it.run(resolver, codeGenerator, logger) }
    }
}
