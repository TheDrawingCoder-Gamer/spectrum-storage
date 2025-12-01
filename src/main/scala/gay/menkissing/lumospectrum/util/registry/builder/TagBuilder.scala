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
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey

import scala.collection.mutable

class TagBuilder[T, P](val owner: InfoCollector, val parent: P, val registryKey: ResourceKey[? <: Registry[T]], val tag: TagKey[T]) extends Builder[TagKey[T], P]:
  private val childItems = mutable.LinkedHashSet[T]()
  private val childTags = mutable.LinkedHashSet[TagKey[T]]()

  def add(child: T): this.type =
    childItems.add(child)
    this


  def addTag(daTag: TagKey[T]): this.type =
    childTags.add(daTag)
    this
    
  def tag(parent: TagKey[T]): this.type =
    // parent
    tag(registryKey, parent)

  override protected def registered(): TagKey[T] =
    childTags.foreach { child =>
      owner.addTagToTag(registryKey, tag, child)
    }
    childItems.foreach { child =>
      owner.addToTag(registryKey, tag, child)
    }
    tag
