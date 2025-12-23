package gay.menkissing.spectrumstorage.datagen

import com.klikli_dev.modonomicon.api.datagen.book.{BookCategoryModel, BookModel}
import gay.menkissing.spectrumstorage.content.block.BottomlessShelfBlock
import gay.menkissing.spectrumstorage.content.{SpectrumStorageBlocks, SpectrumStorageItems}
import gay.menkissing.spectrumstorage.util.registry.InfoCollector
import net.fabricmc.fabric.api.datagen.v1.provider.{FabricCodecDataProvider, FabricModelProvider}
import net.fabricmc.fabric.api.datagen.v1.{DataGeneratorEntrypoint, FabricDataGenerator, FabricDataOutput}
import net.minecraft.core.Direction
import net.minecraft.data.models.blockstates.{Condition, MultiPartGenerator, Variant, VariantProperties}
import net.minecraft.data.models.{BlockModelGenerators, ItemModelGenerators}
import net.minecraft.data.models.model.{ModelLocationUtils, ModelTemplate, ModelTemplates, TextureMapping, TextureSlot}
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.properties.{BlockStateProperties, EnumProperty}
import com.klikli_dev.modonomicon.api.datagen.{BookProvider, LanguageProviderCache}
import gay.menkissing.spectrumstorage.SpectrumStorage

import scala.collection.mutable


object LumoDatagen extends DataGeneratorEntrypoint:
  override def onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator): Unit =
    val pack = fabricDataGenerator.createPack()

    pack.addProvider((o: FabricDataOutput) => ModelGenerator(o))

    InfoCollector.instance.registerDataGenerators(pack)


  private class ModelGenerator(output: FabricDataOutput) extends FabricModelProvider(output):

    override def generateItemModels(itemModelGenerator: ItemModelGenerators): Unit =
      ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(SpectrumStorageItems.bottomlessBottle, "_base"), TextureMapping.layer0(SpectrumStorageItems.bottomlessBottle), itemModelGenerator.output)

    override def generateBlockStateModels(blockStateModelGenerator: BlockModelGenerators): Unit =
      ()

