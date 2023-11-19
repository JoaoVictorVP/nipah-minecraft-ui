package nipah.minecraft.ui.ksp.generators.utils

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import nipah.minecraft.ui.ksp.inheritsFrom

data class GUIInfo (
    val packageName: String,
    val simpleName: String,
    val qualifiedName: String,

    val events: List<GUIEvent>,

    val state: GUIStateInfo?
) {
    fun hasEvent(event: GUIEvent) = event in events
}
enum class GUIEvent {
    Tick,
    TickWithInfo,
    Render
}

data class GUIStateInfo(
    val packageName: String,
    val simpleName: String,
    val qualifiedName: String,

    val properties: List<GUIStatePropertyInfo>
)
data class GUIStatePropertyInfo(
    val name: String,
    val type: String,

    val bufWriter: String,
    val bufReader: String
)

fun parseGUIInfo(gui: KSClassDeclaration, logger: KSPLogger): GUIInfo? {
    val packageName = gui.packageName.asString()
    val simpleName = gui.simpleName.asString()
    val qualifiedName = gui.qualifiedName?.asString() ?: run {
        logger.error("GUI $simpleName must be top-level", gui)
        return null
    }

    val events = gui.getAllFunctions().mapNotNull {
        when(it.simpleName.asString()) {
            "tick" -> {
                val ret = it.returnType?.resolve()?.declaration?.qualifiedName?.asString()
                if(ret != null && ret != "kotlin.Boolean") {
                    logger.error("GUI tick function must return kotlin.Boolean (true = the state will be sent to the clients, false = nothing important happened)", it)
                    return@mapNotNull null
                }

                val fparam = it.parameters.elementAtOrNull(0)
                val sparam = it.parameters.elementAtOrNull(1)

                if(fparam?.type?.resolve()?.declaration?.qualifiedName?.asString() == "net.minecraft.world.World"
                    && sparam?.type?.resolve()?.declaration?.qualifiedName?.asString() == "net.minecraft.util.math.BlockPos") {
                    GUIEvent.TickWithInfo
                }
                else GUIEvent.Tick
            }
            "render" -> {
                val fparam = it.parameters.elementAtOrNull(0)
                if(fparam == null || fparam.type.resolve().declaration.qualifiedName?.asString() != "nipah.minecraft.ui.lib.ContainerGUI") {
                    logger.error("GUI render function must have a parameter of type nipah.minecraft.ui.lib.ContainerGUI", it)
                    return@mapNotNull null
                }

                GUIEvent.Render
            }
            else -> null
        }
    }

    val state = gui.getAllProperties().find { it.simpleName.asString() == "state" }
    if(state != null) {
        if (!state.isMutable) {
            logger.error("GUI state ${state.qualifiedName?.asString() ?: state.simpleName} must be mutable", gui)
            return null
        }

        val stateType = state.type.resolve().declaration as? KSClassDeclaration ?: return null
        if (!stateType.inheritsFrom("nipah.minecraft.ui.lib.UIState")) {
            logger.error("GUI state ${state.qualifiedName?.asString() ?: state.simpleName} must be of type nipah.minecraft.ui.lib.UIState", gui)
            return null
        }

        val stateInfo = parseGUIState(stateType, logger)

        return GUIInfo(packageName, simpleName, qualifiedName, events.toList(), stateInfo)
    }

    return GUIInfo(packageName, simpleName, qualifiedName, events.toList(), null)
}

private fun parseGUIState(type: KSClassDeclaration, logger: KSPLogger): GUIStateInfo? {
    val packageName = type.packageName.asString()
    val simpleName = type.simpleName.asString()
    val qualifiedName = type.qualifiedName?.asString() ?: return null

    val properties = type.getAllProperties().mapNotNull { prop ->
        val name = prop.simpleName.asString()
        val propType = prop.type.resolve().declaration.qualifiedName?.asString() ?: return@mapNotNull null

        val typed = when(propType) {
            "kotlin.Int" -> "Int"
            "kotlin.Long" -> "Long"
            "kotlin.Float" -> "Float"
            "kotlin.Double" -> "Double"
            "kotlin.Boolean" -> "Boolean"
            "kotlin.String" -> "String"
            "net.minecraft.util.Identifier" -> "Identifier"
            "net.minecraft.text.Text" -> "Text"
            "net.minecraft.util.math.BlockPos" -> "BlockPos"
            "java.util.Date" -> "Date"
            "java.util.UUID" -> "UUID"

            else -> {
                logger.error("Unsupported type $propType for property $name")
                return@mapNotNull null
            }
        }

        val bufWriter = "write${typed}"
        val bufReader = "read${typed}"

        GUIStatePropertyInfo(name, propType, bufWriter, bufReader)
    }

    return GUIStateInfo(packageName, simpleName, qualifiedName, properties.toList())
}
