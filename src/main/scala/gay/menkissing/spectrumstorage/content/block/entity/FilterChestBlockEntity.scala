package gay.menkissing.spectrumstorage.content.block.entity

import de.dafuqs.spectrum.api.block.FilterConfigurable
import gay.menkissing.spectrumstorage.content.SpectrumStorageBlocks
import gay.menkissing.spectrumstorage.screen.FilterChestMenu
import gay.menkissing.spectrumstorage.util.network.SpectrumStorageNetworking.GayScreenHandler
import net.minecraft.core.{BlockPos, Direction, HolderLookup, NonNullList}
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.{FriendlyByteBuf, RegistryFriendlyByteBuf}
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.{SoundEvent, SoundEvents, SoundSource}
import net.minecraft.world.{ContainerHelper, WorldlyContainer}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BarrelBlock
import net.minecraft.world.level.block.entity.{ContainerOpenersCounter, RandomizableContainerBlockEntity}
import net.minecraft.world.level.block.state.BlockState

import java.util
import java.util.stream.IntStream
import scala.annotation.nowarn

@nowarn("msg=unstable")
class FilterChestBlockEntity(pos: BlockPos, state: BlockState) extends RandomizableContainerBlockEntity(SpectrumStorageBlocks.filterChestBlockEntity.get(), pos, state), WorldlyContainer, FilterConfigurable, GayScreenHandler[FilterConfigurable.ExtendedDataWithPos](FilterConfigurable.ExtendedDataWithPos.PACKET_CODEC):
  val filterItems: NonNullList[ItemStack] = NonNullList.withSize(FilterChestBlockEntity.filterCount, ItemStack.EMPTY)
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
      acceptsItem(stack.getItem)

  override def canPlaceItemThroughFace(i: Int, stack: ItemStack, direction: Direction): Boolean =
    acceptsItemStack(stack)

  override def canTakeItemThroughFace(i: Int, itemStack: ItemStack, direction: Direction): Boolean = true


  override def getDefaultName: Component = Component.translatable("container.spectrumstorage.filter_chest")

  override def getContainerSize: Int = FilterChestBlockEntity.inventorySize

  override def getItemFilters: util.List[ItemStack] = filterItems

  override def setFilterItem(slot: Int, item: ItemStack): Unit =
    setChanged()
    filterItems.set(slot, item)

  override def saveAdditional(tag: CompoundTag, provider: HolderLookup.Provider): Unit =
    super.saveAdditional(tag, provider)
    ContainerHelper.saveAllItems(tag, items, provider)
    FilterConfigurable.writeFilterNbt(tag, filterItems)

  override def loadAdditional(tag: CompoundTag, provider: HolderLookup.Provider): Unit =
    super.loadAdditional(tag, provider)
    ContainerHelper.loadAllItems(tag, items, provider)
    FilterConfigurable.readFilterNbt(tag, filterItems)

  override def getOpeningData(player: ServerPlayer): FilterConfigurable.ExtendedDataWithPos  =
    FilterConfigurable.ExtendedDataWithPos(worldPosition, this)

  override def getItems: NonNullList[ItemStack] = items

  override def setItems(nonNullList: NonNullList[ItemStack]): Unit = items = nonNullList
  

  override def startOpen(player: Player): Unit =
    super.startOpen(player)
    if !this.remove && !player.isSpectator then
      incrementOpeners(player)

  override def stopOpen(player: Player): Unit =
    super.stopOpen(player)
    if !this.remove && !player.isSpectator then
      decrementOpeners(player)

  override def createMenu(i: Int, inventory: Inventory): AbstractContainerMenu =
    FilterChestMenu(i, inventory, this, this, getOpeningData(inventory.player.asInstanceOf[ServerPlayer]))

object FilterChestBlockEntity:
  val filterCount: Int = 18
  val inventorySize: Int = 9
