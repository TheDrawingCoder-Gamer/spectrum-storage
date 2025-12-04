/*
 * Copyright (c) BulbyVR/TheDrawingCoder-Gamer 2025.
 *
 * This file is part of Lumomancy.
 *
 * Lumomancy is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the license, or (at your option) any later version.
 *
 * Lumomancy is distributed in the hopes it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License along with Lumomancy. If not,
 *  see <https://www.gnu.org/licenses/>
 */

package gay.menkissing.spectrumstorage.util.registry

import com.google.common.collect.{ArrayListMultimap, HashMultimap, Multimap}
import gay.menkissing.spectrumstorage.util.registry.builder.{BlockBuilder, ItemBuilder, TagBuilder}
import gay.menkissing.spectrumstorage.util.registry.provider.generators.{LumoBlockStateGenerator, LumoItemModelProvider, LumoModelProvider, LumoTagsProvider}
import LumoTagsProvider.ChildKind
import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.util.registry.helper.{GuidebookLangHelper, LangHelper}
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.provider.{FabricBlockLootTableProvider, FabricLanguageProvider, FabricTagProvider}
import net.minecraft.client.model.Model
import net.minecraft.core.{HolderLookup, Registry}
import net.minecraft.data.models.blockstates.BlockStateGenerator
import net.minecraft.data.models.{BlockModelGenerators, ItemModelGenerators}
import net.minecraft.data.{CachedOutput, DataProvider}
import net.minecraft.resources.{ResourceKey, ResourceLocation}
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.LootTable

import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.*
import scala.collection.mutable

/**
 * collects info for datagen
 * sort of inspired by registrate but registrate had too many things when I like just registering that thang
 */
class InfoCollector(val modid: String):
  import LumoTagsProvider.ChildKind
  private val lang = mutable.HashMap[String, String]()

  private val blockLootTables = mutable.HashMap[Block, FabricBlockLootTableProvider => LootTable.Builder]()

  private val itemModels = mutable.HashMap[Item, LumoItemModelProvider => Item => Unit]()
  
  private val blockStates = mutable.HashMap[Block, LumoBlockStateGenerator => Block => Unit]()

  private[registry] val tags = ArrayListMultimap.create[ResourceKey[? <: Registry[?]], LumoTagsProvider[?] => Unit]()

  private[registry] val tagMembers = mutable.HashMap[ResourceKey[? <: Registry[?]], Multimap[TagKey[?], ChildKind[?]]]()

  private lazy val doDatagen = System.getProperty("fabric-api.datagen") != null

  def addToTagDirect[T](registry: ResourceKey[? <: Registry[T]], tag: TagKey[T], block: ChildKind[T]): Unit =
    if doDatagen then
      tagMembers.getOrElseUpdate(registry, ArrayListMultimap.create()).put(tag, block)

  def addToTag[T](registry: ResourceKey[? <: Registry[T]], tag: TagKey[T], block: T): Unit =
    addToTagDirect(registry, tag, ChildKind.Direct(block))

  def addTagToTag[T](registry: ResourceKey[? <: Registry[T]], tag: TagKey[T], subtag: TagKey[T]): Unit =
    addToTagDirect(registry, tag, ChildKind.Tag(subtag))
  
  def addOptionalToTag[T](registry: ResourceKey[? <: Registry[T]], tag: TagKey[T], loc: ResourceLocation): Unit =
    addToTagDirect(registry, tag, ChildKind.Optional(loc))

  def setBlockState(block: Block, func: LumoBlockStateGenerator => Block => Unit): Unit =
    if doDatagen then
      blockStates(block) = func

  def setItemModel(item: Item, func: LumoItemModelProvider => Item => Unit): Unit =
    if doDatagen then
      itemModels(item) = func

  def addRawLang(key: String, value: String): InfoCollector =
    if doDatagen then
      lang(key) = value
    this
  
  def bulkAddLangs(idGetter: String => String): LangHelper =
    LangHelper(this, idGetter)
  def addGuidebookEntry(entryName: String): GuidebookLangHelper =
    GuidebookLangHelper(this, entryName)

  def addBlockLootTable(block: Block, table: FabricBlockLootTableProvider => LootTable.Builder): InfoCollector =
    if doDatagen then
      blockLootTables(block) = table
    this

  def block(name: ResourceLocation, block: Block): BlockBuilder[Unit] =
    BlockBuilder(this, (), block, name)

  def block(name: String, block: Block): BlockBuilder[Unit] =
    this.block(ResourceLocation(modid, name), block)

  def item(name: ResourceLocation, item: Item): ItemBuilder[Unit] =
    ItemBuilder(this, (), item, name)

  def item(name: String, item: Item): ItemBuilder[Unit] =
    this.item(ResourceLocation(modid, name), item)

  def tag[T](registry: ResourceKey[? <: Registry[T]], name: ResourceLocation): TagBuilder[T, Unit] =
    TagBuilder(this, (), registry, TagKey.create(registry, name))
  
  def tag[T](registry: ResourceKey[? <: Registry[T]], name: String): TagBuilder[T, Unit] =
    this.tag(registry, ResourceLocation(modid, name))
    
  def appendTag[T](key: TagKey[T]): TagBuilder[T, Unit] =
    TagBuilder(this, (), key.registry(), key)
  
  def registerDataGenerators(pack: FabricDataGenerator#Pack): Unit =
    pack.addProvider { (output, lookup) =>
      val langProvider = new FabricLanguageProvider(output, "en_us") {
        override def generateTranslations(translationBuilder: FabricLanguageProvider.TranslationBuilder): Unit =
          lang.foreach { (k, v) =>
            translationBuilder.add(k, v)
          }
      }
      val blockLootProvider =
        new FabricBlockLootTableProvider(output):
          override def generate(): Unit =
            blockLootTables.foreach { (block, gen) =>
              this.add(block, gen(this))
            }
      val itemModelProvider = new LumoItemModelProvider(output) with DataProvider:
        def run(cache: CachedOutput): CompletableFuture[?] =
          itemModels.foreach { (k, v) =>
            v(this)(k)
          }
          this.generateAll(cache)
          
        override def getName: String = s"Item Model Provider for $modid"
      
      
      
      val blockModelProvider = new LumoBlockStateGenerator(output):
        override def registerStates(): Unit =
          InfoCollector.this.blockStates.foreach { (k, v) =>
            v(this)(k)
          }
      val tagsProvider =
        (tags.keySet().asScala ++ tagMembers.keySet).map { registry =>
          // god made
          LumoTagsProvider[Any](this, registry.asInstanceOf[ResourceKey[Registry[Any]]], output, lookup)
        }

      // registered like this so that the normal ones could also
      // be used along side this
      new DataProvider {
        override def run(output: CachedOutput): CompletableFuture[?] =
          val f150: CompletableFuture[?] = CompletableFuture.allOf(
            tagsProvider.map(_.run(output)).toSeq*
          )
          CompletableFuture.allOf(
              langProvider.run(output),
              blockLootProvider.run(output),
              itemModelProvider.run(output),
              blockModelProvider.run(output),
              f150
          )

        override def getName: String = s"InfoCollector-based provider for $modid"
      }
    }



object InfoCollector:
  val instance: InfoCollector = InfoCollector(SpectrumStorage.ModId)

