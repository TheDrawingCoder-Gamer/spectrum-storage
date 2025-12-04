package gay.menkissing.spectrumstorage.registries

import gay.menkissing.spectrumstorage.content.SpectrumStorageItems
import gay.menkissing.spectrumstorage.util.LumoNumberFormatting
import net.minecraft.network.chat.Component

object LumoTranslationKeys:
  object keys:
    object bottomlessBottle:
      object tooltip:
        val usagePickup: String = SpectrumStorageItems.bottomlessBottle.getDescriptionId + ".tooltip.usage_pickup"
        val usagePlace: String = SpectrumStorageItems.bottomlessBottle.getDescriptionId + ".tooltip.usage_place"
        
        val empty: String = SpectrumStorageItems.bottomlessBottle.getDescriptionId + ".tooltip.empty"
        
        val countMB: String = SpectrumStorageItems.bottomlessBottle.getDescriptionId + ".tooltip.count_mb"
        
  
  object bottomlessBottle:
    object tooltip:
      val usagePickup: Component = Component.translatable(keys.bottomlessBottle.tooltip.usagePickup)
      val usagePlace: Component = Component.translatable(keys.bottomlessBottle.tooltip.usagePlace)
      
      val empty: Component = Component.translatable(keys.bottomlessBottle.tooltip.empty)
      def countMB(amount: Long, max: Long): Component =
        Component.translatable(keys.bottomlessBottle.tooltip.countMB, LumoNumberFormatting.formatMB(amount), LumoNumberFormatting.formatFluidMax(max))
        
