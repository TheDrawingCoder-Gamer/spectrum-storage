package gay.menkissing.lumospectrum

import gay.menkissing.lumospectrum.content.{LumoSpectrumBlocks, LumoSpectrumItems}
import gay.menkissing.lumospectrum.registries.{LumoLang, LumoScreens, LumoTags}
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.ResourceLocation
import org.slf4j.{Logger, LoggerFactory}

object LumoSpectrum extends ModInitializer:
  val ModId: String = "lumospectrum"
  val Logger: Logger = LoggerFactory.getLogger("lumospectrum")

  def locate(id: String): ResourceLocation = new ResourceLocation(ModId, id)

  override def onInitialize(): Unit =
    LumoSpectrumItems.init()
    LumoSpectrumBlocks.init()
    LumoScreens.init()
    LumoTags.init()
    LumoLang.init()
