package nipah.minecraft.ui.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName

fun CodeGenerator.addCode(packageName: String, fileName: String, code: String) {
    val file = this.createNewFile(
        Dependencies(true),
        packageName,
        fileName
    )
    file.bufferedWriter().use { writer ->
        writer.write(code.trimIndent())
    }
}

fun CodeGenerator.addCode(source: KGen.Source) {
    this.addCode(source.packageName, source.fileName, source.make())
}

fun KSDeclaration.eitherNames(): KSName {
    return this.qualifiedName ?: this.simpleName
}
