package gay.menkissing.spectrumstorage.content.block.entity

import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.block.BottomlessShelfBlock
import gay.menkissing.spectrumstorage.content.{SpectrumStorageBlocks, SpectrumStorageItems}
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent

import java.util.Objects

class BottomlessShelfBlockEntity(pos: BlockPos, state: BlockState) extends BottomlessStorageBlockEntity(6, SpectrumStorageBlocks.bottomlessShelfBlockEntity, pos, state):

  override def updateSlotShown(slot: Int): Unit =

    if slot >= 0 && slot < capacity then
      this.lastInteractedSlot = slot
      var blockState = this.getBlockState

      BottomlessShelfBlock.SHELF_SLOT_OCCUPIED_PROPS.zipWithIndex.foreach: (prop, i) =>
        val kind =
          val item = items.get(i)
          if item.isEmpty then
            BottomlessShelfBlock.ShelfSlotOccupiedBy.Empty
          else if item.is(SpectrumStorageItems.bottomlessBottle) then
            BottomlessShelfBlock.ShelfSlotOccupiedBy.Bottle
          else
            BottomlessShelfBlock.ShelfSlotOccupiedBy.Bundle
        blockState = blockState.setValue(prop, kind)
      Objects.requireNonNull(this.level).setBlock(this.worldPosition, blockState, 3)
      this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.worldPosition, GameEvent.Context.of(blockState))
    else
      SpectrumStorage.Logger.error("Expected slot to be 0-5, got {}", slot)

object BottomlessShelfBlockEntity:
  def registerStorages(): Unit =
    FluidStorage.SIDED.registerForBlockEntity[BottomlessStorageBlockEntity]((cooler, dir) => {
      cooler.fluidStorage
    }, SpectrumStorageBlocks.bottomlessShelfBlockEntity)
    ItemStorage.SIDED.registerForBlockEntity[BottomlessStorageBlockEntity]((cooler, dir) => {
      cooler.itemStorage
    }, SpectrumStorageBlocks.bottomlessShelfBlockEntity)