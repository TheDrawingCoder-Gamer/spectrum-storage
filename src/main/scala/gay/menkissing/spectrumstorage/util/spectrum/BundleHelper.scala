package gay.menkissing.spectrumstorage.util.spectrum

import de.dafuqs.spectrum.blocks.bottomless_bundle.{BottomlessComponent, BottomlessItemHandler}
import de.dafuqs.spectrum.registries.SpectrumDataComponentTypes
import gay.menkissing.spectrumstorage.util.ItemResource
import net.minecraft.core.HolderLookup
import net.minecraft.world.item.ItemStack

object BundleHelper:
  final class BundleStorageBuilder(var template: ItemResource, var count: Long, var max: Long):
    def makeStack(count: Int): ItemStack =
      template.makeStack(count)

    def stackSize: Int = template.item.getMaxStackSize(makeStack(1))

    def isEmpty: Boolean = template.isBlank || count == 0

    def save(stack: ItemStack): Unit =
      val current = stack.getOrDefault(SpectrumDataComponentTypes.BOTTOMLESS_STACK, BottomlessComponent.DEFAULT)
      val handler =
        if template.isBlank || count == 0L then
          new BottomlessItemHandler(max, current.handler().deletesOverflow(), current.handler().locked(), ItemStack.EMPTY, 0)
        else
          new BottomlessItemHandler(max, current.handler().deletesOverflow(), current.handler().locked(), template.makeStack(1), count)
      stack.set(SpectrumDataComponentTypes.BOTTOMLESS_STACK, BottomlessComponent(handler))

    def increment(n: Long): Unit =
      this.count = math.min(this.max, this.count + n)

    def insert(variant: ItemResource, count: Int): Int =
      if this.isEmpty then
        template = variant

      if !this.template.permits(variant) then
        0
      else
        val old = this.count
        this.increment(count.toLong)
        (this.count - old).toInt

    // Contract: N -> 0 <= x <= N
    def extract(extractN: Long): Long =
      if extractN != 0 && this.count != 0 then
        val decrement = math.min(this.count, extractN)
        this.increment(-decrement)
        decrement
      else
        0







  object BundleStorageBuilder:
    def fromStackWithAccess(stack: ItemStack, lookup: HolderLookup.Provider): BundleStorageBuilder =
      val component = BottomlessComponent.get(stack, lookup, true)
      BundleStorageBuilder(ItemResource.ofStack(component.handler().variant), component.handler().count(), component.handler().capacity())

