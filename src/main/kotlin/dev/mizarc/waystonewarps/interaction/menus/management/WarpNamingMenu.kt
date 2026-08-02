package dev.mizarc.waystonewarps.interaction.menus.management

import dev.mizarc.waystonewarps.application.actions.world.CreateWarp
import dev.mizarc.waystonewarps.application.results.CreateWarpResult
import dev.mizarc.waystonewarps.infrastructure.mappers.toPosition3D
import dev.mizarc.waystonewarps.interaction.localization.LocalizationKeys
import dev.mizarc.waystonewarps.interaction.localization.LocalizationProvider
import dev.mizarc.waystonewarps.interaction.menus.Menu
import dev.mizarc.waystonewarps.interaction.menus.MenuNavigator
import dev.mizarc.waystonewarps.interaction.messaging.PrimaryColourPalette
import dev.mizarc.waystonewarps.interaction.utils.lore
import dev.mizarc.waystonewarps.interaction.utils.name
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.ItemStack
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
        if (isBedrockPlayer()) openBedrockForm() else openAnvilGui()
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
            plugin.logger.warning("Failed to open Bedrock form for ${player.name}: ${e.message}")
        }
    }

    private fun openAnvilGui() {
        val title = localizationProvider.get(player.uniqueId, LocalizationKeys.MENU_WARP_NAMING_TITLE)
        val lodestoneItem = ItemStack(Material.LODESTONE)
            .name("", PrimaryColourPalette.INFO.color!!)
            .lore(localizationProvider.get(
                player.uniqueId, LocalizationKeys.MENU_WARP_NAMING_ITEM_WARP_LORE,
                location.blockX.toString(), location.blockY.toString(), location.blockZ.toString()
            ))

        AnvilGUI.Builder()
            .plugin(plugin)
            .title(title)
            .itemLeft(lodestoneItem)
            .onClick { slot, state ->
                if (slot != AnvilGUI.Slot.OUTPUT) return@onClick listOf()
                val input = state.text.trim()
                submitName(input)
                listOf(AnvilGUI.ResponseAction.close())
            }
            .onClose { menuNavigator.goBack() }
            .open(player)
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
                if (!isBedrockPlayer()) openAnvilGui()
            }
            is CreateWarpResult.NameAlreadyExists -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_EXISTING, PrimaryColourPalette.FAILED.color!!)}")
                if (!isBedrockPlayer()) openAnvilGui() else openBedrockForm()
            }
            is CreateWarpResult.NameCannotBeBlank -> {
                player.sendMessage("§c${localizationProvider.get(player.uniqueId, LocalizationKeys.CONDITION_NAMING_BLANK, PrimaryColourPalette.FAILED.color!!)}")
                if (!isBedrockPlayer()) openAnvilGui() else openBedrockForm()
            }
        }
    }
}
