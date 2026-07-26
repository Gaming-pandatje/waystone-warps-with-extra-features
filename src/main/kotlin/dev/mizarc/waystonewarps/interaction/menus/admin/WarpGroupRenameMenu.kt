package dev.mizarc.waystonewarps.interaction.menus.admin

import dev.mizarc.waystonewarps.application.actions.groups.RenameWarpGroup
import dev.mizarc.waystonewarps.application.actions.groups.RenameWarpGroupResult
import dev.mizarc.waystonewarps.domain.warps.WarpGroup
import dev.mizarc.waystonewarps.interaction.localization.LocalizationKeys
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import dev.mizarc.waystonewarps.interaction.menus.Menu
import dev.mizarc.waystonewarps.interaction.menus.MenuNavigator
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
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
        openChatInput()
    }

    private fun openChatInput() {
        player.closeInventory()
        player.sendMessage("§6Type a new name for group §e${group.name}§6 in chat (or type §ccancel§6 to abort):")
        val listener = object : Listener {
            @EventHandler
            fun onChat(event: AsyncPlayerChatEvent) {
                if (event.player.uniqueId != player.uniqueId) return
                event.isCancelled = true
                HandlerList.unregisterAll(this)
                val input = event.message.trim()
                if (input.equals("cancel", ignoreCase = true)) {
                    plugin.server.scheduler.runTask(plugin, Runnable { menuNavigator.goBack() })
                    return
                }
                plugin.server.scheduler.runTask(plugin, Runnable { submitName(input) })
            }
        }
        plugin.server.pluginManager.registerEvents(listener, plugin)
    }

    private fun submitName(newName: String) {
        when (renameWarpGroup.execute(group.id, newName)) {
            RenameWarpGroupResult.SUCCESS -> menuNavigator.goBack()
            RenameWarpGroupResult.NAME_BLANK -> {
                player.sendMessage("§cName cannot be blank.")
                openChatInput()
            }
            RenameWarpGroupResult.NOT_FOUND -> menuNavigator.goBack()
            RenameWarpGroupResult.NAME_TAKEN -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.MENU_WARP_GROUP_RENAME_NAME_TAKEN)}")
                openChatInput()
            }
        }
    }
}
