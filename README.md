# nipah-minecraft-ui
A library and code generator (ksp) to simplify the process of creating user interfaces (UI) in Fabric with Kotlin for Minecraft

(For now, only container blocks and UI are supported)

# How To Use
To create an easy container, just do the following steps.

## Installing
To install our plugin, first go to your build.gradle:
```gradle
plugins {
  ...
  id "org.jetbrains.kotlin.jvm" version "1.9.20"
	id 'com.google.devtools.ksp' version '1.9.20-1.0.14'
}
```
Ensure you have these two plugins above

Then on your 'allprojects.repositories':
```gradle
allprojects {
	repositories {
    ...
		maven { url "https://jitpack.io" }
    ...
	}
}
```
Ensure you have this maven setup with this url

And then, on your 'dependencies':
```gradle
dependencies {
  ...
	// Nipah Minecraft UI
	implementation 'com.github.JoaoVictorVP:nipah-minecraft-ui:<version>'
	ksp 'com.github.JoaoVictorVP:nipah-minecraft-ui:<version>'
}
```
Where you replace <version> with the version you want to install, for example you can install the latest: 385948af29

After that, sync your changes and compile the project once for making sure the base classes we need are being generated.

## Create a block
Create the block that you will use as a container:
![image](https://github.com/JoaoVictorVP/nipah-minecraft-ui/assets/98046863/0efb84d5-b321-4cce-9674-cfac47396b56)

Also configure the ID of your block in a companion object, with the name 'ID' as shown in the image.
![image](https://github.com/JoaoVictorVP/nipah-minecraft-ui/assets/98046863/d3f1fbf9-8e52-4568-9ccc-3b37b6e5b469)
You do not need the tiny const id, only the ID: Identifier

After that, you put the @BlockAsContainer annotation on your block with the following configs:
![image](https://github.com/JoaoVictorVP/nipah-minecraft-ui/assets/98046863/2e24d0dd-569c-4d43-87a5-14ef98ce2209)
Where:
 itemCount -> represents the number of slots your container will have
 gui -> represents your GUI class (I will show how to implement it below)
 autoRegisterBlock -> represents wheter or not you do want the code generator to also register your block and block item, you can set it to false if you want to do it manually

## Create the GUI
Now you can go further and implement your GUI class, to do it, make a new class and inherit from the generated 'GUI' class on your project. Then implement these two:
```kotlin
override fun makeSlots(playerInventory: PlayerInventory, inventory: Inventory): ContainerGUI {
    val gui = ContainerGUI()

    gui.slot(inventory, 0, 80, 35)

    gui.defaultPlayerSlots(playerInventory)

    return gui
}

override fun makeScreen(): ContainerGUI {
    val gui = ContainerGUI()

    gui.image(Identifier("minecraft", "textures/gui/container/dispenser.png"), 0, 0, 0, 0).background()

    gui.string("Alchemy Decomposer", 80, 55, 0, "progress")

    return gui
}
```
As you can see, this is just an example of how you can do it, but you can make the layout as you wish, including adding different texts and anything you want that is available. Explore it and try different things.
In the makeSlots function, you should setup your slots, just like you would do in a ScreenHandler initialization phase. You can use the defaultPlayerSlots as well to create the player inventory slots, it is adapted to work on layouts like of the "textures/gui/container/dispenser.png". If you want different designs, you would need to implement that part for yourself.
The makeScreen part is the responsible for actually rendering the important things on your screen, it is client-side and you should build a container to handle your layouts and all. In this example, I created the background image of the dispenser default container gui and a string on the side of the screen.

After this, build the project and wait for the first error to come. It will say something like this:
```
... BlockAsContainer requires the inheritance of AbstractNameBlock
```
You should just make your bock to inherit its abstract version like you would do with the default Block inheritance.

After this, build the project again and you should be ready to go.

## More
You can also use states and special behaviors in your containers. This plugin make it possible for you to simply create two or three additional methods and handle everything well.

If you want to respond to the server-side tick events on your entity blocks, you can make a function in your gui called tick(): Boolean
And if you want to change your created screen container at runtime, you can make a function render(container: ContainerGUI): Unit in your GUI class and edit it here.

You can also have "shared state" between your client and server sides by using states. To create an state you just need to make a class (preferentially something like a data class for simplicity), inherit from UIState and add your properties, like this:
```kotlin
data class AlchemyDecomposerGUIState(
    var progress: Int = 0
): UIState
```
And add it to your GUI class as a property with the name 'state' (it should be a 'var' and you can initialize it with a default value).

The supported property types for now are:
 * Int
 * Long
 * Float
 * Double
 * Boolean
 * String
 * Identifier
 * Text
 * BlockPos
 * Date
 * UUID
They will not be saved in your block, but they can be used freshly every time you want and will sync between the client and server, so you can use them for making progress in crafting and everything you want.

To make the game sync your state, you should return true from your 'tick' function. If you return false, the game will assume nothing changed and will not send the data.

In my gui class, I made a simple example:
```kotlin
class AlchemyDecomposerGUI: GUI {
    var state: AlchemyDecomposerGUIState = AlchemyDecomposerGUIState()

    override fun makeSlots(playerInventory: PlayerInventory, inventory: Inventory): ContainerGUI {
        val gui = ContainerGUI()

        gui.slot(inventory, 0, 80, 35)

        gui.defaultPlayerSlots(playerInventory)

        return gui
    }

    override fun makeScreen(): ContainerGUI {
        val gui = ContainerGUI()

        gui.image(Identifier("minecraft", "textures/gui/container/dispenser.png"), 0, 0, 0, 0).background()

        gui.string("Alchemy Decomposer", 80, 55, 0, "progress")

        return gui
    }

    fun tick(): Boolean {
        state.progress += 1
        return true
    }

    fun render(container: ContainerGUI) {
        val text = container.find("progress") as? StringUI ?: return
        text.text = "Progress: ${state.progress}"
    }
}

data class AlchemyDecomposerGUIState(
    var progress: Int = 0
): UIState
```

## Finishing
Build your project again, and this time go on your mod initializer and put this:
```kotlin
NipahUIModInitializer.init()
```

And in your client mod initializer, this:
```kotlin
NipahUIModInitializer.initClient()
```

## Working
And in the game you can see it working:
![image](https://github.com/JoaoVictorVP/nipah-minecraft-ui/assets/98046863/62ef9667-8400-4ed5-9299-f2b1f83ac6db)
![image](https://github.com/JoaoVictorVP/nipah-minecraft-ui/assets/98046863/8c537e12-f45a-49f3-8bd7-8349517c16f7)
![image](https://github.com/JoaoVictorVP/nipah-minecraft-ui/assets/98046863/8851bf7b-416d-48b8-9854-c453a6ba0330)

It just works!
