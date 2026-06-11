package gay.menkissing.spectrumstorage.util

import net.neoforged.neoforge.fluids.FluidType

object SpectrumStorageNumberFormatting:
  def formatMB(mb: Int): String =
    if mb < 1000 then
      mb.toLong.toString
    else if mb < 1000000 then
      String.format("%1$.2fK", mb / 1000f)
    else
      String.format("%1$.2fM", mb / 1000000f)

  def formatFluidMax(amountMb: Int): String =
    val buckets = math.round(amountMb.toFloat / FluidType.BUCKET_VOLUME)
    buckets.toString
