package gay.menkissing.spectrumstorage.registries

import gay.menkissing.spectrumstorage.content.SpectrumStorageItems
import gay.menkissing.spectrumstorage.util.SpectrumStorageNumberFormatting
import net.minecraft.network.chat.Component

object SpectrumStorageTranslationKeys:
  object keys:
    object bottomlessBottle:
      object tooltip:
        val usagePickup: String = SpectrumStorageItems.bottomlessBottle.get().getDescriptionId + ".tooltip.usage_pickup"
        val usagePlace: String = SpectrumStorageItems.bottomlessBottle.get().getDescriptionId + ".tooltip.usage_place"
        
        val empty: String = SpectrumStorageItems.bottomlessBottle.get().getDescriptionId + ".tooltip.empty"
        
        val countMB: String = SpectrumStorageItems.bottomlessBottle.get().getDescriptionId + ".tooltip.count_mb"
        
  
  object bottomlessBottle:
    object tooltip:
      val usagePickup: Component = Component.translatable(keys.bottomlessBottle.tooltip.usagePickup)
      val usagePlace: Component = Component.translatable(keys.bottomlessBottle.tooltip.usagePlace)
      
      val empty: Component = Component.translatable(keys.bottomlessBottle.tooltip.empty)
      def countMB(amount: Int, max: Int): Component =
        Component.translatable(keys.bottomlessBottle.tooltip.countMB, SpectrumStorageNumberFormatting.formatMB(amount), SpectrumStorageNumberFormatting.formatFluidMax(max))
        
