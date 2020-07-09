package net.codingarea.challengesplugin.challengetypes;

import net.codingarea.challengesplugin.manager.events.ChallengeEditEvent;
import net.codingarea.challengesplugin.manager.menu.MenuType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author anweisen & Dominik
 * Challenges developed on 05-30-2020
 * https://github.com/anweisen
 * https://github.com/KxmischesDomi
 */

public abstract class AbstractChallenge {

	protected MenuType menu;

	public abstract void handleClick(ChallengeEditEvent event);

	public abstract ItemStack getItem();
	public abstract ItemStack getActivationItem();

	public abstract void setValues(int value);
	public abstract int toValue();

	public String getChallengeName() {
		return this.getClass().getSimpleName().toLowerCase().replace("setting", "").replace("challenge", "").replace("modifier", "");
	}

	public ItemStack getItemToShow() {
		return getItem();
	}

	public final void updateItem(Inventory inventory, int item) {
		inventory.setItem(item, getItemToShow());
		inventory.setItem(item + 9, getActivationItem());
	}

	public final ItemStack getNonAmountedItem() {
		try {
			ItemStack item = getItem();
			item.setAmount(1);
			return item;
		} catch (NullPointerException ignored) {
			return null;
		}
	}

	public final MenuType getMenu() {
		return menu;
	}

}
