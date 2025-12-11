package gay.menkissing.spectrumstorage.content.block

import de.dafuqs.spectrum.registries.SpectrumItems
import gay.menkissing.spectrumstorage.content.SpectrumStorageItems
import gay.menkissing.spectrumstorage.content.block.entity.BottomlessShelfBlockEntity
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.util.StringRepresentable
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.{Containers, InteractionHand, InteractionResult}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.{BaseEntityBlock, Block, HorizontalDirectionalBlock, RenderShape}
import net.minecraft.world.level.block.state.{BlockBehaviour, BlockState, StateDefinition}
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.phys.{BlockHitResult, Vec2}

class BottomlessShelfBlock(props: BlockBehaviour.Properties) extends BaseEntityBlock(props):
  locally:
    var blockState = this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
    BottomlessShelfBlock.SHELF_SLOT_OCCUPIED_PROPS.foreach: prop =>
      blockState = blockState.setValue(prop, BottomlessShelfBlock.ShelfSlotOccupiedBy.Empty)
    this.registerDefaultState(blockState)

  override def getStateForPlacement(ctx: BlockPlaceContext): BlockState =
    val dir = ctx.getHorizontalDirection.getOpposite
    this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, dir)


  def getHitSlot(hitResult: BlockHitResult, state: BlockState): Option[Int] =
    BottomlessShelfBlock.getRelativeHitCoordsForFace(hitResult, state.getValue(HorizontalDirectionalBlock.FACING)).map: vec2 =>
      val i = if vec2.y > 0.5f then 0 else 1
      val j = BottomlessShelfBlock.getHorzSection(vec2.x)
      j + i * 3

  override def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]): Unit =
    super.createBlockStateDefinition(builder)
    builder.add(HorizontalDirectionalBlock.FACING)
    BottomlessShelfBlock.SHELF_SLOT_OCCUPIED_PROPS.foreach(it => builder.add(it))

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
    BottomlessShelfBlockEntity(pos, state)

  def useItemOn(stack: ItemStack, state: BlockState, level: Level, pos: BlockPos
               , player: Player, hand: InteractionHand, hitResult: BlockHitResult): InteractionResult =
    level.getBlockEntity(pos) match
      case blockEntity: BottomlessShelfBlockEntity =>
        if !stack.is(SpectrumStorageItems.bottomlessBottle) && !stack.is(SpectrumItems.BOTTOMLESS_BUNDLE) then
          InteractionResult.PASS
        else
          this.getHitSlot(hitResult, state) match
            case None =>
              InteractionResult.PASS
            case Some(slot) =>
              if !state.getValue(BottomlessShelfBlock.SHELF_SLOT_OCCUPIED_PROPS(slot)).isEmpty then
                InteractionResult.PASS
              else
                BottomlessShelfBlock.insertContainer(level, pos, player, blockEntity, stack, slot)
                InteractionResult.sidedSuccess(level.isClientSide)
      case _ => InteractionResult.PASS

  def useWithoutItem(state: BlockState, level: Level, pos: BlockPos, player: Player, hitResult: BlockHitResult): InteractionResult =
    level.getBlockEntity(pos) match
      case blockEntity: BottomlessShelfBlockEntity =>
        this.getHitSlot(hitResult, state) match
          case None =>
            InteractionResult.PASS
          case Some(slot) =>
            if state.getValue(BottomlessShelfBlock.SHELF_SLOT_OCCUPIED_PROPS(slot)).isEmpty then
              InteractionResult.CONSUME
            else
              BottomlessShelfBlock.removeContainer(level, pos, player, blockEntity, slot)
              InteractionResult.sidedSuccess(level.isClientSide)
      case _ => InteractionResult.PASS

  override def use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hitResult: BlockHitResult): InteractionResult =
    val item = player.getItemInHand(hand)
    if item.isEmpty then
      useWithoutItem(state, level, pos, player, hitResult)
    else
      useItemOn(item, state, level, pos, player, hand, hitResult)

  override def getRenderShape(blockState: BlockState): RenderShape = RenderShape.MODEL

  override def onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean): Unit =
    if state.is(newState.getBlock) then
      return

    val update = level.getBlockEntity(pos) match
      case blockEntity: BottomlessShelfBlockEntity if !blockEntity.isEmpty =>
        (0 until 6).foreach: slot =>
          val item = blockEntity.getItem(slot)
          if !item.isEmpty then
            Containers.dropItemStack(level, pos.getX, pos.getY, pos.getZ, item)

        blockEntity.clearContent()
        true
      case _ => false

    super.onRemove(state, level, pos, newState, movedByPiston)

    if update then
      level.updateNeighbourForOutputSignal(pos, this)



object BottomlessShelfBlock:
  enum ShelfSlotOccupiedBy extends Enum[ShelfSlotOccupiedBy], StringRepresentable:
    case Empty, Bottle, Bundle

    def isEmpty: Boolean = this == Empty

    override def getSerializedName: String =
      this match
        case Empty => "empty"
        case Bottle => "bottle"
        case Bundle => "bundle"
    end getSerializedName
  end ShelfSlotOccupiedBy

  private def makeProp(idx: Int): EnumProperty[ShelfSlotOccupiedBy] =
    EnumProperty.create(s"shelf_slot_${idx}_occupied_by", classOf[ShelfSlotOccupiedBy])

  val SHELF_SLOT_0_OCCUPIED_BY: EnumProperty[ShelfSlotOccupiedBy] = makeProp(0)
  val SHELF_SLOT_1_OCCUPIED_BY: EnumProperty[ShelfSlotOccupiedBy] = makeProp(1)
  val SHELF_SLOT_2_OCCUPIED_BY: EnumProperty[ShelfSlotOccupiedBy] = makeProp(2)
  val SHELF_SLOT_3_OCCUPIED_BY: EnumProperty[ShelfSlotOccupiedBy] = makeProp(3)
  val SHELF_SLOT_4_OCCUPIED_BY: EnumProperty[ShelfSlotOccupiedBy] = makeProp(4)
  val SHELF_SLOT_5_OCCUPIED_BY: EnumProperty[ShelfSlotOccupiedBy] = makeProp(5)

  val SHELF_SLOT_OCCUPIED_PROPS: List[EnumProperty[ShelfSlotOccupiedBy]] = List(
    SHELF_SLOT_0_OCCUPIED_BY,
    SHELF_SLOT_1_OCCUPIED_BY,
    SHELF_SLOT_2_OCCUPIED_BY,
    SHELF_SLOT_3_OCCUPIED_BY,
    SHELF_SLOT_4_OCCUPIED_BY,
    SHELF_SLOT_5_OCCUPIED_BY
  )

  def insertContainer(level: Level, pos: BlockPos, player: Player, blockEntity: BottomlessShelfBlockEntity, stack: ItemStack, slot: Int): Unit =
    if !level.isClientSide then
      blockEntity.setItem(slot, stack.copyAndClear())

  def removeContainer(level: Level, pos: BlockPos, player: Player, blockEntity: BottomlessShelfBlockEntity, slot: Int): Unit =
    if !level.isClientSide then
      val stack = blockEntity.removeItem(slot, 1)
      if !player.getInventory.add(stack) then
        player.drop(stack, false)

      level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos)

  def getHorzSection(x: Float): Int =
    val g = 0.375f
    val h = 0.6875f
    if x < g then
      0
    else if x < h then
      1
    else
      2

  def getRelativeHitCoordsForFace(hitResult: BlockHitResult, face: Direction): Option[Vec2] =
    val dir = hitResult.getDirection
    if face != dir then
      None
    else
      val pos = hitResult.getBlockPos.relative(dir)
      val vec3 = hitResult.getLocation.subtract(pos.getX, pos.getY, pos.getZ)
      val d = vec3.x()
      val e = vec3.y()
      val f = vec3.z()

      dir match
        case Direction.NORTH => Some(Vec2((1.0 - d).toFloat, e.toFloat))
        case Direction.SOUTH => Some(Vec2(d.toFloat, e.toFloat))
        case Direction.WEST => Some(Vec2(f.toFloat, e.toFloat))
        case Direction.EAST => Some(Vec2((1.0 - f).toFloat, e.toFloat))
        case _ => None