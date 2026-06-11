package gay.menkissing.spectrumstorage.content.block.entity

import de.dafuqs.spectrum.blocks.bottomless_bundle.{BottomlessBundleItem, BottomlessComponent, BottomlessItemHandler}
import de.dafuqs.spectrum.registries.{SpectrumBlocks, SpectrumDataComponentTypes, SpectrumEnchantmentKeys, SpectrumEnchantmentTags, SpectrumItems}
import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.SpectrumStorageBlocks.bottomlessBarrel
import gay.menkissing.spectrumstorage.content.block.BottomlessShelfBlock
import gay.menkissing.spectrumstorage.content.item.BottomlessBottleItem
import gay.menkissing.spectrumstorage.content.{SpectrumStorageBlocks, SpectrumStorageItems}
import gay.menkissing.spectrumstorage.registries.SpectrumStorageComponents
import gay.menkissing.spectrumstorage.screen.BottomlessStorageMenu
import gay.menkissing.spectrumstorage.util.{FluidResource, ItemResource, SpectrumStorageEnchantmentHelper}
import gay.menkissing.spectrumstorage.util.spectrum.BundleHelper
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.{BlockPos, Direction, HolderLookup, NonNullList, Vec3i}
import net.minecraft.nbt.{CompoundTag, ListTag, NbtOps, Tag}
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Component.Serializer
import net.minecraft.sounds.{SoundEvent, SoundEvents, SoundSource}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.{AbstractContainerMenu, ChestMenu}
import net.minecraft.world.{Container, ContainerHelper, Containers, MenuProvider, Nameable}
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.{EnchantmentHelper, Enchantments}
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BarrelBlock
import net.minecraft.world.level.block.entity.{BaseContainerBlockEntity, BlockEntity, BlockEntityType, ContainerOpenersCounter}
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.gameevent.GameEvent
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.capabilities.{Capabilities, RegisterCapabilitiesEvent}
import net.neoforged.neoforge.fluids.{FluidStack, SimpleFluidContent}
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.items.IItemHandler

import java.util.Objects
import scala.jdk.CollectionConverters.*

abstract class BottomlessStorageBlockEntity(val capacity: Int, baseEntity: BlockEntityType[? <: BottomlessStorageBlockEntity], pos: BlockPos, state: BlockState) extends BlockEntity(baseEntity, pos, state):




  protected val items = NonNullList.withSize(capacity, ItemStack.EMPTY)
  protected var lastInteractedSlot: Int = -1


  val itemStorage: ItemStorages = new ItemStorages
  val fluidStorage: FluidStorages = new FluidStorages

  class ItemStorages extends IItemHandler {
    val storages = Vector.tabulate(capacity)(new BundleItemStorageHandler(_))

    def unsetSlot(slot: Int): Unit =
      storages(slot).unset()

    def setSlotFilter(slot: Int, stack: ItemResource): Unit =
      storages(slot).setFilter(stack)

    override def getSlots: Int = capacity

    override def getStackInSlot(slot: Int): ItemStack =
      storages(slot).getStackInSlot(0)

    override def getSlotLimit(slot: Int): Int =
      storages(slot).getSlotLimit(0)

    override def insertItem(slot: Int, stack: ItemStack, simulated: Boolean): ItemStack =
      storages(slot).insertItem(0, stack, simulated)

    override def extractItem(slot: Int, amount: Int, simulated: Boolean): ItemStack =
      storages(slot).extractItem(0, amount, simulated)

    override def isItemValid(slot: Int, stack: ItemStack): Boolean = true
  }

  class BundleItemStorageHandler(val slot: Int) extends IItemHandler {
    def stack: ItemStack = items.get(slot)

    def isVoidingStack(stack: ItemStack): Boolean =
      EnchantmentHelper.hasTag(stack, SpectrumEnchantmentTags.DELETES_OVERFLOW)

    def isValidStack(stack: ItemStack): Boolean =
      stack.is(SpectrumBlocks.BOTTOMLESS_BUNDLE.asItem())

    def stackMax(stack: ItemStack): Int =
      if isValidStack(stack) then
        BottomlessComponent.getMaxStoredAmount(
          SpectrumStorageEnchantmentHelper.getLevel(level.registryAccess(), Enchantments.POWER, stack)
        ).toInt
      else
        0


    var filter: ItemResource = ItemResource.EMPTY


    def unset(): Unit =
      this.filter = ItemResource.EMPTY

    def setFilter(filter: ItemResource): Unit =
      this.filter = filter


    def permits(stored: ItemResource, resource: ItemResource): Boolean =
      if !stored.isBlank && !filter.isBlank && filter != stored then
        SpectrumStorage.Logger.info("player must have modified the bundle manually, updating filter")

      if !stored.isBlank then
        filter = stored
        setChanged()

      filter.isBlank || filter == resource

    override def getSlots: Int = 1

    override def getStackInSlot(i: Int): ItemStack =
      val bundle = this.stack
      if isValidStack(bundle) then
        val storage = BundleHelper.BundleStorageBuilder.fromStackWithAccess(bundle, level.registryAccess())
        storage.makeStack(math.min(storage.count, storage.stackSize).toInt).copy()
      else
        ItemStack.EMPTY

    override def insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack =
      val bundle = this.stack
      if isValidStack(bundle) && slot == 0 then
        val storage = BundleHelper.BundleStorageBuilder.fromStackWithAccess(bundle, level.registryAccess())
        if !permits(storage.template, ItemResource.ofStack(stack)) then
          return stack
        val change = storage.insert(ItemResource.ofStack(stack), stack.getCount)
        val remainder = stack.copyWithCount(stack.getCount - change)

        if simulate then
          remainder
        else
          storage.save(bundle)
          setChanged()
          if isVoidingStack(bundle) then
            ItemStack.EMPTY
          else
            remainder
      else
        stack

    override def extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack =
      val bundle = this.stack
      if isValidStack(stack) && slot == 0 then
        val storage = BundleHelper.BundleStorageBuilder.fromStackWithAccess(bundle, level.registryAccess())
        val result = storage.makeStack(storage.extract(amount).toInt)

        if !simulate then
          setChanged()
          storage.save(bundle)
        result
      else
        ItemStack.EMPTY

    override def getSlotLimit(slot: Int): Int =
      val bundle = this.stack
      if isValidStack(bundle) then
        stackMax(bundle)
      else
        0

    override def isItemValid(slot: Int, itemStack: ItemStack): Boolean =
      true

  }

  class FluidStorages extends IFluidHandler {
    val storages = Vector.tabulate(capacity)(new BottleFluidStorageHandler(_))

    def unsetSlot(slot: Int): Unit =
      storages(slot).unset()

    def setSlotFilter(slot: Int, filter: FluidResource): Unit =
      storages(slot).setFilter(filter)

    def setSlotFilterFromStack(slot: Int, stack: ItemStack): Unit =
      storages(slot).setFilterFromStack(stack)

    override def getTanks: Int = capacity

    override def getFluidInTank(slot: Int): FluidStack =
      storages(slot).getFluidInTank(0)

    override def getTankCapacity(slot: Int): Int =
      storages(slot).getTankCapacity(0)

    override def isFluidValid(slot: Int, fluidStack: FluidStack): Boolean =
      storages(slot).isFluidValid(0, fluidStack)

    override def fill(resource: FluidStack, fluidAction: IFluidHandler.FluidAction): Int =
      var i = 0
      var toFill = resource.getAmount
      var amountFill = 0
      while i < capacity do
        val storage = storages(i)
        val amount = storage.fill(resource.copyWithAmount(toFill), fluidAction)
        toFill -= amount
        amountFill += amount
        if toFill == 0 then
          return amountFill

        i += 1

      amountFill

    override def drain(resource: FluidStack, fluidAction: IFluidHandler.FluidAction): FluidStack =
      var i = 0
      var toDrain = resource.getAmount
      var amountDrained = 0
      while i < capacity do
        val storage = storages(i)
        val result = storage.drain(resource.copyWithAmount(toDrain), fluidAction)
        toDrain -= result.getAmount
        amountDrained += result.getAmount
        if toDrain == 0 then
          return resource.copyWithAmount(amountDrained)

        i += 1
      resource.copyWithAmount(amountDrained)

    override def drain(maxDrain: Int, fluidAction: IFluidHandler.FluidAction): FluidStack =
      var i = 0
      var template = FluidResource.EMPTY
      var toDrain = maxDrain
      var amountDrained = 0
      while i < capacity do
        val storage = storages(i)
        val result =
          if template.isBlank then
            val r = storage.drain(toDrain, fluidAction)
            if !r.isEmpty then
              template = FluidResource.ofStack(r)
            r
          else
            storage.drain(template.makeStack(toDrain), fluidAction)

        toDrain -= result.getAmount
        amountDrained += result.getAmount

        if toDrain == 0 then
          return template.makeStack(amountDrained)

        i += 1
      if template.isBlank then
        FluidStack.EMPTY
      else
        template.makeStack(amountDrained)


  }

  class BottleFluidStorageHandler(val slot: Int) extends IFluidHandler {
    def stack: ItemStack = items.get(slot)

    def isValidStack(stack: ItemStack): Boolean =
      stack.is(SpectrumStorageItems.bottomlessBottle)

    def stackMax(stack: ItemStack): Int =
      if isValidStack(stack) then
        BottomlessBottleItem.getMaxStack(level, stack)
      else
        0

    var filter: FluidResource = FluidResource.EMPTY

    def unset(): Unit =
      filter = FluidResource.EMPTY

    def setFilter(filter: FluidResource): Unit =
      this.filter = filter

    def setFilterFromStack(stack: ItemStack): Unit =
      val component = stack
        .getOrDefault(SpectrumStorageComponents.BottomlessBottleContentsComponent, SimpleFluidContent.EMPTY)
      val fluidStack = component.copy()
      this.filter = FluidResource.of(component.getFluid, fluidStack.getComponentsPatch)

    def permits(stored: FluidResource, resource: FluidResource): Boolean =
      if !stored.isBlank && !filter.isBlank && filter != stored then
        SpectrumStorage.Logger.debug("Shouldn't be possible to see this due to filter jank")

      if !stored.isBlank then
        filter = stored
        setChanged()

      filter.isBlank || filter == resource

    override def getTanks: Int = 1

    override def getFluidInTank(slot: Int): FluidStack =
      val bottle = stack
      if isValidStack(bottle) then
        val component = bottle
          .getOrDefault(SpectrumStorageComponents.BottomlessBottleContentsComponent, SimpleFluidContent.EMPTY)
        component.copy()
      else
        FluidStack.EMPTY

    override def getTankCapacity(slot: Int): Int =
      val bottle = stack
      if isValidStack(bottle) then
        stackMax(bottle)
      else
        0

    override def isFluidValid(slot: Int, fluidStack: FluidStack): Boolean = true

    override def fill(fluidStack: FluidStack, fluidAction: IFluidHandler.FluidAction): Int =
      val bottle = stack
      if isValidStack(bottle) then
        val builder = BottomlessBottleItem.SimpleFluidContentBuilder.fromStack(bottle)
        if !permits(builder.template, FluidResource.ofStack(fluidStack)) then
          return 0
        val count = builder.insert(FluidResource.ofStack(fluidStack), fluidStack.getAmount)

        if fluidAction.execute() then
          builder.buildAndSet(bottle)
          setChanged()

        count
      else
        0

    override def drain(fluidStack: FluidStack, fluidAction: IFluidHandler.FluidAction): FluidStack =
      val bottle = stack
      if isValidStack(bottle) then
        val builder = BottomlessBottleItem.SimpleFluidContentBuilder.fromStack(bottle)
        val count = builder.extract(FluidResource.ofStack(fluidStack), fluidStack.getAmount)

        if fluidAction.execute() then
          builder.buildAndSet(bottle)
          setChanged()

        fluidStack.copyWithAmount(count)
      else
        FluidStack.EMPTY

    override def drain(maxDrain: Int, fluidAction: IFluidHandler.FluidAction): FluidStack =
      val bottle = stack
      if isValidStack(bottle) then
        val builder = BottomlessBottleItem.SimpleFluidContentBuilder.fromStack(bottle)
        val resource = builder.template
        val count = builder.extract(resource, maxDrain)

        if fluidAction.execute() then
          builder.buildAndSet(bottle)
          setChanged()

        resource.makeStack(count)
      else
        FluidStack.EMPTY

  }

  override def loadAdditional(tag: CompoundTag, lookup: HolderLookup.Provider): Unit =
    super.loadAdditional(tag, lookup)
    ContainerHelper.loadAllItems(tag, items, lookup)
    this.loadItemFilters(tag, lookup)
    this.loadFluidFilters(tag, lookup)
    this.lastInteractedSlot = tag.getInt("last_interacted_slot")

  override protected def saveAdditional(tag: CompoundTag, lookup: HolderLookup.Provider): Unit =
    super.saveAdditional(tag, lookup)
    ContainerHelper.saveAllItems(tag, items, lookup)
    this.saveItemFilters(tag, lookup)
    this.saveFluidFilters(tag, lookup)
    tag.putInt("last_interacted_slot", lastInteractedSlot)

  def clearContent(): Unit =
    items.clear()

  def isEmpty: Boolean =
    items.stream().allMatch(_.isEmpty)

  protected def updateSlot(slot: Int): Unit =
    val stack = this.items.get(slot)
    if stack.is(SpectrumStorageItems.bottomlessBottle) then
      itemStorage.unsetSlot(slot)
      fluidStorage.setSlotFilterFromStack(slot, stack)
    else if stack.is(SpectrumBlocks.BOTTOMLESS_BUNDLE.asItem()) then
      fluidStorage.unsetSlot(slot)
      val component = BottomlessComponent.get(stack, level.registryAccess(), true)
      itemStorage.setSlotFilter(slot, ItemResource.ofStack(component.handler().variant))
    else
      fluidStorage.unsetSlot(slot)
      itemStorage.unsetSlot(slot)

  protected def updateSlotShown(slot: Int): Unit = ()

  def loadItemFilters(tag: CompoundTag, provider: HolderLookup.Provider): Unit =
    val listTag = tag.getList(BottomlessStorageBlockEntity.tagItemFilters, 10)
    for i <- 0 until listTag.size() do
      val compound = listTag.getCompound(i)
      val j = compound.getByte("Slot").toInt
      if j >= 0 && j < capacity then
        // if error then will auto return blankie wankie
        val variant = ItemResource.CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), compound).getOrThrow()
        itemStorage.setSlotFilter(j, variant.getFirst)

  def loadFluidFilters(tag: CompoundTag, provider: HolderLookup.Provider): Unit =
    val listTag = tag.getList(BottomlessStorageBlockEntity.tagFluidFilters, 10)
    for i <- 0 until listTag.size() do
      val compound = listTag.getCompound(i)
      val j = compound.getByte("Slot").toInt
      if j >= 0 && j < capacity then

        val variant = FluidResource.CODEC.decode(provider.createSerializationContext(NbtOps.INSTANCE), compound).getOrThrow().getFirst
        fluidStorage.setSlotFilter(j, variant)

  def saveItemFilters(tag: CompoundTag, provider: HolderLookup.Provider): Unit =
    val listTag = ListTag()
    for i <- 0 until capacity do
      val filter = itemStorage.storages(i).filter
      if !filter.isBlank then
        val compound = CompoundTag()
        compound.putByte("Slot", i.toByte)
        val compound2 = ItemResource.CODEC.encode(filter, provider.createSerializationContext(NbtOps.INSTANCE), compound).getOrThrow()
        listTag.add(compound2)
    tag.put(BottomlessStorageBlockEntity.tagItemFilters, listTag)

  def saveFluidFilters(tag: CompoundTag, provider: HolderLookup.Provider): Unit =
    val listTag = ListTag()
    for i <- 0 until 6 do
      val filter = fluidStorage.storages(i).filter
      if !filter.isBlank then
        val compound = CompoundTag()
        compound.putByte("Slot", i.toByte)
        val compound2 = FluidResource.CODEC.encode(filter, provider.createSerializationContext(NbtOps.INSTANCE), compound).getOrThrow()
        listTag.add(compound2)
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
    if stack.is(SpectrumStorageItems.bottomlessBottle) || stack.is(SpectrumBlocks.BOTTOMLESS_BUNDLE.asItem()) then
      this.items.set(slot, stack)
      this.updateSlot(slot)
      this.updateSlotShown(slot)
    else if stack.isEmpty then
      this.removeItem(slot, 1)

  def stillValid(player: Player): Boolean = Container.stillValidBlockEntity(this, player)

  def getMaxStackSize(stack: ItemStack): Int = 1





abstract class ContainerBottomlessStorageBlockEntity(capacity: Int, baseEntity: BlockEntityType[? <: BottomlessStorageBlockEntity], pos: BlockPos, state: BlockState) extends BottomlessStorageBlockEntity(capacity, baseEntity, pos, state), MenuProvider, NameableBlockEntity:
  protected val openSound: SoundEvent = SoundEvents.BARREL_OPEN
  protected val closeSound: SoundEvent = SoundEvents.BARREL_CLOSE


  val containerView = new ContainerForBottomlessStorage()





  private val openersCounter: ContainerOpenersCounter =
    new ContainerOpenersCounter:
      override def onOpen(level: Level, blockPos: BlockPos, blockState: BlockState): Unit =
        ContainerBottomlessStorageBlockEntity.this.playSound(blockState, SoundEvents.BARREL_OPEN)
        ContainerBottomlessStorageBlockEntity.this.updateBlockState(blockState, true)

      override def onClose(level: Level, blockPos: BlockPos, blockState: BlockState): Unit =
        ContainerBottomlessStorageBlockEntity.this.playSound(blockState, SoundEvents.BARREL_CLOSE)
        ContainerBottomlessStorageBlockEntity.this.updateBlockState(blockState, false)

      override def openerCountChanged(level: Level, blockPos: BlockPos, blockState: BlockState, i: Int, j: Int): Unit = ()

      override def isOwnContainer(player: Player): Boolean =
        player.containerMenu match
          case ce: BottomlessStorageMenu =>
            ce.container match
              case cfbs: ContainerForBottomlessStorage =>
                cfbs.parent == ContainerBottomlessStorageBlockEntity.this
              case _ => false
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

  def defaultName: Component

  final class ContainerForBottomlessStorage extends Container:
    def parent: ContainerBottomlessStorageBlockEntity = ContainerBottomlessStorageBlockEntity.this

    def getContainerSize: Int = capacity


    override def startOpen(player: Player): Unit =
      if !ContainerBottomlessStorageBlockEntity.this.remove && !player.isSpectator then
        incrementOpeners(player)

    override def stopOpen(player: Player): Unit =
      if !ContainerBottomlessStorageBlockEntity.this.remove && !player.isSpectator then
        decrementOpeners(player)

    override def clearContent(): Unit = items.clear()

    override def getItem(i: Int): ItemStack =
      items.get(i)

    override def isEmpty: Boolean = ContainerBottomlessStorageBlockEntity.this.isEmpty

    override def removeItem(slot: Int, amount: Int): ItemStack =
      ContainerBottomlessStorageBlockEntity.this.removeItem(slot, amount)

    override def removeItemNoUpdate(slot: Int): ItemStack =
      ContainerBottomlessStorageBlockEntity.this.removeItemNoUpdate(slot)

    override def setChanged(): Unit =
      // Better safe than soggy.......
      ContainerBottomlessStorageBlockEntity.this.setChanged()

    override def setItem(slot: Int, stack: ItemStack): Unit =
      ContainerBottomlessStorageBlockEntity.this.setItem(slot, stack)

    override def stillValid(player: Player): Boolean =
      ContainerBottomlessStorageBlockEntity.this.stillValid(player)

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

  override def getDisplayName: Component = getName


  def getContainerSize: Int = capacity

object BottomlessStorageBlockEntity:
  val tagFluidFilters = "fluid_filters"
  val tagItemFilters = "item_filters"
  final class BottomlessBarrelBlockEntity(pos: BlockPos, state: BlockState) extends ContainerBottomlessStorageBlockEntity(BottomlessStorageMenu.barrelContainerSize, SpectrumStorageBlocks.bottomlessBarrelBlockEntity.get(), pos, state):
    override def defaultName: Component = Component.translatable("container.spectrumstorage.bottomless_barrel")

    override def createMenu(windowId: Int, inventory: Inventory, player: Player): AbstractContainerMenu =
      BottomlessStorageMenu.barrelServer(windowId, inventory, containerView)


  final class BottomlessAmphoraBlockEntity(pos: BlockPos, state: BlockState) extends ContainerBottomlessStorageBlockEntity(BottomlessStorageMenu.amphoraContainerSize, SpectrumStorageBlocks.bottomlessAmphoraBlockEntity.get(), pos, state):
    override def defaultName: Component = Component.translatable("container.spectrumstorage.bottomless_amphora")

    override def createMenu(windowId: Int, inventory: Inventory, player: Player): AbstractContainerMenu =
      BottomlessStorageMenu.amphoraServer(windowId, inventory, containerView)



  def dropContents(level: Level, pos: BlockPos, entity: BottomlessStorageBlockEntity): Unit =
    (0 until entity.capacity).foreach: slot =>
      val item = entity.getItem(slot)
      if !item.isEmpty then
        Containers.dropItemStack(level, pos.getX, pos.getY, pos.getZ, item)

    entity.clearContent()


  def removeContainer(level: Level, pos: BlockPos, player: Player, blockEntity: BottomlessStorageBlockEntity, slot: Int): Unit =
    if !level.isClientSide then
      val stack = blockEntity.removeItem(slot, 1)
      if !player.getInventory.add(stack) then
        player.drop(stack, false)

      level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos)

  final class BottomlessStorageIFluidProvider

  def registerStorages(bus: IEventBus): Unit =
    import SpectrumStorageBlocks.{
      bottomlessAmphoraBlockEntity => bottomlessAmphoraBE,
      bottomlessShelfBlockEntity => bottomlessShelfBE,
      bottomlessBarrelBlockEntity => bottomlessBarrelBE
    }
    // fingers crossed this works?
    bus.addListener: (ev: RegisterCapabilitiesEvent) =>
      List[BlockEntityType[? <: BottomlessStorageBlockEntity]](bottomlessShelfBE.get(), bottomlessAmphoraBE.get(), bottomlessBarrelBE.get()).foreach: entity =>
        ev.registerBlockEntity(
          Capabilities.ItemHandler.BLOCK,
          entity,
          (en, side) => en.itemStorage
        )
        ev.registerBlockEntity(
          Capabilities.FluidHandler.BLOCK,
          entity,
          (en, side) => en.fluidStorage
        )