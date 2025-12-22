package gay.menkissing.spectrumstorage.client.gui

import gay.menkissing.spectrumstorage.screen.ToolContainerMenu
import gay.menkissing.spectrumstorage.util.resources.ResourceLocationExt
import net.fabricmc.api.{EnvType, Environment}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

@Environment(EnvType.CLIENT)
object ToolContainerGui:
  def apply(menu: ToolContainerMenu, inventory: Inventory, component: Component): SharedContainerGui[ToolContainerMenu] =
    new SharedContainerGui[ToolContainerMenu](ToolContainerMenu.rows, menu, inventory, component)
