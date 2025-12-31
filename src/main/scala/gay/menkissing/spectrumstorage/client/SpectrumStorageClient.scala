package gay.menkissing.spectrumstorage.client

import gay.menkissing.spectrumstorage.client.gui.{FilterChestGui, SharedContainerGui, ToolContainerGui}
import gay.menkissing.spectrumstorage.content.item.BottomlessBottleModelLoader
import gay.menkissing.spectrumstorage.registries.LumoScreens
import gay.menkissing.spectrumstorage.screen.BottomlessStorageMenu
import net.fabricmc.api.{ClientModInitializer, EnvType, Environment}
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.client.gui.screens.MenuScreens

@Environment(EnvType.CLIENT)
object SpectrumStorageClient extends ClientModInitializer:
  override def onInitializeClient(): Unit =
    ModelLoadingPlugin.register(new BottomlessBottleModelLoader())
    MenuScreens.register(LumoScreens.toolContainer, (a, b, c) => ToolContainerGui(a, b, c))
    MenuScreens.register(LumoScreens.bottomlessBarrel, (a, b, c) => SharedContainerGui[BottomlessStorageMenu](BottomlessStorageMenu.barrelRows, a, b, c))
    MenuScreens.register(LumoScreens.bottomlessAmphora, (a, b, c) => SharedContainerGui[BottomlessStorageMenu](BottomlessStorageMenu.amphoraRows, a, b, c))
    MenuScreens.register(LumoScreens.filterChest, (a, b, c) => FilterChestGui(a, b, c))

