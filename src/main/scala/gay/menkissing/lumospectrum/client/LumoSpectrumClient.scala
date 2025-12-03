package gay.menkissing.lumospectrum.client

import gay.menkissing.lumospectrum.client.gui.ToolContainerGui
import gay.menkissing.lumospectrum.content.item.BottomlessBottleModelLoader
import gay.menkissing.lumospectrum.registries.LumoScreens
import net.fabricmc.api.{ClientModInitializer, EnvType, Environment}
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.client.gui.screens.MenuScreens

@Environment(EnvType.CLIENT)
object LumoSpectrumClient extends ClientModInitializer:
  override def onInitializeClient(): Unit =
    ModelLoadingPlugin.register(new BottomlessBottleModelLoader())
    MenuScreens.register(LumoScreens.toolContainer, (a, b, c) => ToolContainerGui(a, b, c))

