package nipah.minecraft.ui.ksp

fun KGen.Source.mcImportForBlocks(): KGen.Source {
    this import "net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry"
    this import "net.fabricmc.fabric.api.item.v1.FabricItemSettings"
    this import "net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings"
    this import "net.fabricmc.fabric.impl.container.ContainerProviderImpl"
    this import "net.minecraft.block.Block"
    this import "net.minecraft.block.BlockEntityProvider"
    this import "net.minecraft.block.BlockState"
    this import "net.minecraft.block.entity.BlockEntity"
    this import "net.minecraft.entity.player.PlayerEntity"
    this import "net.minecraft.item.BlockItem"
    this import "net.minecraft.item.ItemGroup"
    this import "net.minecraft.screen.NamedScreenHandlerFactory"
    this import "net.minecraft.util.ActionResult"
    this import "net.minecraft.util.Hand"
    this import "net.minecraft.util.Identifier"
    this import "net.minecraft.util.ItemScatterer"
    this import "net.minecraft.util.hit.BlockHitResult"
    this import "net.minecraft.util.math.BlockPos"
    this import "net.minecraft.util.registry.Registry"
    this import "net.minecraft.world.BlockView"
    this import "net.minecraft.world.World"

    return this
}

fun KGen.Source.mcImportForInventories(): KGen.Source {
    this import "net.minecraft.entity.player.PlayerEntity"
    this import "net.minecraft.inventory.Inventories"
    this import "net.minecraft.inventory.Inventory"
    this import "net.minecraft.inventory.SidedInventory"
    this import "net.minecraft.item.ItemStack"
    this import "net.minecraft.util.collection.DefaultedList"
    this import "net.minecraft.util.math.Direction"

    return this
}

fun KGen.Source.mcImportForBlockEntities(): KGen.Source {
    this import "net.minecraft.block.BlockState"
    this import "net.minecraft.block.entity.BlockEntity"
    this import "net.minecraft.block.entity.BlockEntityType"
    this import "net.minecraft.entity.player.PlayerEntity"
    this import "net.minecraft.entity.player.PlayerInventory"
    this import "net.minecraft.inventory.Inventories"
    this import "net.minecraft.item.ItemStack"
    this import "net.minecraft.nbt.NbtCompound"
    this import "net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory"
    this import "net.minecraft.screen.ScreenHandler"
    this import "net.minecraft.text.Text"
    this import "net.minecraft.util.collection.DefaultedList"
    this import "net.minecraft.util.math.Direction"
    this import "net.minecraft.util.registry.Registry"

    this import "net.minecraft.network.PacketByteBuf"
    this import "net.minecraft.server.network.ServerPlayerEntity"
    this import "net.fabricmc.fabric.api.networking.v1.PacketByteBufs"
    this import "net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking"

    return this
}

fun KGen.Source.mcImportForScreenHandlers(): KGen.Source {
    this import "nipah.minecraft.ui.lib.NipahUIScreenHandler"
    this import "net.minecraft.entity.player.PlayerEntity"
    this import "net.minecraft.entity.player.PlayerInventory"
    this import "net.minecraft.inventory.Inventory"
    this import "net.minecraft.inventory.SimpleInventory"
    this import "net.minecraft.item.ItemStack"
    this import "net.minecraft.screen.ScreenHandler"
    this import "net.minecraft.screen.slot.Slot"
    this import "net.minecraft.screen.ScreenHandlerType"
    this import "net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry"
    this import "net.minecraft.network.PacketByteBuf"

    return this
}

fun KGen.Source.mcImportForScreens(): KGen.Source {
    this import "com.mojang.blaze3d.systems.RenderSystem"
    this import "net.minecraft.client.MinecraftClient"
    this import "net.minecraft.client.gui.screen.ingame.HandledScreen"
    this import "net.minecraft.client.util.math.MatrixStack"
    this import "net.minecraft.entity.player.PlayerInventory"
    this import "net.minecraft.text.Text"
    this import "net.minecraft.util.Identifier"

    return this
}
