import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.util.Identifier
import nipah.minecraft.ui.annotations.BlockAsContainer
import nipah.minecraft.ui.lib.NipahUIModInitializer

@BlockAsContainer(itemCount = 1, gui = MyBlockGUI::class)
class MyBlock(s: FabricBlockSettings): AbstractMyBlock(s) {
    companion object {
        val ID = Identifier("my-mod-example", "my-block")
    }

    fun init() {
        NipahUIModInitializer.init()
        NipahUIModInitializer.initClient()
    }
}
