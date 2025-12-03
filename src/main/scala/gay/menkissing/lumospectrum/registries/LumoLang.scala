package gay.menkissing.lumospectrum.registries

import gay.menkissing.lumospectrum.util.registry.InfoCollector

object LumoLang:
  def init(): Unit =
    InfoCollector.instance
                 .addGuidebookEntry("bottomless_bottle")
                 .addPage(
                   """
                     |I find myself often carrying half a dozen buckets holding the same thing. It would be nice if I could just cram all that liquid in one item.
                     |
                     |The bottomless bottle does just that - it stores a large amount of liquid inside itself.
                     |""".stripMargin)
                 .addPage("Right-click picks up liquid, while sneak right-click places it.")
                 .addPage("Power doubled its capacity each level.")
                 .save()
    InfoCollector.instance.addGuidebookEntry("bottomless_shelf")
                 .addPage(
                   """
                     |My bottomless bottle seems to be too unstable to place down on its own - So why not put it on a shelf?
                     |
                     |The bottomless shelf can hold 6 bottomless bundles or 6 bottomless bundles, or a mix of them.
                     |""".stripMargin)
                 .addPage("*Don't be shelfish!*")
                 .addPage(
                   """
                     |A bottomless shelf will remember what items and fluids were inside its items, meaning you can use it as a filter.
                     |""".stripMargin)
                 .save()
    InfoCollector.instance.addGuidebookEntry("tool_container")
                 .addPage(
                   """
                     |On my travels I find myself carrying half a tool shed with me.
                     |
                     |I can mitigate this somewhat by making a tool container to keep all my tools in.
                     |""".stripMargin)
                 .addPage("*Don't ask how they all fit*")
                 .save()
    
