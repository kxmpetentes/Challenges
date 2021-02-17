package net.codingarea.challenges.plugin.utils.item;

import net.codingarea.challenges.plugin.lang.Message;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author anweisen | https://github.com/anweisen
 * @since 1.0
 */
public class ItemBuilder {

	public static final ItemStack FILL_ITEM     = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName("§0").build(),
								  FILL_ITEM_2   = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setName("§0").build(),
								  AIR           = new ItemStack(Material.AIR);

	protected ItemStack item;
	protected ItemMeta meta;

	public ItemBuilder(@Nonnull ItemStack item) {
		this.item = item;
		this.meta = item.getItemMeta();
	}

	public ItemBuilder(@Nonnull Material material) {
		this(new ItemStack(material));
	}

	@Deprecated
	public ItemBuilder(@Nonnull Material material, @Nonnull String name) {
		this(new ItemStack(material));
		setName(name);
	}

	@Deprecated
	public ItemBuilder(@Nonnull Material material, @Nonnull String name, @Nonnull String... lore) {
		this(new ItemStack(material));
		setName(name);
		setLore(lore);
	}

	@Deprecated
	public ItemBuilder(@Nonnull Material material, @Nonnull String name, int amount) {
		this(new ItemStack(material));
		setName(name);
		setAmount(amount);
	}

	@Nonnull
	public ItemMeta getMeta() {
		return meta == null ? meta = item.getItemMeta() : meta;
	}

	@Nonnull
	public <M> M getCastedMeta() {
		return (M) meta;
	}

	@Nonnull
	public ItemBuilder setLore(@Nonnull Message message) {
		return setLore(message.asArray());
	}

	@Nonnull
	public ItemBuilder setLore(@Nonnull List<String> lore) {
		getMeta().setLore(lore);
		return this;
	}

	@Nonnull
	public ItemBuilder setLore(@Nonnull String... lore) {
		return setLore(Arrays.asList(lore));
	}

	@Nonnull
	public ItemBuilder appendLore(@Nonnull String... lore) {
		return appendLore(Arrays.asList(lore));
	}

	@Nonnull
	public ItemBuilder appendLore(@Nonnull Collection<String> lore) {
		List<String> newLore = getMeta().getLore();
		if (newLore == null) newLore = new ArrayList<>();
		newLore.addAll(lore);
		setLore(newLore);
		return this;
	}

	@Nonnull
	public ItemBuilder setName(@Nullable String name) {
		getMeta().setDisplayName(name);
		return this;
	}

	@Nonnull
	public ItemBuilder setName(@Nullable Object name) {
		return setName(name == null ? null : name.toString());
	}

	@Nonnull
	public ItemBuilder name(@Nullable Object name) {
		return setName(name);
	}

	@Nonnull
	public ItemBuilder addEnchantment(@Nonnull Enchantment enchantment, int level) {
		getMeta().addEnchant(enchantment, level, true);
		return this;
	}

	@Nonnull
	public ItemBuilder enchant(@Nonnull Enchantment enchantment, int level) {
		return addEnchantment(enchantment, level);
	}

	@Nonnull
	public ItemBuilder addFlag(@Nonnull ItemFlag... flags) {
		getMeta().addItemFlags(flags);
		return this;
	}

	@Nonnull
	public ItemBuilder removeFlag(@Nonnull ItemFlag... flags) {
		getMeta().removeItemFlags(flags);
		return this;
	}

	@Nonnull
	public ItemBuilder hideAttributes() {
		return addFlag(ItemFlag.values());
	}

	@Nonnull
	public ItemBuilder showAttributes() {
		return removeFlag(ItemFlag.values());
	}

	@Nonnull
	public ItemBuilder setUnbreakable(boolean unbreakable) {
		getMeta().setUnbreakable(unbreakable);
		return this;
	}

	@Nonnull
	public ItemBuilder unbreakable() {
		return setUnbreakable(true);
	}

	@Nonnull
	public ItemBuilder breakable() {
		return setUnbreakable(false);
	}

	@Nonnull
	public ItemBuilder setAmount(int amount) {
		item.setAmount(amount);
		return this;
	}

	@Nonnull
	public ItemBuilder amount(int amount) {
		return setAmount(amount);
	}

	@Nonnull
	public ItemBuilder setDamage(int damage) {
		this.<Damageable>getCastedMeta().setDamage(damage);
		return this;
	}

	@Nonnull
	public ItemBuilder damage(int damage) {
		return setDamage(damage);
	}

	@Nonnull
	public ItemStack build() {
		item.setItemMeta(getMeta()); // Call to getter to prevent null value
		return item;
	}

	@Nonnull
	public ItemStack toItem() {
		return build();
	}

	public static class BannerBuilder extends ItemBuilder {

		public BannerBuilder(@Nonnull Material material) {
			super(material);
		}

		@Nonnull
		public BannerBuilder addPattern(@Nonnull BannerPattern pattern, @Nonnull DyeColor color) {
			getMeta().addPattern(new Pattern(color, pattern.getPatternType()));
			return this;
		}

		@Nonnull
		@Override
		public BannerMeta getMeta() {
			return getCastedMeta();
		}

	}

	public static class SkullBuilder extends ItemBuilder {

		public SkullBuilder() {
			super(Material.PLAYER_HEAD);
		}

		public SkullBuilder(@Nonnull String owner) {
			super(Material.PLAYER_HEAD);
			setOwner(owner);
		}

		public SkullBuilder(@Nonnull String owner, @Nonnull String name, @Nonnull String... lore) {
			super(Material.PLAYER_HEAD, name, lore);
			setOwner(owner);
		}

		@Nonnull
		@SuppressWarnings("deprecation")
		public SkullBuilder setOwner(@Nonnull String owner) {
			getMeta().setOwner(owner);
			return this;
		}

		@Nonnull
		@Override
		public SkullMeta getMeta() {
			return getCastedMeta();
		}

	}

	public static class PotionBuilder extends ItemBuilder {

		public PotionBuilder(@Nonnull Material material) {
			super(material);
		}

		@Nonnull
		public PotionBuilder addEffect(@Nonnull PotionEffect effect) {
			getMeta().addCustomEffect(effect, true);
			return this;
		}

		@Nonnull
		public PotionBuilder setColor(@Nonnull Color color) {
			getMeta().setColor(color);
			return this;
		}

		@Nonnull
		@Override
		public PotionMeta getMeta() {
			return getCastedMeta();
		}

	}

	public static class LeatherArmorBuilder extends ItemBuilder {

		public LeatherArmorBuilder(@Nonnull Material material) {
			super(material);
		}

		@Nonnull
		public LeatherArmorBuilder setColor(@Nonnull Color color) {
			getMeta().setColor(color);
			return this;
		}

		@Nonnull
		@Override
		public LeatherArmorMeta getMeta() {
			return getCastedMeta();
		}

	}

}
