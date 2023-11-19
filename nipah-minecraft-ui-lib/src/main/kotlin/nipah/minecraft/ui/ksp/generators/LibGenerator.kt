package nipah.minecraft.ui.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import nipah.minecraft.ui.ksp.addCode

class LibGenerator: Generator {
    private val packageName: String
        get() = "nipah.minecraft.ui.lib"

    override fun run(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger): List<KSAnnotated> {
        genGUI(codeGenerator)
        genScreenWrapper(codeGenerator)
        genContainerGUI(codeGenerator)

        genState(codeGenerator)

        genNipahUIScreenHandler(codeGenerator)

        return emptyList()
    }

    private fun genGUI(gen: CodeGenerator) {
        gen.addCode(packageName, "GUI", /* language=kotlin */ """
            package $packageName

            import net.minecraft.entity.player.PlayerEntity
            import net.minecraft.entity.player.PlayerInventory
            import net.minecraft.inventory.Inventory
            import net.minecraft.item.ItemStack
            import net.minecraft.screen.ScreenHandler
            import net.minecraft.screen.slot.Slot

            interface GUI {
                fun makeSlots(playerInventory: PlayerInventory, inventory: Inventory): ContainerGUI
                fun makeScreen(): ContainerGUI
            }
        """.trimIndent())
    }

    private fun genScreenWrapper(gen: CodeGenerator) {
        gen.addCode(packageName, "ScreenWrapper", /* language=kotlin */ """
            package $packageName

            import com.mojang.blaze3d.systems.RenderSystem
            import net.minecraft.client.MinecraftClient
            import net.minecraft.client.gui.screen.ingame.HandledScreen
            import net.minecraft.client.util.math.MatrixStack
            import net.minecraft.entity.player.PlayerInventory
            import net.minecraft.text.Text
            import net.minecraft.util.Identifier
            import net.minecraft.client.font.TextRenderer

            data class ScreenWrapper(
                val width: WrappedProperty<Int>,
                val height: WrappedProperty<Int>,
                val backgroundWidth: WrappedProperty<Int>,
                val backgroundHeight: WrappedProperty<Int>,
                val title: Text,
                val titleX: WrappedProperty<Int>,
                val titleY: WrappedProperty<Int>,
                val textRenderer: WrappedProperty<TextRenderer?>,
                
                val drawTexture: (matrices: MatrixStack?, x: Int, y: Int, u: Int, v: Int, width: Int, height: Int) -> Unit,
                val drawTextWithShadow: (matrices: MatrixStack, textRenderer: TextRenderer, text: Text, x: Int, y: Int, color: Int) -> Unit,
                val drawCenteredText: (matrices: MatrixStack, textRenderer: TextRenderer, text: Text, centerX: Int, y: Int, color: Int) -> Unit
            )

            class WrappedProperty<T>(val get: () -> T, val set: (T) -> Unit)
        """.trimIndent())
    }

    private fun genContainerGUI(gen: CodeGenerator) {
        gen.addCode(packageName, "ContainerGUI", /* language=kotlin */ """
            package $packageName

            import net.minecraft.inventory.Inventory
            import com.mojang.blaze3d.systems.RenderSystem
            import net.minecraft.client.MinecraftClient
            import net.minecraft.client.gui.screen.ingame.HandledScreen
            import net.minecraft.client.util.math.MatrixStack
            import net.minecraft.entity.player.PlayerInventory
            import net.minecraft.text.Text
            import net.minecraft.util.Identifier
            import net.minecraft.screen.slot.Slot
            
            class ContainerGUI {
                val slots = mutableListOf<Slot>()
                val images = mutableListOf<Image>()
                val strings = mutableListOf<StringUI>()

                fun find(name: String): UIElement? {
                    return images.firstOrNull { it.name == name } ?:
                        strings.firstOrNull { it.name == name }
                }
                
                fun slot(inventory: Inventory, index: Int, x: Int, y: Int): Slot {
                    val slot = Slot(inventory, index, x, y)
                    slots.add(slot)
                    return slot
                }

                fun image(id: Identifier, x: Int, y: Int, width: Int, height: Int, name: String = "image"): Image {
                    val image = Image(id, x, y, width, height, name)
                    images.add(image)
                    return image
                }

                fun string(text: String, x: Int, y: Int, color: Int, name: String = "string"): StringUI {
                    val string = StringUI(text, x, y, color, name)
                    strings.add(string)
                    return string
                }

                fun defaultPlayerSlots(playerInventory: PlayerInventory) {
                    var l: Int

                    //The player inventory
                    var m = 0
                    while (m < 3) {
                        l = 0
                        while (l < 9) {
                            slot(playerInventory, l + m * 9 + 9, 8 + l * 18, 84 + m * 18)
                            ++l
                        }
                        ++m
                    }
            
                    //The player Hotbar
                    m = 0
                    while (m < 9) {
                        slot(playerInventory, m, 8 + m * 18, 142)
                        ++m
                    }
                }

                fun drawBackground(matrices: MatrixStack?, delta: Float, mouseX: Int, mouseY: Int, screen: ScreenWrapper) {
                    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F)
                    images.forEach {
                        MinecraftClient.getInstance().textureManager.bindTexture(it.id)
                        if(it.isBackground) {
                            val width = screen.width.get()
                            val height = screen.height.get()

                            val backgroundWidth = screen.backgroundWidth.get()
                            val backgroundHeight = screen.backgroundHeight.get()
                        
                            val x = (width - backgroundWidth) / 2
                            val y = (height - backgroundHeight) / 2
                            screen.drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight)
                        } else {
                            screen.drawTexture(matrices, it.x, it.y, 0, 0, it.width, it.height)
                        }
                    }

                    if(matrices == null) {
                        return
                    }

                    strings.forEach {
                        screen.drawCenteredText(matrices, screen.textRenderer.get()!!, Text.of(it.text), it.x, it.y, it.color)
                    }
                }
            }

            open class UIElement(val name: String)

            class Image(var id: Identifier, var x: Int, var y: Int, var width: Int, var height: Int, name: String = "image"): UIElement(name) {
                var isBackground = false

                fun background(): Image {
                    isBackground = true
                    return this
                }
            }

            class StringUI(var text: String, var x: Int, var y: Int, var color: Int, name: String = "string"): UIElement(name)
        """.trimIndent())
    }

    private fun genState(gen: CodeGenerator) {
        gen.addCode(packageName, "State", /* language=kotlin */ """
            package $packageName

            interface UIState
        """.trimIndent())
    }

    private fun genNipahUIScreenHandler(gen: CodeGenerator) {
        gen.addCode(packageName, "NipahUIScreenHandler", /* language=kotlin */ """
            package $packageName

            import net.minecraft.screen.ScreenHandlerType
            import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry

            interface NipahUIScreenHandler {
                val entityId: Long
                
                fun updateState(buf: net.minecraft.network.PacketByteBuf)
            }
        """.trimIndent())
    }
}
