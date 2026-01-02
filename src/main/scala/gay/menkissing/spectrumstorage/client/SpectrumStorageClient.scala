package gay.menkissing.spectrumstorage.client

import de.dafuqs.spectrum.inventories.ScreenBackgroundVariant
import gay.menkissing.spectrumstorage.client.gui.{FilterChestGui, SharedContainerGui, ToolContainerGui}
import gay.menkissing.spectrumstorage.content.SpectrumStorageBlocks
import gay.menkissing.spectrumstorage.content.item.BottomlessBottleModelLoader
import gay.menkissing.spectrumstorage.registries.LumoScreens
import gay.menkissing.spectrumstorage.screen.BottomlessStorageMenu
import net.fabricmc.api.{ClientModInitializer, EnvType, Environment}
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.client.renderer.RenderType

@Environment(EnvType.CLIENT)
object SpectrumStorageClient extends ClientModInitializer:
  override def onInitializeClient(): Unit =
    ModelLoadingPlugin.register(new BottomlessBottleModelLoader())
    MenuScreens.register(LumoScreens.toolContainer, (a, b, c) => ToolContainerGui(a, b, c))
    MenuScreens.register(LumoScreens.bottomlessBarrel, (a, b, c) => SharedContainerGui[BottomlessStorageMenu](BottomlessStorageMenu.barrelRows, ScreenBackgroundVariant.EARLYGAME, a, b, c))
    MenuScreens.register(LumoScreens.bottomlessAmphora, (a, b, c) => SharedContainerGui[BottomlessStorageMenu](BottomlessStorageMenu.amphoraRows, ScreenBackgroundVariant.EARLYGAME, a, b, c))
    MenuScreens.register(LumoScreens.filterChest, (a, b, c) => FilterChestGui(a, b, c))
    BlockRenderLayerMap.INSTANCE.putBlock(SpectrumStorageBlocks.filterChest, RenderType.cutout())

