package net.codingarea.challenges.plugin.challenges.custom.settings.action.impl;

import net.anweisen.utilities.bukkit.utils.item.MaterialWrapper;
import net.codingarea.challenges.plugin.challenges.custom.settings.action.PlayerTargetAction;
import net.codingarea.challenges.plugin.challenges.implementation.setting.MaxHealthSetting;
import net.codingarea.challenges.plugin.challenges.type.abstraction.AbstractChallenge;
import net.codingarea.challenges.plugin.challenges.type.helper.ChallengeHelper;
import net.codingarea.challenges.plugin.challenges.type.helper.SubSettingsHelper;
import net.codingarea.challenges.plugin.content.Message;
import net.codingarea.challenges.plugin.management.menu.generator.categorised.ChallengeCategory;
import net.codingarea.challenges.plugin.utils.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * @author KxmischesDomi | https://github.com/kxmischesdomi
 * @since 2.1.0
 */
public class ModifyMaxHealthAction extends PlayerTargetAction {

	public ModifyMaxHealthAction(String name) {
		super(name, SubSettingsHelper.createEntityTargetSettingsBuilder(false, true)
				.createValueChild().fill(builder -> {
					builder.addModifierSetting("health_offset",
							new ItemBuilder(MaterialWrapper.RED_DYE,
									Message.forName("item-custom-action-max_health-offset")),
							0, -20, 20,
							integer -> "", integer -> "HP §8(§e" + (integer / 2f) + " §c❤§8)");
				}));
	}

	@Override
	public Material getMaterial() {
		return MaterialWrapper.RED_DYE;
	}

	@Override
	public void executeForPlayer(Player player, Map<String, String[]> subActions) {
		int healthOffset = Integer.parseInt(subActions.get("health_offset")[0]);
		MaxHealthSetting instance = AbstractChallenge.getFirstInstance(MaxHealthSetting.class);

		int oldMaxHealth = instance.getMaxHealth(player);
		if (oldMaxHealth <= 0) {
			ChallengeHelper.kill(player);
			return;
		}

		instance.addHealth(player, healthOffset);

		int newMaxHealth = instance.getMaxHealth(player);
		if (newMaxHealth <= 0) {
			ChallengeHelper.kill(player);
		}
	}

}
