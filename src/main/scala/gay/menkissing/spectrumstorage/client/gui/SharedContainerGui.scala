package gay.menkissing.spectrumstorage.client.gui

import de.dafuqs.spectrum.inventories.ScreenBackgroundVariant
import gay.menkissing.spectrumstorage.screen.ToolContainerMenu
import net.fabricmc.api.{EnvType, Environment}
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

@Environment(EnvType.CLIENT)
class SharedContainerGui[T <: AbstractContainerMenu](val rows: Int, val tier: ScreenBackgroundVariant, menu: T, inventory: Inventory, component: Component)
  extends AbstractContainerScreen[T](menu, inventory, component):
  this.imageHeight = 114 + rows * 18
  this.inventoryLabelY = this.imageHeight - 94
  private val backgroundTexture = getBackground(tier)

  override def render(gui: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit =
    this.renderBackground(gui)
    super.render(gui, mouseX, mouseY, partialTicks)
    this.renderTooltip(gui, mouseX, mouseY)

  override protected def renderBg(guiGraphics: GuiGraphics, f: Float, i: Int, j: Int): Unit =
    val k: Int = (this.width - this.imageWidth) / 2
    val l: Int = (this.height - this.imageHeight) / 2
    guiGraphics.blit(backgroundTexture, k, l, 0, 0, this.imageWidth, rows * 18 + 17)
    guiGraphics.blit(backgroundTexture, k, l + rows * 18 + 17, 0, 126, this.imageWidth, 96)
    
  private def getBackground(tier: ScreenBackgroundVariant): ResourceLocation =
    tier match
      case ScreenBackgroundVariant.EARLYGAME => SharedContainerGui.tier1
      case ScreenBackgroundVariant.MIDGAME => SharedContainerGui.tier2
      case ScreenBackgroundVariant.LATEGAME => SharedContainerGui.tier3

object SharedContainerGui:
  val tier1 = ResourceLocation("spectrum", "textures/gui/container/generic_54_tier_1.png")
  val tier2 = ResourceLocation("spectrum", "textures/gui/container/generic_54_tier_2.png")
  val tier3 = ResourceLocation("spectrum", "textures/gui/container/generic_54_tier_3.png")