package dev.mizarc.waystonewarps.interaction.menus.management

import dev.mizarc.waystonewarps.application.actions.world.CreateWarp
import dev.mizarc.waystonewarps.application.results.CreateWarpResult
import dev.mizarc.waystonewarps.infrastructure.mappers.toPosition3D
import dev.mizarc.waystonewarps.interaction.localization.LocalizationKeys
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import dev.mizarc.waystonewarps.interaction.menus.Menu
import dev.mizarc.waystonewarps.interaction.menus.MenuNavigator
import dev.mizarc.waystonewarps.interaction.messaging.PrimaryColourPalette
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WarpNamingMenu(
    private val player: Player,
    private val menuNavigator: MenuNavigator,
    private val location: Location
) : Menu, KoinComponent {
    private val createWarp: CreateWarp by inject()
    private val localizationProvider: LocalizationProvider by inject()
    private val plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("WaystoneWarps")!!

    private fun isBedrockPlayer(): Boolean {
        return try {
            org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.uniqueId)
        } catch (e: Exception) { false }
    }

    override fun open() {
        if (isBedrockPlayer()) openBedrockForm() else openChatInput()
    }

    private fun openBedrockForm() {
        try {
            val floodgateApi = org.geysermc.floodgate.api.FloodgateApi.getInstance()
            val form = org.geysermc.cumulus.form.CustomForm.builder()
                .title("Name your Waystone")
                .input("Enter name here", "")
                .validResultHandler { response ->
                    val input = response.asInput(0)?.trim() ?: return@validResultHandler
                    plugin.server.scheduler.runTask(plugin, Runnable { submitName(input) })
                }
                .build()
            floodgateApi.sendForm(player.uniqueId, form)
        } catch (e: Exception) {
            // Floodgate failed unexpectedly, do not open chat — just log
            plugin.logger.warning("Failed to open Bedrock form for ${player.name}: ${e.message}")
        }
    }

    private fun openChatInput() {
        player.closeInventory()
        player.sendMessage("§6Type the name for your Waystone in chat (or type §ccancel§6 to abort):")
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

    private fun submitName(inputName: String) {
        val belowLocation = location.clone().subtract(0.0, 1.0, 0.0)
        val result = createWarp.execute(
            player.uniqueId, inputName, location.toPosition3D(),
            location.world.uid, location.world.getBlockAt(belowLocation).type.name
        )
        when (result) {
            is CreateWarpResult.Success -> {
                location.world.playSound(player.location, Sound.BLOCK_VAULT_OPEN_SHUTTER, SoundCategory.BLOCKS, 1.0f, 1.0f)
                menuNavigator.openMenu(WarpManagementMenu(player, menuNavigator, result.warp))
            }
            is CreateWarpResult.LimitExceeded -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_LIMIT, PrimaryColourPalette.FAILED.color!!)}")
                if (!isBedrockPlayer()) openChatInput()
            }
            is CreateWarpResult.NameAlreadyExists -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_EXISTING, PrimaryColourPalette.FAILED.color!!)}")
                if (!isBedrockPlayer()) openChatInput() else openBedrockForm()
            }
            is CreateWarpResult.NameCannotBeBlank -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_BLANK, PrimaryColourPalette.FAILED.color!!)}")
                if (!isBedrockPlayer()) openChatInput() else openBedrockForm()
            }
        }
    }
}
