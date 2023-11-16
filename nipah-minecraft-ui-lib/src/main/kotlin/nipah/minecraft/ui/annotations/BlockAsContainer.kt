package nipah.minecraft.ui.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class BlockAsContainer(
    val itemCount: Int,
    val gui: KClass<*>,
    val autoRegisterBlock: Boolean = true
)
