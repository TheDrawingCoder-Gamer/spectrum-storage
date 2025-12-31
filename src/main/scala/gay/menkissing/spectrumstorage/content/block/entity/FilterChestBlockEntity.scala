package gay.menkissing.spectrumstorage.content.block.entity

import de.dafuqs.spectrum.api.block.FilterConfigurable
import de.dafuqs.spectrum.blocks.chests.SpectrumChestBlockEntity
import gay.menkissing.spectrumstorage.content.SpectrumStorageBlocks
import gay.menkissing.spectrumstorage.screen.FilterChestMenu
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.minecraft.core.{BlockPos, Direction, NonNullList}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.{SoundEvent, SoundEvents, SoundSource}
import net.minecraft.world.{ContainerHelper, WorldlyContainer}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BarrelBlock
import net.minecraft.world.level.block.entity.{BlockEntity, ContainerOpenersCounter, RandomizableContainerBlockEntity}
import net.minecraft.world.level.block.state.BlockState

import java.util
import java.util.stream.IntStream
import scala.annotation.nowarn

@nowarn("msg=unstable")
class FilterChestBlockEntity(pos: BlockPos, state: BlockState) extends RandomizableContainerBlockEntity(SpectrumStorageBlocks.filterChestBlockEntity, pos, state), WorldlyContainer, FilterConfigurable, ExtendedScreenHandlerFactory:
  val filterItems: NonNullList[ItemVariant] = NonNullList.withSize(FilterChestBlockEntity.filterCount, ItemVariant.blank())
  var items: NonNullList[ItemStack] = NonNullList.withSize(FilterChestBlockEntity.inventorySize, ItemStack.EMPTY)
  val openersCounter: ContainerOpenersCounter =
    new ContainerOpenersCounter:
      override def onOpen(level: Level, blockPos: BlockPos, blockState: BlockState): Unit =
        playSound(blockState, SoundEvents.BARREL_OPEN)
        updateBlockState(blockState, true)

      override def onClose(level: Level, blockPos: BlockPos, blockState: BlockState): Unit =
        playSound(blockState, SoundEvents.BARREL_CLOSE)
        updateBlockState(blockState, false)

      override def openerCountChanged(level: Level, blockPos: BlockPos, blockState: BlockState, i: Int, j: Int): Unit = ()

      override def isOwnContainer(player: Player): Boolean =
        player.containerMenu match
          case ce: FilterChestMenu =>
            ce.container eq FilterChestBlockEntity.this
          case _ => false

  private def incrementOpeners(player: Player): Unit =
    this.openersCounter.incrementOpeners(
      player,
      this.getLevel,
      this.getBlockPos,
      this.getBlockState
    )
  private def decrementOpeners(player: Player): Unit =
    this.openersCounter.decrementOpeners(
      player,
      this.getLevel,
      this.getBlockPos,
      this.getBlockState
    )

  def recheckOpen(): Unit =
    if !this.remove then
      openersCounter.recheckOpeners(this.getLevel, this.getBlockPos, this.getBlockState)

  def playSound(blockState: BlockState, soundEvent: SoundEvent): Unit =
    val vec3i = blockState.getValue(BarrelBlock.FACING).getNormal
    val d = this.worldPosition.getX.toDouble + 0.5F.toDouble + (vec3i.getX.toDouble / 2.0F.toDouble)
    val e = this.worldPosition.getY.toDouble + 0.5F.toDouble + (vec3i.getY.toDouble / 2.0F.toDouble)
    val f = this.worldPosition.getZ.toDouble + 0.5F.toDouble + (vec3i.getZ.toDouble / 2.0F.toDouble)
    this.level.playSound(
      null,
      d, e, f,
      soundEvent,
      SoundSource.BLOCKS,
      0.5F,
      this.level.random.nextFloat * 0.1F + 0.9F
    )

  def updateBlockState(state: BlockState, open: Boolean): Unit =
    this.level.setBlock(this.getBlockPos, state.setValue(BarrelBlock.OPEN, open), 3)


  override def getSlotsForFace(direction: Direction): Array[Int] =
    IntStream.rangeClosed(0, FilterChestBlockEntity.inventorySize - 1).toArray

  def acceptsItemStack(stack: ItemStack): Boolean =
    if stack.isEmpty then
      false
    else
      filterItems.stream().anyMatch(it => !it.isBlank && stack.is(it.getItem)) ||
        filterItems.stream().allMatch(_.isBlank)

  override def canPlaceItemThroughFace(i: Int, stack: ItemStack, direction: Direction): Boolean =
    acceptsItemStack(stack)

  override def canTakeItemThroughFace(i: Int, itemStack: ItemStack, direction: Direction): Boolean = true


  override def getDefaultName: Component = Component.translatable("container.spectrumstorage.filter_chest")

  override def getContainerSize: Int = FilterChestBlockEntity.inventorySize

  override def getItemFilters: util.List[ItemVariant] = filterItems

  override def setFilterItem(slot: Int, item: ItemVariant): Unit = filterItems.set(slot, item)

  override def saveAdditional(tag: CompoundTag): Unit =
    super.saveAdditional(tag)
    ContainerHelper.saveAllItems(tag, items)
    FilterConfigurable.writeFilterNbt(tag, filterItems)

  override def load(tag: CompoundTag): Unit =
    super.load(tag)
    ContainerHelper.loadAllItems(tag, items)
    FilterConfigurable.readFilterNbt(tag, filterItems)

  override def writeScreenOpeningData(player: ServerPlayer, buf: FriendlyByteBuf): Unit =
    buf.writeBlockPos(this.pos)
    FilterConfigurable.writeScreenOpeningData(buf, filterItems, 1, FilterChestBlockEntity.filterCount, FilterChestBlockEntity.filterCount)

  override def getItems: NonNullList[ItemStack] = items

  override def setItems(nonNullList: NonNullList[ItemStack]): Unit = items = nonNullList

  override def createMenu(i: Int, inventory: Inventory): AbstractContainerMenu =
    FilterChestMenu(i, inventory, this)

  override def startOpen(player: Player): Unit =
    super.startOpen(player)
    if !this.remove && !player.isSpectator then
      incrementOpeners(player)

  override def stopOpen(player: Player): Unit =
    super.stopOpen(player)
    if !this.remove && !player.isSpectator then
      decrementOpeners(player)

object FilterChestBlockEntity:
  val filterCount: Int = 18
  val inventorySize: Int = 9
