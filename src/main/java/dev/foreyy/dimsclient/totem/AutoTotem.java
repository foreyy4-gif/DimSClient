package dev.foreyy.dimsclient.totem;

import dev.foreyy.dimsclient.DimsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

/** Moves a totem into the offhand. The swap is the same click the inventory screen sends. */
public final class AutoTotem {
	private AutoTotem() {
	}

	public static void tick(Minecraft client) {
		if (!DimsClient.CONFIG.totemEnabled || client.player == null || client.gameMode == null || client.gui.screen() != null) {
			return;
		}
		if (client.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			return;
		}
		if (!(client.player.containerMenu instanceof InventoryMenu menu)) {
			return;
		}
		int from = -1;
		for (int i = 0; i < menu.slots.size(); i++) {
			Slot slot = menu.slots.get(i);
			if (slot.getItem().is(Items.TOTEM_OF_UNDYING) && i != InventoryMenu.SHIELD_SLOT) {
				from = i;
				break;
			}
		}
		if (from < 0) {
			return;
		}
		client.gameMode.handleContainerInput(menu.containerId, from, 40, ContainerInput.SWAP, client.player);
	}
}
