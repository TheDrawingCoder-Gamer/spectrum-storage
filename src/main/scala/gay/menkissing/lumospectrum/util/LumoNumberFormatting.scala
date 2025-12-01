package gay.menkissing.lumospectrum.util

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants

object LumoNumberFormatting:
  def formatMB(amount: Long): String =
    val mb = amount.toFloat / 81f
    if mb < 1000 then
      mb.toLong.toString
    else if mb < 1000000 then
      String.format("%1$.2fK", mb / 1000f)
    else
      String.format("%1$.2fM", mb / 1000000f)

  def formatFluidMax(amount: Long): String =
    val buckets = math.round(amount.toFloat / FluidConstants.BUCKET)
    buckets.toString
