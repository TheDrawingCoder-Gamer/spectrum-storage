package gay.menkissing.lumospectrum.client

import gay.menkissing.lumospectrum.content.item.BottomlessBottleModelLoader
import net.fabricmc.api.{ClientModInitializer, EnvType, Environment}
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin

@Environment(EnvType.CLIENT)
object LumoSpectrumClient extends ClientModInitializer:
  override def onInitializeClient(): Unit =
    ModelLoadingPlugin.register(new BottomlessBottleModelLoader())

