package gay.menkissing.spectrumstorage.client.gui

import gay.menkissing.spectrumstorage.screen.ToolContainerMenu
import gay.menkissing.spectrumstorage.util.resources.ResourceLocationExt
import net.fabricmc.api.{EnvType, Environment}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

@Environment(EnvType.CLIENT)
class SharedContainerGui[T <: AbstractContainerMenu](val rows: Int, menu: T, inventory: Inventory, component: Component)
  extends AbstractContainerScreen[T](menu, inventory, component):
  this.imageHeight = 114 + rows * 18
  this.inventoryLabelY = this.imageHeight - 94

  override def render(gui: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit =
    this.renderBackground(gui)
    super.render(gui, mouseX, mouseY, partialTicks)
    this.renderTooltip(gui, mouseX, mouseY)

  override protected def renderBg(guiGraphics: GuiGraphics, f: Float, i: Int, j: Int): Unit =
    val k: Int = (this.width - this.imageWidth) / 2
    val l: Int = (this.height - this.imageHeight) / 2
    guiGraphics.blit(SharedContainerGui.texture, k, l, 0, 0, this.imageWidth, rows * 18 + 17)
    guiGraphics.blit(SharedContainerGui.texture, k, l + rows * 18 + 17, 0, 126, this.imageWidth, 96)

object SharedContainerGui:
  val texture = ResourceLocationExt.withDefaultNamespace("textures/gui/container/generic_54.png")