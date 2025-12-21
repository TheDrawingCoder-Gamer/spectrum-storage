package gay.menkissing.spectrumstorage.content.block.entity

import de.dafuqs.spectrum.blocks.bottomless_bundle.BottomlessBundleItem
import de.dafuqs.spectrum.enchantments.SpectrumEnchantment
import de.dafuqs.spectrum.registries.{SpectrumEnchantments, SpectrumItems}
import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.block.BottomlessShelfBlock
import gay.menkissing.spectrumstorage.content.item.BottomlessBottleItem
import gay.menkissing.spectrumstorage.content.{SpectrumStorageBlocks, SpectrumStorageItems}
import net.fabricmc.fabric.api.transfer.v1.fluid.{FluidStorage, FluidVariant}
import net.fabricmc.fabric.api.transfer.v1.item.{ItemStorage, ItemVariant}
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions
import net.fabricmc.fabric.api.transfer.v1.storage.base.{CombinedStorage, ResourceAmount, SingleSlotStorage}
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.{BlockPos, NonNullList}
import net.minecraft.nbt.{CompoundTag, ListTag}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.{Container, ContainerHelper}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.{EnchantmentHelper, Enchantments}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent

import java.util.Objects
import scala.jdk.CollectionConverters.*

abstract class BottomlessStorageBlockEntity(val capacity: Int, baseEntity: BlockEntityType[BottomlessStorageBlockEntity], pos: BlockPos, state: BlockState) extends BlockEntity(baseEntity, pos, state):
  import BottomlessStorageBlockEntity.BundleHelper

  protected val items = NonNullList.withSize(capacity, ItemStack.EMPTY)
  protected var lastInteractedSlot: Int = -1

  val itemStorage: CombinedStorage[ItemVariant, BundleItemStorageWrapper] =
    CombinedStorage((0 until capacity).map(BundleItemStorageWrapper(_)).toList.asJava)
  val fluidStorage: CombinedStorage[FluidVariant, BottleFluidStorageWrapper] =
    CombinedStorage((0 until capacity).map(BottleFluidStorageWrapper(_)).toList.asJava)

  class BundleItemStorageWrapper(val slot: Int) extends SnapshotParticipant[ResourceAmount[ItemVariant]], SingleSlotStorage[ItemVariant]:
    var filter: ItemVariant = ItemVariant.blank()

    def resetVariant(): Unit = filter = ItemVariant.blank()

    def validVariant(storedVariant: ItemVariant, resource: ItemVariant): Boolean =
      if filter.isBlank && !storedVariant.isBlank then
        filter = storedVariant

      if !storedVariant.isBlank then
        assert(storedVariant == filter)

      filter.isBlank || filter == resource

    def bundle: Option[ItemStack] =
      val thingie = BottomlessStorageBlockEntity.this.items.get(slot)
      Option.when(thingie.is(SpectrumItems.BOTTOMLESS_BUNDLE))(thingie)

    override def insert(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
      StoragePreconditions.notBlankNotNegative(resource, maxAmount)

      this.bundle match
        case None => 0L
        case Some(bundle) =>
          val builder = BundleHelper.buildFromStack(bundle)


          if validVariant(builder.template, resource) then
            this.updateSnapshots(transaction)
            val inserted = builder.insert(resource, maxAmount)
            builder.build.patchStack(bundle)

            inserted
          else
            0L

    override def extract(resource: ItemVariant, maxAmount: Long, transaction: TransactionContext): Long =
      StoragePreconditions.notBlankNotNegative(resource, maxAmount)

      this.bundle match
        case None => 0L
        case Some(bundle) =>
          val builder = BundleHelper.buildFromStack(bundle)

          if validVariant(builder.template, resource) then
            this.updateSnapshots(transaction)
            val extracted = builder.extract(resource, maxAmount)
            builder.build.patchStack(bundle)

            extracted
          else
            0L

    override def isResourceBlank: Boolean =
      this.bundle.forall: stack =>
        BundleHelper.contentsFromStack(stack).isEmpty

    override def getResource: ItemVariant =
      this.bundle match
        case None => ItemVariant.blank()
        case Some(bundle) => BundleHelper.contentsFromStack(bundle).variant

    override def getAmount: Long =
      this.bundle match
        case None => 0L
        case Some(bundle) => BundleHelper.contentsFromStack(bundle).count

    override def getCapacity: Long =
      this.bundle match
        case None => 0L
        case Some(bundle) => BottomlessBundleItem.getMaxStoredAmount(EnchantmentHelper.getEnchantments(bundle).getOrDefault(Enchantments.POWER_ARROWS, 0))

    override def createSnapshot(): ResourceAmount[ItemVariant] =
      this.bundle match
        case None => ResourceAmount(ItemVariant.blank(), 0L)
        case Some(bundle) =>
          val contents = BundleHelper.contentsFromStack(bundle)
          ResourceAmount(contents.variant, contents.count)

    override def readSnapshot(snapshot: ResourceAmount[ItemVariant]): Unit =
      this.bundle match
        case None => ()
        case Some(bundle) =>
          BundleHelper.Contents(snapshot.resource(), snapshot.amount()).patchStack(bundle)

    override def onFinalCommit(): Unit =
      // better safe than soggy...
      BottomlessStorageBlockEntity.this.setChanged()

  class BottleFluidStorageWrapper(val slot: Int) extends SnapshotParticipant[ResourceAmount[FluidVariant]], SingleSlotStorage[FluidVariant]:
    var filter: FluidVariant = FluidVariant.blank()


    def resetVariant(): Unit = filter = FluidVariant.blank()

    def bottle: Option[ItemStack] =
      val thingie = BottomlessStorageBlockEntity.this.items.get(slot)
      Option.when(thingie.is(SpectrumStorageItems.bottomlessBottle))(thingie)

    def validVariant(storedVariant: FluidVariant, resource: FluidVariant): Boolean =
      if filter.isBlank && !storedVariant.isBlank then
        filter = storedVariant

      if !storedVariant.isBlank then
        assert(storedVariant == filter)

      filter.isBlank || filter == resource

    override def insert(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
      StoragePreconditions.notBlankNotNegative(resource, maxAmount)

      this.bottle match
        case None => 0L
        case Some(bottle) =>
          val builder = BottomlessBottleItem.BottomlessBottleContents.Builder.fromStack(bottle)

          if validVariant(builder.template, resource) then
            this.updateSnapshots(transaction)
            val inserted = builder.insert(resource, maxAmount)
            BottomlessBottleItem.BottomlessBottleContents.replaceInStack(bottle, builder.build)
            //StasisCoolerBlockEntity.this.setItem(slot, bottle)

            inserted
          else
            0L
    }

    override def extract(resource: FluidVariant, maxAmount: Long, transaction: TransactionContext): Long = {
      StoragePreconditions.notBlankNotNegative(resource, maxAmount)

      this.bottle match
        case None => 0L
        case Some(bottle) =>
          val builder = BottomlessBottleItem.BottomlessBottleContents.Builder.fromStack(bottle)

          if validVariant(builder.template, resource) then
            this.updateSnapshots(transaction)
            val extracted = builder.extract(resource, maxAmount)
            BottomlessBottleItem.BottomlessBottleContents.replaceInStack(bottle, builder.build)
            //StasisCoolerBlockEntity.this.setItem(slot, bottle)

            extracted
          else
            0L
    }

    override def getResource: FluidVariant =
      this.bottle match
        case None => FluidVariant.blank()
        case Some(b) => BottomlessBottleItem.BottomlessBottleContents.getFromStack(b).variant

    override def isResourceBlank: Boolean = getResource.isBlank

    override def getAmount: Long =
      this.bottle match
        case None => 0L
        case Some(bottle) =>
          BottomlessBottleItem.BottomlessBottleContents.getFromStack(bottle).amount

    override def getCapacity: Long =
      this.bottle match
        case None => 0L
        case Some(bottle) =>
          BottomlessBottleItem.getMaxStackExpensive(bottle)
    override def createSnapshot(): ResourceAmount[FluidVariant] =
      this.bottle match
        case None => ResourceAmount(FluidVariant.blank(), 0L)
        case Some(bottle) =>
          val contents = BottomlessBottleItem.BottomlessBottleContents.getFromStack(bottle)
          ResourceAmount(contents.variant, contents.amount)

    override def readSnapshot(t: ResourceAmount[FluidVariant]): Unit =
      this.bottle match
        case None => ()
        case Some(bottle) =>
          val contents = BottomlessBottleItem.BottomlessBottleContents(t.resource(), t.amount())
          BottomlessBottleItem.BottomlessBottleContents.replaceInStack(bottle, contents)

    override def onFinalCommit(): Unit =
      // better safe than soggy...
      BottomlessStorageBlockEntity.this.setChanged()
  override def load(tag: CompoundTag): Unit =
    ContainerHelper.loadAllItems(tag, items)
    this.loadItemFilters(tag)
    this.loadFluidFilters(tag)
    this.lastInteractedSlot = tag.getInt("last_interacted_slot")

  override def saveAdditional(tag: CompoundTag): Unit =
    super.saveAdditional(tag)
    ContainerHelper.saveAllItems(tag, items)
    this.saveItemFilters(tag)
    this.saveFluidFilters(tag)
    tag.putInt("last_interacted_slot", lastInteractedSlot)
  
  def clearContent(): Unit =
    items.clear()
  
  def isEmpty: Boolean =
    items.stream().allMatch(_.isEmpty)
  
  private def updateSlot(slot: Int): Unit =
    val stack = this.items.get(slot)
    if stack.is(SpectrumStorageItems.bottomlessBottle) then
      itemStorage.parts.get(slot).resetVariant()
      fluidStorage.parts.get(slot).filter = BottomlessBottleItem.BottomlessBottleContents.getFromStack(stack).variant
    else if stack.is(SpectrumItems.BOTTOMLESS_BUNDLE) then
      fluidStorage.parts.get(slot).resetVariant()
      itemStorage.parts.get(slot).filter = BundleHelper.contentsFromStack(stack).variant
    else
      fluidStorage.parts.get(slot).resetVariant()
      itemStorage.parts.get(slot).resetVariant()
  
  protected def updateSlotShown(slot: Int): Unit = ()

  def loadItemFilters(tag: CompoundTag): Unit =
    val listTag = tag.getList(BottomlessStorageBlockEntity.tagItemFilters, 10)
    for i <- 0 until listTag.size() do
      val compound = listTag.getCompound(i)
      val j = compound.getByte("Slot").toInt
      if j >= 0 && j < capacity then
        // if error then will auto return blankie wankie
        val variant = ItemVariant.fromNbt(compound)
        itemStorage.parts.get(j).filter = variant
  
  def loadFluidFilters(tag: CompoundTag): Unit =
    val listTag = tag.getList(BottomlessStorageBlockEntity.tagFluidFilters, 10)
    for i <- 0 until listTag.size() do
      val compound = listTag.getCompound(i)
      val j = compound.getByte("Slot").toInt
      if j >= 0 && j < capacity then
        val variant = FluidVariant.fromNbt(compound)
        fluidStorage.parts.get(j).filter = variant
  
  def saveItemFilters(tag: CompoundTag): Unit =
    val listTag = ListTag()
    for i <- 0 until capacity do
      val filter = itemStorage.parts.get(i).filter
      if !filter.isBlank then
        val compound = filter.toNbt
        compound.putByte("Slot", i.toByte)
        listTag.add(compound)
    tag.put(BottomlessStorageBlockEntity.tagItemFilters, listTag)

  def saveFluidFilters(tag: CompoundTag): Unit =
    val listTag = ListTag()
    for i <- 0 until 6 do
      val filter = fluidStorage.parts.get(i).filter
      if !filter.isBlank then
        val compound = filter.toNbt
        compound.putByte("Slot", i.toByte)
        listTag.add(compound)
    tag.put(BottomlessStorageBlockEntity.tagFluidFilters, listTag)
  
  def getItem(slot: Int): ItemStack = this.items.get(slot)
  
  def removeItem(slot: Int, amount: Int): ItemStack =
    val stack = Objects.requireNonNullElse(this.items.get(slot), ItemStack.EMPTY)
    this.items.set(slot, ItemStack.EMPTY)
    updateSlot(slot)
    if !stack.isEmpty then
      this.updateSlotShown(slot)
    
    stack
  
  def removeItemNoUpdate(slot: Int): ItemStack = this.removeItem(slot, 1)
  
  def setItem(slot: Int, stack: ItemStack): Unit =
    if stack.is(SpectrumStorageItems.bottomlessBottle) || stack.is(SpectrumItems.BOTTOMLESS_BUNDLE) then
      this.items.set(slot, stack)
      this.updateSlot(slot)
      this.updateSlotShown(slot)
    else if stack.isEmpty then
      this.removeItem(slot, 1)
  
  def stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)
  
  def getMaxStackSize(stack: ItemStack): Int = 1

object BottomlessStorageBlockEntity:
  val tagFluidFilters = "fluid_filters"
  val tagItemFilters = "item_filters"

  object BundleHelper:
    def contentsFromStack(stack: ItemStack): Contents =
      if stack.is(SpectrumItems.BOTTOMLESS_BUNDLE) then
        val stackKind = BottomlessBundleItem.getFirstBundledStack(stack)
        val stackAmount = BottomlessBundleItem.getStoredAmount(stack)
        Contents(ItemVariant.of(stackKind), stackAmount.toLong)
      else
        Contents.EMPTY

    final case class Contents(variant: ItemVariant, count: Long):
      def isEmpty: Boolean = variant.isBlank || count == 0

      def patchStack(stack: ItemStack): Unit =
        if stack.is(SpectrumItems.BOTTOMLESS_BUNDLE) then
          if this.isEmpty then
            stack.removeTagKey("StoredStack")
          else
            // THIS WOULDNT NEED TO HAPPEN IF WE JUST USED DATA COMPONENTS
            // OR GOD FORBID USE ItemVariant
            // God hates us all
            val storedItemTag = new CompoundTag()
            val maxStoredAmount = BottomlessBundleItem.getMaxStoredAmount(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack))
            val newAmount = math.min(maxStoredAmount, count.toInt)

            val id = BuiltInRegistries.ITEM.getKey(variant.getItem)
            storedItemTag.putString("ID", id.toString)
            storedItemTag.putInt("Count", newAmount)
            if variant.getNbt != null then
              storedItemTag.put("Tag", variant.getNbt.copy())

            stack.getOrCreateTag().put("StoredStack", storedItemTag)


    object Contents:
      val EMPTY: Contents = Contents(ItemVariant.blank(), 0L)

    def buildFromStack(stack: ItemStack): Builder =
      val c = contentsFromStack(stack)
      Builder(c.variant,
        c.count,
        BottomlessBundleItem.getMaxStoredAmount(EnchantmentHelper.getEnchantments(stack).getOrDefault(Enchantments.POWER_ARROWS, 0)),
        EnchantmentHelper.getItemEnchantmentLevel(SpectrumEnchantments.VOIDING, stack) > 0)

    final class Builder(var template: ItemVariant, var amount: Long, val max: Long, val voiding: Boolean):
      def isEmpty: Boolean =
        template.isBlank || amount == 0

      // TODO: storing

      def copied: Builder =
        Builder(template, amount, max, voiding)

      def build: Contents =
        Contents(template, amount)

      def getMaxAllowed(variant: ItemVariant, amount: Long): Long =
        if variant.isBlank || amount <= 0 || (!this.isEmpty && template != variant) then
          0
        else if voiding then
          Int.MaxValue
        else
          this.max - this.amount

      def insert(variant: ItemVariant, amount: Long): Long =
        val added =

          math.min(amount, getMaxAllowed(variant, amount))
        if added == 0 then
          return 0
        if this.isEmpty then
          this.template = variant

        this.amount += added
        added

      def extract(variant: ItemVariant, amount: Long): Long =
        if variant != template then
          0
        else
          val toRemove = math.min(this.amount, amount)
          this.amount -= toRemove
          if this.amount == 0 then
            this.template = ItemVariant.blank()

          toRemove


  def removeContainer(level: Level, pos: BlockPos, player: Player, blockEntity: BottomlessStorageBlockEntity, slot: Int): Unit =
    if !level.isClientSide then
      val stack = blockEntity.removeItem(slot, 1)
      if !player.getInventory.add(stack) then
        player.drop(stack, false)

      level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos)