package net.codingarea.challenges.plugin.challenges.implementation.challenge;

import net.anweisen.utilities.bukkit.utils.item.ItemUtils;
import net.anweisen.utilities.bukkit.utils.misc.BukkitReflectionUtils;
import net.codingarea.challenges.plugin.Challenges;
import net.codingarea.challenges.plugin.challenges.type.RandomizerSetting;
import net.codingarea.challenges.plugin.content.Message;
import net.codingarea.challenges.plugin.management.blocks.BlockDropManager;
import net.codingarea.challenges.plugin.management.blocks.BlockDropManager.DropPriority;
import net.codingarea.challenges.plugin.management.menu.MenuType;
import net.codingarea.challenges.plugin.utils.item.ItemBuilder;
import org.bukkit.Material;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author anweisen | https://github.com/anweisen
 * @since 2.0
 */
public class BlockRandomizerChallenge extends RandomizerSetting {

	public BlockRandomizerChallenge() {
		super(MenuType.CHALLENGES);
	}

	@Nonnull
	@Override
	public ItemBuilder createDisplayItem() {
		return new ItemBuilder(Material.MINECART, Message.forName("item-block-randomizer-challenge"));
	}

	@Override
	protected void reloadRandomization() {
		BlockDropManager manager = Challenges.getInstance().getBlockDropManager();

		List<Material> blocks = new ArrayList<>(Arrays.asList(Material.values()));
		blocks.removeIf(material -> !ItemUtils.isObtainableInSurvival(material) || !material.isBlock() || BukkitReflectionUtils.isAir(material));
		Collections.shuffle(blocks, random);

		List<Material> drops = new ArrayList<>(Arrays.asList(Material.values()));
		drops.removeIf(material -> !material.isItem() || !ItemUtils.isObtainableInSurvival(material));
		Collections.shuffle(drops, random);

		while (!blocks.isEmpty()) {
			Material block = blocks.remove(0);
			List<Material> items = new ArrayList<>();

			int addDrops = getMatches(blocks.size(), drops.size());
			for (int i = 0; i < addDrops && !drops.isEmpty(); i++) {
				items.add(drops.remove(0));
			}
			manager.setCustomDrops(block, items, DropPriority.RANDOMIZER);
		}

	}

	@Override
	protected void onDisable() {
		BlockDropManager manager = Challenges.getInstance().getBlockDropManager();
		manager.resetCustomDrops(DropPriority.RANDOMIZER);
	}

}
