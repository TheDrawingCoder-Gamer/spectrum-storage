package gay.menkissing.spectrumstorage.util

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.{BuiltInRegistries, Registries}
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.{ByteBufCodecs, StreamCodec}
import net.minecraft.world.item.{Item, ItemStack, Items}

import java.util.Objects

final class ItemResource private (val item: Item, val components: DataComponentPatch):
  override val hashCode: Int = Objects.hash(item, components)

  def isBlank: Boolean =
    item == Items.AIR

  def componentsMatch(other: DataComponentPatch): Boolean =
    this.components == other

  def makeStack(size: Int): ItemStack =
    if size == 0 then
      ItemStack.EMPTY
    else
      ItemStack(item.builtInRegistryHolder(), size, components)

  def permits(that: ItemResource): Boolean =
    if this.isBlank then
      that.isBlank
    else
      this.item == that.item && this.componentsMatch(that.components)

  def sameAsStack(stack: ItemStack): Boolean =
    if !stack.is(item) then
      false
    else if this.isBlank && stack.isEmpty then
      true
    else
      this.componentsMatch(stack.getComponentsPatch)

  override def equals(obj: Any): Boolean =
    if (this eq obj.asInstanceOf[Object]) true
    else if (obj == null && getClass != obj.getClass) false
    else
      val that = obj.asInstanceOf[ItemResource]
      hashCode == that.hashCode && item == that.item && componentsMatch(that.components)

object ItemResource:
  val CODEC: Codec[ItemResource] =
    RecordCodecBuilder.create: builder =>
      builder.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter((it: ItemResource) => it.item),
        DataComponentPatch.CODEC.fieldOf("components").forGetter((it: ItemResource) => it.components)
      ).apply(builder, ItemResource.apply)

  val STREAM_CODEC: StreamCodec[RegistryFriendlyByteBuf, ItemResource] =
    StreamCodec.composite(
      ByteBufCodecs.registry(Registries.ITEM), (it: ItemResource) => it.item,
      DataComponentPatch.STREAM_CODEC,(it: ItemResource) => it.components,
      ItemResource.apply
    )

  val EMPTY = ItemResource(Items.AIR, DataComponentPatch.EMPTY)

  def ofStack(stack: ItemStack): ItemResource =
    Objects.requireNonNull(stack, "Item stack may not be null")

    ItemResource(stack.getItem, stack.getComponentsPatch)

  def of(item: Item): ItemResource =
    of(item, DataComponentPatch.EMPTY)

  def of(item: Item, patch: DataComponentPatch): ItemResource =
    Objects.requireNonNull(item, "Item may not be null")
    Objects.requireNonNull(patch, "Components may not be null")

    ItemResource(item, patch)