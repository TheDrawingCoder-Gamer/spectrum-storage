package gay.menkissing.lumospectrum.registries

import de.dafuqs.spectrum.registries.SpectrumItems
import gay.menkissing.lumospectrum.util.registry.InfoCollector
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalItemTags
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.{ItemTags, TagKey}
import net.minecraft.world.item.{Item, Items}

object LumoTags:
  def commonTag(path: String): ResourceLocation =
    ResourceLocation("c", path)

  object item:
    private def tag(namespace: String, path: String): TagKey[Item] =
      TagKey.create(Registries.ITEM, ResourceLocation(namespace, path))
    private def commonTag(path: String): TagKey[Item] =
      TagKey.create(Registries.ITEM, LumoTags.commonTag(path))

    val validToolTag: TagKey[Item] =
      InfoCollector.instance.tag(Registries.ITEM, "valid_tools")
                   .addTag(ItemTags.TOOLS)
                   .add(Items.SPYGLASS)
                   .add(Items.CLOCK)
                   .add(SpectrumItems.TUNING_STAMP)
                   .add(SpectrumItems.OMNI_ACCELERATOR)
                   .add(SpectrumItems.CRESCENT_CLOCK)
                   .add(SpectrumItems.RADIANCE_STAFF)
                   .add(SpectrumItems.NATURES_STAFF)
                   .add(SpectrumItems.STAFF_OF_REMEMBRANCE)
                   .add(SpectrumItems.CONSTRUCTORS_STAFF)
                   .add(SpectrumItems.EXCHANGING_STAFF)
                   .add(SpectrumItems.BLOCK_FLOODER)
                   .add(SpectrumItems.CELESTIAL_POCKETWATCH)
                   .add(SpectrumItems.PAINTBRUSH)
                   .addOptional(ResourceLocation("botania", "twig_wand"))
                   .addOptional(ResourceLocation("botania", "dreamwood_wand"))
                   .addTag(ItemTags.COMPASSES)
                   .addTag(commonTag("wrenches"))
                   .addTag(commonTag("shears"))
                   .addTag(commonTag("bows"))
                   .addTag(commonTag("fishing_rods"))
                   .addTag(tag("botania", "rods"))
                   .addTag(tag("hexcasting", "staves"))

                   .lang("Valid Tool Container Tools")
                   .register()

  def init(): Unit =
    val _ = item
