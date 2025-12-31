package gay.menkissing.spectrumstorage.registries

import gay.menkissing.spectrumstorage.util.registry.InfoCollector

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
                 .addPage("Power increases its capacity eightfold each level.")
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
    InfoCollector.instance.addGuidebookEntry("bottomless_barrel")
                 .addPage(
                   """
                     |While bottomless shelves are great, I find myself making complicated systems to access many of them.
                     |It would be nice if I could compact them even more.
                     |
                     |The bottomless barrel does just that - it can hold up to 27 bottomless bundles or bottles.
                     |""".stripMargin
                 )
                 .addPage("*That's a lot of stuff*")
                 .addPage(
                   """
                     |Just like the bottomless shelf, the bottomless barrel will remember what items and fluids were inside its items, meaning you can use it as a filter
                     |""".stripMargin
                 )
                 .save()
    InfoCollector.instance.addGuidebookEntry("bottomless_amphora")
                 .addPage(
                   """
                     |I had thought that barrels could store a lot, but amphoras can store double that. So if I make a bottomless amphora...
                     |
                     |The bottomless amphora can store a whopping 54 bottomless bundles or bottles inside of it!
                     |""".stripMargin
                 )
                 .addPage("*This is getting absurd*")
                 .addPage(
                   """
                     |Just like the bottomless shelf and bottomless barrel before it, the bottomless amphora remembers what items and fluids were inside its items, meaning you can use it as a filter.
                     |""".stripMargin
                 )
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
    InfoCollector.instance.addGuidebookEntry("filter_chest")
                 .addPage(
                   """
                     |The machines I build to sort my vast catalog of items tend to get quite large. It would be nice to have something that easily filters them.
                     |
                     |The filter barrel can filter up to 18 unique items, but it can only hold 9 stacks of items. It will only allow insertion of items if they are in its filter.
                     |""".stripMargin)
                 .addPage("*Filter? I hardly know her*")
                 .save()
    InfoCollector.instance.addRawLang("book.spectrumstorage.added_by_spectrumstorage", "§oAdded by Spectrum Storage")
    InfoCollector.instance.bulkAddLangs(it => s"container.spectrumstorage.$it")
                 .lang("bottomless_barrel", "Bottomless Barrel")
                 .lang("bottomless_amphora", "Bottomless Amphora")
                 .lang("filter_chest", "Filter Barrel")
                 .save()

