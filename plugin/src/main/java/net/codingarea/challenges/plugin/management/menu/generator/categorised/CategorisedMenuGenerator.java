package net.codingarea.challenges.plugin.management.menu.generator.categorised;

import net.anweisen.utilities.bukkit.utils.animation.SoundSample;
import net.anweisen.utilities.bukkit.utils.menu.MenuPosition;
import net.codingarea.challenges.plugin.Challenges;
import net.codingarea.challenges.plugin.challenges.type.IChallenge;
import net.codingarea.challenges.plugin.content.Message;
import net.codingarea.challenges.plugin.management.menu.InventoryTitleManager;
import net.codingarea.challenges.plugin.management.menu.generator.implementation.SettingsMenuGenerator;
import net.codingarea.challenges.plugin.utils.item.ItemBuilder;
import net.codingarea.challenges.plugin.utils.misc.InventoryUtils;
import org.bukkit.ChatColor;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author KxmischesDomi | https://github.com/kxmischesdomi
 * @since 1.0
 */
public class CategorisedMenuGenerator extends SettingsMenuGenerator {

	private final Map<ChallengeCategory, SettingsMenuGenerator> categories = new HashMap<>();

	@Override
	public void addChallengeToCache(@NotNull IChallenge challenge) {
		ChallengeCategory category = challenge.getCategory() != null ? challenge.getCategory() : ChallengeCategory.MISC;
		if (!categories.containsKey(challenge.getCategory())) {
			categories.computeIfAbsent(category, challengeCategory -> {
				SettingsMenuGenerator generator = new CategorisedSettingsMenuGenerator(this, category);
				generator.setMenuType(getMenuType());
				return generator;
			});
		}
		categories.get(category).addChallengeToCache(challenge);
	}

	@Override
	public void generateInventories() {
		super.generateInventories();

	}

	@Override
	public void generatePage(@NotNull Inventory inventory, int page) {


		int i = 0;
		for (Map.Entry<ChallengeCategory, SettingsMenuGenerator> entry : getCategoriesForPage(page)) {
			ChallengeCategory category = entry.getKey();
			SettingsMenuGenerator generator = entry.getValue();

			ItemBuilder builder = category.getDisplayItem();
			long activatedCount = generator.getChallenges().stream().filter(IChallenge::isEnabled).count();
			builder.appendLore("", Message.forName("lore-category-activated").asString(activatedCount, generator.getChallenges().size()));

			int slot = i + 10;
			if (i >= 7) slot += 2;
			inventory.setItem(slot, builder.build());
			i++;
		}

	}

	@Override
	public void updateItem(IChallenge challenge) {
		generateInventories();
	}

	@Override
	public int getPagesCount() {
		return (int) (Math.ceil((double) categories.size() / (getSlots().length*2)));
	}
//
//	@Override
//	public int getSize() {
//		return 3*9;
//	}
//
//	@Override
//	public int[] getNavigationSlots(int page) {
//		return new int[] { 18, 26 };
//	}

	@Override
	public MenuPosition getMenuPosition(int page) {
		return info -> {
			if (InventoryUtils.handleNavigationClicking(this,
					getNavigationSlots(page),
					page,
					info,
					() -> Challenges.getInstance().getMenuManager().openGUIInstantly(info.getPlayer()))) {
				return;
			}

			int i = 0;
			for (Map.Entry<ChallengeCategory, SettingsMenuGenerator> entry : getCategoriesForPage(page)) {
				int slot = i + 10;
				if (i >= 7) slot += 2;
				if (slot == info.getSlot()) {
					SettingsMenuGenerator generator = entry.getValue();
					generator.open(info.getPlayer(), 0);
					return;
				}
				i++;
			}

			SoundSample.CLICK.play(info.getPlayer());
		};
	}

	private List<Map.Entry<ChallengeCategory, SettingsMenuGenerator>> getCategoriesForPage(int page) {

		List<Map.Entry<ChallengeCategory, SettingsMenuGenerator>> list = categories.entrySet()
				.stream()
				.sorted(Comparator.comparingInt(value -> value.getKey().getPriority()))
				.collect(Collectors.toList());

		int startIndex = page*14;
		int endIndex = Math.min(page*14+14, categories.size());

		return list.subList(startIndex, endIndex);
	}

	public static class CategorisedSettingsMenuGenerator extends SettingsMenuGenerator {

		private final CategorisedMenuGenerator generator;
		private final ChallengeCategory category;

		public CategorisedSettingsMenuGenerator(CategorisedMenuGenerator generator, ChallengeCategory category) {
			this.generator = generator;
			this.category = category;
			this.onLeaveClick = player -> {
				generator.open(player, 0);
			};
		}

		@Override
		protected String getTitle(int page) {
			String[] strings = category.getMessageSupplier().get().asArray();
			String display = strings.length == 0 ? "" : ChatColor.stripColor(strings[0]);
			return InventoryTitleManager.getTitle(getMenuType(), display, String.valueOf(page+1));
		}

		@Override
		public void updateItem(IChallenge challenge) {
			generator.updateItem(challenge);
			super.updateItem(challenge);
		}

	}

}
