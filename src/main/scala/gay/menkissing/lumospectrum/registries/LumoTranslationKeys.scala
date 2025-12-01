package gay.menkissing.lumospectrum.registries

import gay.menkissing.lumospectrum.content.LumoSpectrumItems
import gay.menkissing.lumospectrum.util.LumoNumberFormatting
import net.minecraft.network.chat.Component

object LumoTranslationKeys:
  object keys:
    object bottomlessBottle:
      object tooltip:
        val usagePickup: String = LumoSpectrumItems.bottomlessBottle.getDescriptionId + ".tooltip.usage_pickup"
        val usagePlace: String = LumoSpectrumItems.bottomlessBottle.getDescriptionId + ".tooltip.usage_place"
        
        val empty: String = LumoSpectrumItems.bottomlessBottle.getDescriptionId + ".tooltip.empty"
        
        val countMB: String = LumoSpectrumItems.bottomlessBottle.getDescriptionId + ".tooltip.count_mb"
        
  
  object bottomlessBottle:
    object tooltip:
      val usagePickup: Component = Component.translatable(keys.bottomlessBottle.tooltip.usagePickup)
      val usagePlace: Component = Component.translatable(keys.bottomlessBottle.tooltip.usagePlace)
      
      val empty: Component = Component.translatable(keys.bottomlessBottle.tooltip.empty)
      def countMB(amount: Long, max: Long): Component =
        Component.translatable(keys.bottomlessBottle.tooltip.countMB, LumoNumberFormatting.formatMB(amount), LumoNumberFormatting.formatFluidMax(max))
        
