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

package gay.menkissing.lumospectrum.util.registry.provider.generators

import com.google.common.collect.ArrayListMultimap
import gay.menkissing.lumospectrum.util.registry.InfoCollector
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.core.{HolderLookup, Registry}
import net.minecraft.resources.{ResourceKey, ResourceLocation}
import net.minecraft.tags.TagKey

import java.util.concurrent.CompletableFuture
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import annotation.{nowarn, unchecked}

class LumoTagsProvider[T](val owner: InfoCollector, registry: ResourceKey[Registry[T]], output: FabricDataOutput, lookup: CompletableFuture[HolderLookup.Provider])
  extends FabricTagProvider[T](output, registry, lookup):

  override def tag(tag: TagKey[T]): FabricTagProvider[T]#FabricTagBuilder = super.getOrCreateTagBuilder(tag)

  @nowarn("msg=type test")
  override def addTags(provider: HolderLookup.Provider): Unit =
    owner.tags.get(registryKey).forEach(_(this))

    val reg = provider.lookup(registryKey).get()

    val thangs = mutable.HashMap[T, ResourceKey[T]]()

    // done this way so i can SORT THAT THANG!!!
    owner.tagMembers(registryKey).asMap().forEach: (tag, blocks) =>
      val goodTag = tag.asInstanceOf[TagKey[T]]
      val builder = super.getOrCreateTagBuilder(goodTag)
      blocks.iterator().asScala.toList.sortBy {
        case key: TagKey[?] => "#" + key.location().toString
        case block: T => thangs.getOrElseUpdate(block, reg.listElements().filter(_.value() == block).findFirst().get().key()).location().toString
      }.foreach {
        case key: TagKey[T] => builder.addOptionalTag(key)
        case block: T => builder.add(block)
      }





