package gay.menkissing.spectrumstorage.client

import gay.menkissing.spectrumstorage.client.gui.ToolContainerGui
import gay.menkissing.spectrumstorage.content.item.BottomlessBottleModelLoader
import gay.menkissing.spectrumstorage.registries.LumoScreens
import net.fabricmc.api.{ClientModInitializer, EnvType, Environment}
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.client.gui.screens.MenuScreens

@Environment(EnvType.CLIENT)
object SpectrumStorageClient extends ClientModInitializer:
  override def onInitializeClient(): Unit =
    ModelLoadingPlugin.register(new BottomlessBottleModelLoader())
    MenuScreens.register(LumoScreens.toolContainer, (a, b, c) => ToolContainerGui(a, b, c))

