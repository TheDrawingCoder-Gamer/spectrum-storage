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

package gay.menkissing.lumospectrum.util.registry.builder

import com.google.common.collect.HashMultimap
import gay.menkissing.lumospectrum.util.registry.InfoCollector
import gay.menkissing.lumospectrum.util.registry.provider.generators.LumoTagsProvider.ChildKind
import net.minecraft.core.Registry
import net.minecraft.resources.{ResourceKey, ResourceLocation}
import net.minecraft.tags.TagKey

import scala.collection.mutable

class TagBuilder[T, P](val owner: InfoCollector, val parent: P, val registryKey: ResourceKey[? <: Registry[T]], val tag: TagKey[T]) extends Builder[TagKey[T], P]:
  private val childItems = mutable.LinkedHashSet[ChildKind[T]]()

  def add(child: T): this.type =
    childItems.add(ChildKind.Direct(child))
    this
  def addOptional(child: ResourceLocation): this.type =
    childItems.add(ChildKind.Optional(child))
    this

  def addTag(daTag: TagKey[T]): this.type =
    childItems.add(ChildKind.Tag(daTag))
    this
    
  def tag(parent: TagKey[T]): this.type =
    // parent
    tag(registryKey, parent)
  
  def lang(value: String): this.type =
    lang(it => TagBuilder.getLang(it), value)
  override protected def registered(): TagKey[T] =
    childItems.foreach: child =>
      owner.addToTagDirect(registryKey, tag, child)
    tag

object TagBuilder:
  def getLang(tag: TagKey[?]): String =
    val loc = tag.location()
    s"tag.${loc.getNamespace}.${loc.getPath.replace("/", ".")}"
    