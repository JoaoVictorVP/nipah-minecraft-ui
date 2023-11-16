package nipah.minecraft.ui.ksp.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import nipah.minecraft.ui.ksp.*

/* Steps
    * Identify the block type and check the descendants
    * Requires the existence of an ID companion object member with the block Identifier for later usage
    * Generate an AbstractBlock for the block with the needed functions implemented, so the user can inherit from it without problems
    * Generate an BaseBlockEntity for the block so the users can opt either to use it as default or to overwrite it if needed
*/

class BlockAsContainer: Generator {
    data class Src(val type: KSClassDeclaration, val annotation: KSAnnotation)

    private lateinit var sources: List<Src>
    private val deferred = mutableListOf<KSClassDeclaration>()

    override fun firstRoundRun(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger) {
        sources = resolver.getSymbolsWithAnnotation("nipah.minecraft.ui.annotations.BlockAsContainer").mapNotNull { src ->
            val type = src as? KSClassDeclaration ?: return@mapNotNull null
            val annotation = src.annotations.find { it.shortName.asString() == "BlockAsContainer" } ?: return@mapNotNull null

            Src(type, annotation)
        }.toList()
    }

    override fun run(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger): List<KSAnnotated> {
        if(deferred.isNotEmpty()) {
            deferred.forEach {
                checkInheritance(it, logger)
            }
            return emptyList()
        }

        sources.forEach { src ->
            val (type, annotation) = src

            if(type.isCompanionObject) {
                logger.error("BlockAsContainer cannot be applied to companion objects", type.primaryConstructor ?: type)
                return@forEach
            }

            if(!checkTypePrimaryCtorForBlockSettings(type)) {
                logger.error("BlockAsContainer requires the existence of a primary constructor with a FabricBlockSettings parameter", type)
                return@forEach
            }

            val companion = type.declarations.find {
                (it as? KSClassDeclaration ?: return@find false).isCompanionObject
            } as? KSClassDeclaration ?: kotlin.run {
                logger.error("BlockAsContainer requires a companion object", type)
                return@forEach
            }

            val id = companion.getAllProperties().firstOrNull { it.simpleName.asString() == "ID" }
            if(id == null) {
                logger.error("BlockAsContainer requires the existence of an ID companion object member with the block Identifier for later usage", type)
                return@forEach
            }
            if(id.type.resolve().declaration.let { it.qualifiedName?.asString() != "net.minecraft.util.Identifier" }) {
                logger.error("BlockAsContainer requires the existence of an ID companion object member with the block Identifier for later usage", type)
                return@forEach
            }

            val itemCount = annotation.arguments.find { it.name?.asString() == "itemCount" }?.value as? Int ?: 1

            val gui = annotation.arguments.find { it.name?.asString() == "gui" }?.value?.let {
                val guiType = it as? KSType ?: kotlin.run {
                    logger.error("BlockAsContainer requires the existence of a gui class", type)
                    return@forEach
                }

                val guiClass = guiType.declaration as? KSClassDeclaration ?: kotlin.run {
                    logger.error("BlockAsContainer requires the existence of a gui class", type)
                    return@forEach
                }

                if(guiClass.isCompanionObject) {
                    logger.error("BlockAsContainer cannot be applied to companion objects", guiClass)
                    return@forEach
                }
                if(!guiClass.superTypes.any { superType ->
                        superType.resolve().declaration.eitherNames().asString().contains("GUI")
                    }) {
                    logger.error("BlockAsContainer requires that the provided GUI type ${guiClass.simpleName.asString()} implements GUI", type)
                    return@forEach
                }
                guiClass
            } ?: kotlin.run {
                logger.error("BlockAsContainer requires the existence of a gui class", type)
                return@forEach
            }
            val guiName = gui.qualifiedName ?: kotlin.run {
                logger.error("BlockAsContainer requires the existence of a gui class", type)
                return@forEach
            }

            val autoRegisterBlock = annotation.arguments.find { it.name?.asString() == "autoRegisterBlock" }?.value as? Boolean ?: true

            val abstractBlockName = makeAbstractBlock(resolver, codeGenerator, logger, type, autoRegisterBlock) ?: return@forEach
            makeInventory(codeGenerator, type)
            makeBaseBlockEntity(resolver, codeGenerator, logger, type, abstractBlockName, itemCount)
            makeScreenHandler(resolver, codeGenerator, logger, type, itemCount, guiName.asString())
            makeScreen(resolver, codeGenerator, logger, type, guiName.asString())

            deferred.add(type)
        }

        makeInitializers(resolver, codeGenerator, logger)

        return deferred
    }

    private fun checkInheritance(type: KSClassDeclaration, logger: KSPLogger) {
        val className = type.simpleName.asString()
        val abstractClassName = "Abstract${className.firstLetterAsCapital()}"

        if(type.superTypes.any { superType ->
            superType.resolve().declaration.let {
                it.qualifiedName?.asString()?.contains(abstractClassName) == true
            }
        }) return

        logger.error("BlockAsContainer requires the inheritance of $abstractClassName", type)
    }

    private fun checkTypePrimaryCtorForBlockSettings(type: KSClassDeclaration): Boolean {
        val primaryCtor = type.primaryConstructor ?: return false
        val settingsParam = primaryCtor.parameters.find { it.type.resolve().declaration.let { it.qualifiedName?.asString() == "net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings" } } ?: return false
        return true
    }

    private fun abstractNameGen(className: String): String {
        return "Abstract${className.firstLetterAsCapital()}"
    }

    private fun baseEntityBlockNameGen(className: String): String {
        return "Base${className}Entity"
    }

    private fun inventoryNameGen(className: String): String {
        return "${className}Inventory"
    }

    private fun screenHandlerNameGen(className: String): String {
        return "${className}ScreenHandler"
    }

    private fun screenNameGen(className: String): String {
        return "${className}Screen"
    }

    private fun makeAbstractBlock(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger, type: KSClassDeclaration, autoRegisterBlock: Boolean): String? {
        val packageName = type.packageName.asString()
        val className = type.simpleName.asString()
        val qualifiedName = type.qualifiedName?.asString() ?: return null

        val abstractClassName = abstractNameGen(className)

        val src = KGen.Source(packageName, abstractClassName).mcImportForBlocks() maker
                KGen.Class(abstractClassName).apply {
                    this abstract true
                    this with KGen.PrimaryConstructor().apply {
                        this with KGen.Param("settings", "FabricBlockSettings")
                    }

                    this inherits KGen.Class.Inherits("Block", "settings")
                    this inherits KGen.Class.Inherits("BlockEntityProvider")

                    this with KGen.Class.CompanionObject().apply {
                        this with KGen.Field("self", qualifiedName, "$qualifiedName(FabricBlockSettings.copyOf(net.minecraft.block.Blocks.BREWING_STAND))")

                        this with KGen.Fun("init").apply {
                            body("""
                                ${if(autoRegisterBlock) {
                                    "Registry.register(Registry.BLOCK, $qualifiedName.ID, self)\n" +
                                    "Registry.register(Registry.ITEM, $qualifiedName.ID, BlockItem(self, FabricItemSettings().group(ItemGroup.BREWING)))\n"
                                } else ""}
                                ContainerProviderImpl.INSTANCE.registerFactory($qualifiedName.ID) { syncId, _, player, _ ->
                                    ${screenHandlerNameGen(className)}(syncId, player.inventory)
                                }
                            """.trimIndent())
                        }

                        this with KGen.Fun("initClient").apply {
                            body("""
                                ScreenRegistry.register(null!!, ::${screenNameGen(className)})
                            """.trimIndent())
                        }
                    }

                    this with KGen.Fun.override("createBlockEntity").apply {
                        this with KGen.Param("world", "BlockView")
                        this returns "BlockEntity"
                        body("return ${baseEntityBlockNameGen(className)}()")
                    }

                    this with KGen.Fun.override("onStateReplaced").apply {
                        this with KGen.Param("state", "BlockState")
                        this with KGen.Param("world", "World")
                        this with KGen.Param("pos", "BlockPos")
                        this with KGen.Param("newState", "BlockState")
                        this with KGen.Param("moved", "Boolean")
                        body("""
                            if (state.block !== newState.block) {
                                val blockEntity = world.getBlockEntity(pos)
                                if (blockEntity is ${baseEntityBlockNameGen(className)}) {
                                    ItemScatterer.spawn(world, pos, blockEntity)
                                    // update comparators
                                    world.updateComparators(pos, this)
                                }
                                super.onStateReplaced(state, world, pos, newState, moved)
                            }
                        """.trimIndent())
                    }

                    this with KGen.Fun.override("onUse").apply {
                        this with KGen.Param("state", "BlockState")
                        this with KGen.Param("world", "World")
                        this with KGen.Param("pos", "BlockPos")
                        this with KGen.Param("player", "PlayerEntity")
                        this with KGen.Param("hand", "Hand")
                        this with KGen.Param("hit", "BlockHitResult")
                        this returns "ActionResult"
                        body("""
                            if (world!!.isClient) {
                                return ActionResult.SUCCESS
                            }
                            
                            val screenHandlerFactory = state!!.createScreenHandlerFactory(world, pos)
                            player.openHandledScreen(screenHandlerFactory)
                            return ActionResult.CONSUME
                        """.trimIndent())
                    }

                    this with KGen.Fun.override("createScreenHandlerFactory").apply {
                        this with KGen.Param("state", "BlockState")
                        this with KGen.Param("world", "World")
                        this with KGen.Param("pos", "BlockPos")
                        this returns "NamedScreenHandlerFactory?"
                        body("""
                            val blockEntity = world!!.getBlockEntity(pos)
                            return if (blockEntity is NamedScreenHandlerFactory) {
                                blockEntity
                            } else null
                        """.trimIndent())
                    }
                }

        codeGenerator.addCode(src)

        return abstractClassName
    }

    private fun makeInventory(codeGenerator: CodeGenerator, type: KSClassDeclaration) {
        val packageName = type.packageName.asString()
        val className = type.simpleName.asString()

        val inventoryName = inventoryNameGen(className)

        val src = KGen.Source(packageName, inventoryName).mcImportForInventories() maker
                KGen.Interface(inventoryName).apply {
                    this attribute "FunctionalInterface"
                    this inherits KGen.Class.Inherits("SidedInventory")

                    this raw """
                            companion object {
                                fun of(items: DefaultedList<ItemStack?>?): $inventoryName {
                                    return object : $inventoryName {
                                        override fun getItems() = items!!
                                        override fun getAvailableSlots(side: Direction?): IntArray {
                                            val slots = IntArray(items!!.size)
                                            for (i in slots.indices) {
                                                slots[i] = i
                                            }
                                            return slots
                                        }

                                        override fun canInsert(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
                                            return true
                                        }

                                        override fun canExtract(slot: Int, stack: ItemStack?, dir: Direction?): Boolean {
                                            return true
                                        }
                                    }
                                }

                                fun ofSize(size: Int): $inventoryName? {
                                    return of(DefaultedList.ofSize(size, ItemStack.EMPTY))
                                }
                            }

                            fun getItems(): DefaultedList<ItemStack?>

                            override fun size(): Int {
                                return getItems().size
                            }

                            override fun isEmpty(): Boolean {
                                for (i in 0 until size()) {
                                    val stack = getStack(i)
                                    if (!stack.isEmpty) {
                                        return false
                                    }
                                }
                                return true
                            }

                            override fun getStack(slot: Int): ItemStack {
                                return getItems()[slot]
                            }

                            override fun removeStack(slot: Int, count: Int): ItemStack? {
                                val result = Inventories.splitStack(getItems(), slot, count)
                                if (!result.isEmpty) {
                                    markDirty()
                                }
                                return result
                            }

                            override fun removeStack(slot: Int): ItemStack? {
                                return Inventories.removeStack(getItems(), slot)
                            }

                            override fun setStack(slot: Int, stack: ItemStack) {
                                getItems()[slot] = stack
                                if (stack.count > stack.maxCount) {
                                    stack.count = stack.maxCount
                                }
                            }

                            override fun clear() {
                                getItems().clear()
                            }

                            override fun markDirty() {
                                // Override if you want behavior.
                            }

                            override fun canPlayerUse(player: PlayerEntity?): Boolean {
                                return true
                            }
                    """.trimIndent()
                }

        codeGenerator.addCode(src)
    }

    private fun makeBaseBlockEntity(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger, type: KSClassDeclaration, abstractBlockName: String, itemCount: Int) {
        val packageName = type.packageName.asString()
        val className = type.simpleName.asString()
        val qualifiedName = type.qualifiedName?.asString() ?: return

        val blockEntityName = baseEntityBlockNameGen(className)

        val src = KGen.Source(packageName, blockEntityName).mcImportForBlockEntities() maker
                KGen.Class(blockEntityName).apply {
                    this inherits KGen.Class.Inherits("BlockEntity", "TYPE")
                    this inherits KGen.Class.Inherits(inventoryNameGen(className))
                    this inherits KGen.Class.Inherits("NamedScreenHandlerFactory")
                    this with KGen.Class.CompanionObject().apply {
                        this with KGen.Field("TYPE", "BlockEntityType<$blockEntityName>", "BlockEntityType.Builder.create(::$blockEntityName, $abstractBlockName.self).build(null)")

                        this with KGen.Fun("init").apply {
                            body("""
                                Registry.register(Registry.BLOCK_ENTITY_TYPE, $qualifiedName.ID, TYPE)
                            """.trimIndent())
                        }
                    }

                    this with KGen.Field("items", "", "DefaultedList.ofSize($itemCount, ItemStack.EMPTY)").private()

                    this with KGen.Fun.override("getItems").apply {
                       this returns "DefaultedList<ItemStack?>"
                       body("return items")
                    }

                    this with KGen.Fun.override("markDirty").apply {
                        body("super<BlockEntity>.markDirty()")
                    }

                    this with KGen.Fun.override("getAvailableSlots").apply {
                        this with KGen.Param("side", "Direction?")
                        this returns "IntArray"
                        body("return intArrayOf(0)")
                    }

                    this with KGen.Fun.override("canInsert").apply {
                        this with KGen.Param("slot", "Int")
                        this with KGen.Param("stack", "ItemStack?")
                        this with KGen.Param("dir", "Direction?")
                        this returns "Boolean"
                        body("return true")
                    }

                    this with KGen.Fun.override("canExtract").apply {
                        this with KGen.Param("slot", "Int")
                        this with KGen.Param("stack", "ItemStack?")
                        this with KGen.Param("dir", "Direction?")
                        this returns "Boolean"
                        body("return true")
                    }

                    this with KGen.Fun.override("fromTag").apply {
                        this with KGen.Param("state", "BlockState?")
                        this with KGen.Param("tag", "NbtCompound?")
                        body("super.fromTag(state, tag)\nInventories.readNbt(tag, items)")
                    }

                    this with KGen.Fun.override("canPlayerUse").apply {
                        this with KGen.Param("player", "PlayerEntity?")
                        this returns "Boolean"
                        body("return pos.isWithinDistance(player!!.pos, 4.5)")
                    }

                    this with KGen.Fun.override("onOpen").apply {
                        this with KGen.Param("player", "PlayerEntity?")
                        body("super.onOpen(player)")
                    }

                    this with KGen.Fun.override("writeNbt").apply {
                        this with KGen.Param("nbt", "NbtCompound?")
                        this returns "NbtCompound"
                        body("val x = Inventories.writeNbt(nbt, items)\nreturn super.writeNbt(x)")
                    }

                    this with KGen.Fun.override("createMenu").apply {
                        this with KGen.Param("syncId", "Int")
                        this with KGen.Param("inv", "PlayerInventory?")
                        this with KGen.Param("player", "PlayerEntity?")
                        this returns "ScreenHandler?"
                        body("""
                            if(inv == null) {
                                return null
                            }
                            
                            return ${screenHandlerNameGen(className)}(syncId, inv, this)
                        """.trimIndent())
                    }

                    this with KGen.Fun.override("getDisplayName").apply {
                        this returns "Text"
                        body("return ${abstractBlockName}.self.name")
                    }
                }

        codeGenerator.addCode(src)
    }

    private fun makeScreenHandler(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger, type: KSClassDeclaration, itemsCount: Int, gui: String) {
        val packageName = type.packageName.asString()
        val className = type.simpleName.asString()
        val qualifiedName = type.qualifiedName?.asString() ?: return

        val screenHandlerName = screenHandlerNameGen(className)

        val inventoryName = inventoryNameGen(className)

        val src = KGen.Source(packageName, screenHandlerName).mcImportForScreenHandlers() maker
                KGen.Class(screenHandlerName).apply {
                    this inherits KGen.Class.Inherits("ScreenHandler")

                    this with KGen.Class.CompanionObject().apply {
                        this with KGen.Field("SCREEN_HANDLER", "ScreenHandlerType<$screenHandlerName>").lateInit()

                        this with KGen.Fun("init").apply {
                            body("""
                                SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(
                                    $qualifiedName.ID,
                                    ::$screenHandlerName
                                )
                            """.trimIndent())
                        }
                    }

                    this with KGen.Field("inventory", "Inventory").private()

                    this with KGen.Constructor().apply {
                        this with KGen.Param("syncId", "Int")
                        this with KGen.Param("playerInventory", "PlayerInventory")
                        this calling "this(syncId, playerInventory, SimpleInventory($itemsCount))"
                    }

                    this with KGen.Constructor().apply {
                        this with KGen.Param("syncId", "Int")
                        this with KGen.Param("playerInventory", "PlayerInventory")
                        this with KGen.Param("inventory", "Inventory")
                        this calling "super(SCREEN_HANDLER, syncId)"

                        this body """
                            checkSize(inventory, $itemsCount)
                            this.inventory = inventory
                            inventory.onOpen(playerInventory.player)
                    
                            init(playerInventory)
                        """.trimIndent()
                    }

                    this with KGen.Fun("init").apply {
                        this with KGen.Param("playerInventory", "PlayerInventory")

                        this body """
                            val gui = $gui()
                            val container = gui.makeSlots(playerInventory, inventory)
                            
                            container.slots.forEach(this::addSlot)
                        """.trimIndent()
                    }

                    this with KGen.Fun.override("canUse").apply {
                        this with KGen.Param("player", "PlayerEntity?")
                        this returns "Boolean"
                        body("return inventory.canPlayerUse(player)")
                    }

                    this with KGen.Fun.override("transferSlot").apply {
                        this with KGen.Param("player", "PlayerEntity?")
                        this with KGen.Param("invSlot", "Int")
                        this returns "ItemStack?"
                        body("""
                            var newStack = ItemStack.EMPTY
                            val slot = slots[invSlot]
                            if (slot != null && slot.hasStack()) {
                                val originalStack = slot.stack
                                newStack = originalStack.copy()
                                if (invSlot < inventory.size()) {
                                    if (!insertItem(originalStack, inventory.size(), slots.size, true)) {
                                        return ItemStack.EMPTY
                                    }
                                } else if (!insertItem(originalStack, 0, inventory.size(), false)) {
                                    return ItemStack.EMPTY
                                }
                                if (originalStack.isEmpty) {
                                    slot.stack = ItemStack.EMPTY
                                } else {
                                    slot.markDirty()
                                }
                            }
                            return newStack
                        """.trimIndent())
                    }
                }

        codeGenerator.addCode(src)
    }

    private fun makeScreen(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger, type: KSClassDeclaration, gui: String) {
        val packageName = type.packageName.asString()
        val className = type.simpleName.asString()
        val qualifiedName = type.qualifiedName?.asString() ?: return

        val screenHandlerName = screenHandlerNameGen(className)

        val screenName = screenNameGen(className)

        val src = KGen.Source(packageName, screenName).mcImportForScreens() maker
                KGen.Class(screenName).apply {
                    this with KGen.PrimaryConstructor().apply {
                        this with KGen.Param("handler", screenHandlerName)
                        this with KGen.Param("playerInventory", "PlayerInventory")
                        this with KGen.Param("title", "Text")
                    }

                    this inherits KGen.Class.Inherits("HandledScreen", "handler, playerInventory, title").generics(screenHandlerName)

                    this with KGen.Field("gui", "", "$gui()").private()

                    this with KGen.Field("container", "ContainerGUI").private().lateInit()

                    this with KGen.Field("wrapper", "", """
                        ScreenWrapper(
                            WrappedProperty({ width }, { width = it }),
                            WrappedProperty({ height }, { height = it }),
                            WrappedProperty({ backgroundWidth }, { backgroundWidth = it }),
                            WrappedProperty({ backgroundHeight }, { backgroundHeight = it }),
                            title,
                            WrappedProperty({ titleX }, { titleX = it }),
                            WrappedProperty({ titleY }, { titleY = it }),
                            textRenderer,
                            
                            ::drawTexture,
                            ::drawTextWithShadow,
                            ::drawCenteredText
                        )
                    """.trimIndent()).private()

                    this with KGen.Fun.override("init").apply {
                        body("""
                            super.init()
                            titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
                            container = gui.makeScreen()
                        """.trimIndent())
                    }

                    this with KGen.Fun.override("render").apply {
                        this with KGen.Param("matrices", "MatrixStack")
                        this with KGen.Param("mouseX", "Int")
                        this with KGen.Param("mouseY", "Int")
                        this with KGen.Param("delta", "Float")
                        body("""
                            renderBackground(matrices)
                            super.render(matrices, mouseX, mouseY, delta)
                            drawMouseoverTooltip(matrices, mouseX, mouseY)
                        """.trimIndent())
                    }

                    this with KGen.Fun.override("drawBackground").apply {
                        this with KGen.Param("matrices", "MatrixStack")
                        this with KGen.Param("delta", "Float")
                        this with KGen.Param("mouseX", "Int")
                        this with KGen.Param("mouseY", "Int")
                        body("""
                            container.drawBackground(matrices, delta, mouseX, mouseY, wrapper)
                        """.trimIndent())
                    }
                }

        codeGenerator.addCode(src)
    }

    private fun makeInitializers(resolver: Resolver, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val src = KGen.Source("nipah.minecraft.ui.lib", "NipahUIModInitializer") maker
                KGen.Singleton("NipahUIModInitializer").apply {
                    this with KGen.Fun("init").apply {
                        body("""
                        ${sources.map { src ->
                            val (type, _) = src
                            val className = type.simpleName.asString()
                            
                            """
                                // $className
                                ${abstractNameGen(className)}.init()
                                ${baseEntityBlockNameGen(className)}.init()
                                ${screenHandlerNameGen(className)}.init()
                            """.trimIndent()
                        }.joinToString()}
                        """.trimIndent())
                    }

                    this with KGen.Fun("initClient").apply {
                        body("""
                        ${sources.map { src ->
                            val (type, _) = src
                            val className = type.simpleName.asString()

                            """
                                // $className
                                ${abstractNameGen(className)}.initClient()
                            """.trimIndent()
                        }.joinToString()}
                        """.trimIndent())
                    }
                }

        codeGenerator.addCode(src)
    }
}
