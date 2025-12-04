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

package gay.menkissing.spectrumstorage.util.registry.builder

import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.util.registry.InfoCollector
import gay.menkissing.spectrumstorage.util.registry.provider.generators.LumoBlockStateGenerator
import gay.menkissing.spectrumstorage.util.resources.{*, given}
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider
import net.minecraft.core.Registry
import net.minecraft.core.registries.{BuiltInRegistries, Registries}
import net.minecraft.data.models.BlockModelGenerators
import net.minecraft.data.models.blockstates.BlockStateGenerator
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.{BlockItem, Item}
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.LootTable

class BlockBuilder[P](val owner: InfoCollector, val parent: P, val block: Block, val rl: ResourceLocation) extends Builder[Block, P]:
  private var daItem: Option[Item] = None

  def item(props: Item.Properties = Item.Properties()): ItemBuilder[this.type] =
    val item = BlockItem(block, props)
    daItem = Some(item)
    ItemBuilder[this.type](owner, this, item, rl)

  def blockItem(props: Item.Properties = Item.Properties()): ItemBuilder[this.type] =
    item(props)
      .model(gen => item =>
        gen.withExistingParent(item, block.modelLoc)
      )
  
  def simpleItem(props: Item.Properties = Item.Properties()): this.type =
    blockItem(props)
      .build()

  def blockstate(func: LumoBlockStateGenerator => Block => Unit): this.type =
    owner.setBlockState(block, func)
    this

  def defaultBlockstate(): this.type =
    blockstate(gen => block => gen.simpleBlock(block))
  
  override protected def registered(): Block =
    Registry.register(BuiltInRegistries.BLOCK, rl, block)
    block
    
  def lang(value: String): this.type =
    lang((block: Block) => block.getDescriptionId, value)
    this
    
  def lootTable(table: FabricBlockLootTableProvider => LootTable.Builder): this.type =
    owner.addBlockLootTable(block, table)
    this
    
  def dropOther(item: ItemLike): this.type =
    lootTable(_.createSingleItemTable(item))
    this
    
  def dropSelf(): this.type =
    lootTable(_.createSingleItemTable(block))
    this

  def tag(tag: TagKey[Block]): this.type =
    this.tag(Registries.BLOCK, tag)