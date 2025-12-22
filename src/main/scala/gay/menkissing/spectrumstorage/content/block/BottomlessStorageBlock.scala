package gay.menkissing.spectrumstorage.content.block

import gay.menkissing.spectrumstorage.SpectrumStorage
import gay.menkissing.spectrumstorage.content.block.entity.{BottomlessStorageBlockEntity, ContainerBottomlessStorageBlockEntity}
import gay.menkissing.spectrumstorage.screen.BottomlessStorageMenu
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.{InteractionHand, InteractionResult, MenuProvider}
import net.minecraft.world.entity.player.{Inventory, Player}
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.{BarrelBlock, BaseEntityBlock, Block, Mirror, Rotation}
import net.minecraft.world.level.block.entity.{BlockEntity, BlockEntityType}
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState, StateDefinition}
import net.minecraft.world.phys.BlockHitResult

abstract class BottomlessStorageBlock(val capacity: Int, props: BlockBehaviour.Properties) extends BarrelBlock(props):
  locally:
    this.registerDefaultState:
      this.stateDefinition.any()
          .setValue(BottomlessStorageBlock.FACING, Direction.NORTH)
          .setValue(BottomlessStorageBlock.OPEN, false)
      
  
  
  override def use(blockState: BlockState, level: Level, blockPos: BlockPos, player: Player, interactionHand: InteractionHand, blockHitResult: BlockHitResult): InteractionResult =
    if level.isClientSide then
      InteractionResult.SUCCESS
    else
      val blockEntity = level.getBlockEntity(blockPos)
      blockEntity match
        case bse: ContainerBottomlessStorageBlockEntity =>
          player.openMenu(bse)
        case _ => ()
      InteractionResult.CONSUME

  override def onRemove(replaced: BlockState, level: Level, blockPos: BlockPos, newBlock: BlockState, movedByPiston: Boolean): Unit =
    if !replaced.is(newBlock.getBlock) then
      val be = level.getBlockEntity(blockPos)
      val update =
        be match
          case bse: ContainerBottomlessStorageBlockEntity if !bse.isEmpty =>
            BottomlessStorageBlockEntity.dropContents(level, blockPos, bse)
            true
          case _ => false
      super.onRemove(replaced, level, blockPos, newBlock, movedByPiston)
      if update then
        level.updateNeighbourForOutputSignal(blockPos, this)


  override def tick(blockState: BlockState, serverLevel: ServerLevel, blockPos: BlockPos, randomSource: RandomSource): Unit =
    val be = serverLevel.getBlockEntity(blockPos)
    be match
      case sbe: ContainerBottomlessStorageBlockEntity =>
        sbe.recheckOpen()
      case _ => ()

  override def setPlacedBy(level: Level, blockPos: BlockPos, blockState: BlockState, livingEntity: LivingEntity, itemStack: ItemStack): Unit =
    if itemStack.hasCustomHoverName then
      level.getBlockEntity(blockPos) match
        case bsbe: ContainerBottomlessStorageBlockEntity =>
          bsbe.setCustomName(itemStack.getHoverName)
        case _ => ()
  
  
  
  
          
object BottomlessStorageBlock:
  val FACING = BlockStateProperties.FACING
  val OPEN = BlockStateProperties.OPEN
  
  final class BottomlessBarrelBlock(props: BlockBehaviour.Properties) extends BottomlessStorageBlock(BottomlessStorageMenu.barrelContainerSize, props):
    override def newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity =
      new BottomlessStorageBlockEntity.BottomlessBarrelBlockEntity(blockPos, blockState)
  
  final class BottomlessAmphoraBlock(props: BlockBehaviour.Properties) extends BottomlessStorageBlock(BottomlessStorageMenu.amphoraContainerSize, props):
    override def newBlockEntity(blockPos: BlockPos, blockState: BlockState): BlockEntity =
      new BottomlessStorageBlockEntity.BottomlessAmphoraBlockEntity(blockPos, blockState)