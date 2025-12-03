package gay.menkissing.lumospectrum

import gay.menkissing.lumospectrum.content.{LumoSpectrumBlocks, LumoSpectrumItems}
import gay.menkissing.lumospectrum.registries.{LumoScreens, LumoTags}
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.{Logger, LoggerFactory}

object LumoSpectrum extends ModInitializer:
  val MODID: String = "lumospectrum"
  val LOGGER: Logger = LoggerFactory.getLogger("lumospectrum")

  def locate(id: String): ResourceLocation = new ResourceLocation(MODID, id)

  override def onInitialize(): Unit =
    LumoSpectrumItems.init()
    LumoSpectrumBlocks.init()
    LumoScreens.init()
    LumoTags.init()
