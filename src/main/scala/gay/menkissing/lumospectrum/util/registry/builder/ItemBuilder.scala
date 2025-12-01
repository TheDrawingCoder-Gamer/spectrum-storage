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

import gay.menkissing.lumospectrum.util.registry.InfoCollector
import gay.menkissing.lumospectrum.util.registry.provider.generators.{LumoItemModelProvider, LumoModelProvider}
import net.minecraft.core.Registry
import net.minecraft.core.registries.{BuiltInRegistries, Registries}
import net.minecraft.data.models.ItemModelGenerators
import net.minecraft.data.models.model.{ModelTemplate, ModelTemplates}
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item

class ItemBuilder[P](val owner: InfoCollector, val parent: P, val item: Item, val rl: ResourceLocation) extends Builder[Item, P]:
  override protected def registered(): Item =
    Registry.register(BuiltInRegistries.ITEM, rl, item)

  def lang(value: String): this.type =
    lang(_.getDescriptionId, value)

  def tooltip(sub: String, value: String): this.type =
    lang(it =>
      it.getDescriptionId + ".tooltip." + sub,
      value
    )

  def model(func: LumoItemModelProvider => Item => Unit): this.type =
    owner.setItemModel(item, func)
    this

  def tag(tag: TagKey[Item]): this.type =
    this.tag(Registries.ITEM, tag)
  
  def defaultModel(): this.type =
    model(gen => gen.generated)
