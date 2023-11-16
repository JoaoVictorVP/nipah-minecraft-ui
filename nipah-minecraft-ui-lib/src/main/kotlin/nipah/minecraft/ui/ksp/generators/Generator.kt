package nipah.minecraft.ui.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

interface Generator {
    fun firstRoundRun(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger) {}

    fun run(
        resolver: Resolver,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ): List<KSAnnotated>
}
