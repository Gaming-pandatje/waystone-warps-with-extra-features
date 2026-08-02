package dev.mizarc.waystonewarps.interaction.menus.admin

import dev.mizarc.waystonewarps.application.actions.groups.RenameWarpGroup
import dev.mizarc.waystonewarps.application.actions.groups.RenameWarpGroupResult
import dev.mizarc.waystonewarps.domain.warps.WarpGroup
import dev.mizarc.waystonewarps.interaction.localization.LocalizationKeys
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import dev.mizarc.waystonewarps.interaction.menus.Menu
import dev.mizarc.waystonewarps.interaction.menus.MenuNavigator
import dev.mizarc.waystonewarps.interaction.utils.name
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WarpGroupRenameMenu(
    private val player: Player,
    private val menuNavigator: MenuNavigator,
    private val group: WarpGroup,
    private val localizationProvider: LocalizationProvider
) : Menu, KoinComponent {
    private val renameWarpGroup: RenameWarpGroup by inject()
    private val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("WaystoneWarps")!!

    override fun open() {
        val bookItem = ItemStack(Material.BOOKSHELF).name(group.name)

        AnvilGUI.Builder()
            .plugin(plugin)
            .title(localizationProvider.get(player.uniqueId, LocalizationKeys.MENU_WARP_GROUP_RENAME_TITLE))
            .itemLeft(bookItem)
            .text(group.name)
            .onClick { slot, state ->
                if (slot != AnvilGUI.Slot.OUTPUT) return@onClick listOf()
                val input = state.text.trim()
                when (renameWarpGroup.execute(group.id, input)) {
                    RenameWarpGroupResult.SUCCESS -> {
                        menuNavigator.goBack()
                        listOf(AnvilGUI.ResponseAction.close())
                    }
                    RenameWarpGroupResult.NAME_BLANK -> {
                        listOf(AnvilGUI.ResponseAction.replaceInputText(group.name))
                    }
                    RenameWarpGroupResult.NOT_FOUND -> {
                        menuNavigator.goBack()
                        listOf(AnvilGUI.ResponseAction.close())
                    }
                    RenameWarpGroupResult.NAME_TAKEN -> {
                        player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.MENU_WARP_GROUP_RENAME_NAME_TAKEN)}")
                        listOf(AnvilGUI.ResponseAction.replaceInputText(group.name))
                    }
                }
            }
            .onClose { menuNavigator.goBack() }
            .open(player)
    }
}
