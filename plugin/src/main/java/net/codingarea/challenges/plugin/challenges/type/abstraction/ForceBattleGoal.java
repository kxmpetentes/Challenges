package net.codingarea.challenges.plugin.challenges.type.abstraction;

import net.anweisen.utilities.bukkit.utils.animation.SoundSample;
import net.anweisen.utilities.common.config.Document;
import net.anweisen.utilities.common.config.document.GsonDocument;
import net.codingarea.challenges.plugin.ChallengeAPI;
import net.codingarea.challenges.plugin.content.Message;
import net.codingarea.challenges.plugin.content.Prefix;
import net.codingarea.challenges.plugin.management.menu.MenuType;
import net.codingarea.challenges.plugin.management.menu.generator.categorised.SettingCategory;
import net.codingarea.challenges.plugin.management.scheduler.policy.TimerPolicy;
import net.codingarea.challenges.plugin.management.scheduler.task.ScheduledTask;
import net.codingarea.challenges.plugin.management.scheduler.task.TimerTask;
import net.codingarea.challenges.plugin.management.scheduler.timer.TimerStatus;
import net.codingarea.challenges.plugin.utils.item.ItemBuilder;
import net.codingarea.challenges.plugin.utils.misc.InventoryUtils;
import net.codingarea.challenges.plugin.utils.misc.NameHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author sehrschlechtYT | https://github.com/sehrschlechtYT
 * @since 2.2.0
 */
public abstract class ForceBattleGoal<T> extends MenuGoal {

	protected final Map<UUID, Integer> jokerUsed = new HashMap<>();
	protected final Map<UUID, List<T>> foundTargets = new HashMap<>();
	protected final Map<UUID, T> currentTarget = new HashMap<>();
	protected T[] targetsPossibleToFind;
	private ItemStack jokerItem;

	public ForceBattleGoal(@NotNull MenuType menu, @NotNull Message title) {
		super(menu, title);
		setCategory(SettingCategory.FORCE_BATTLE);
		;

		registerSetting("jokers", new NumberSubSetting(
				() -> new ItemBuilder(Material.BARRIER, Message.forName("item-force-battle-goal-jokers")),
				value -> null,
				value -> "§e" + value,
				1,
				32,
				5
		));
		registerSetting("showScoreboard", new BooleanSubSetting(
				() -> new ItemBuilder(Material.BOOK, Message.forName("item-force-battle-show-scoreboard")),
				true
		));
		registerSetting("dupedTargets", new BooleanSubSetting(
				() -> new ItemBuilder(Material.PAPER, Message.forName("item-force-battle-duped-targets")),
				true
		));
	}

	@Override
	protected void onEnable() {
		jokerItem = new ItemBuilder(Material.BARRIER, "§cJoker").build();

		targetsPossibleToFind = getTargetsPossibleToFind();

		broadcastFiltered(this::updateJokersInInventory);
		broadcastFiltered(this::setRandomTargetIfCurrentlyNone);

		scoreboard.setContent((board, player) -> {
			List<Player> ingamePlayers = ChallengeAPI.getIngamePlayers();
			int emptyLinesAvailable = 15 - ingamePlayers.size();

			if (emptyLinesAvailable > 0) {
				board.addLine("");
				emptyLinesAvailable--;
			}

			for (int i = 0; i < ingamePlayers.size() && i < 15; i++) {
				Player ingamePlayer = ingamePlayers.get(i);
				T target = currentTarget.get(ingamePlayer.getUniqueId());
				String display = target == null ? Message.forName("none").asString() : getTargetName(target);
				board.addLine(NameHelper.getName(ingamePlayer) + " §8» §e" + display);
			}

			if (emptyLinesAvailable > 0) {
				board.addLine("");
			}
		});
		if (showScoreboard()) {
			scoreboard.show();
		}
	}

	@Override
	protected void onDisable() {
		if (jokerItem == null) return; // Disable through plugin disable
		broadcastFiltered(this::updateJokersInInventory);
		jokerItem = null;
		scoreboard.hide();
		targetsPossibleToFind = null;
	}

	protected abstract T[] getTargetsPossibleToFind();

	private void updateJokersInInventory(Player player) {
		PlayerInventory inventory = player.getInventory();
		boolean enabled = isEnabled() && ChallengeAPI.isStarted();
		int usableJokers = getUsableJokers(player.getUniqueId());

		int jokersInInventory = 0;

		LinkedList<ItemStack> itemStacks = new LinkedList<>(Arrays.asList(inventory.getContents()));
		itemStacks.add(player.getItemOnCursor());
		for (ItemStack itemStack : itemStacks) {
			if (jokerItem.isSimilar(itemStack)) {
				if (enabled) {
					jokersInInventory += itemStack.getAmount();
					if (jokersInInventory >= usableJokers) {
						int jokersToSubtract = jokersInInventory - usableJokers;
						jokersInInventory -= jokersToSubtract;
						itemStack.setAmount(itemStack.getAmount() - jokersToSubtract);
					}
				} else {
					inventory.removeItem(itemStack);
				}
			}

		}

		if (enabled) {
			if (jokersInInventory < usableJokers) {
				ItemStack clone = jokerItem.clone();
				clone.setAmount(usableJokers - jokersInInventory);
				InventoryUtils.dropOrGiveItem(inventory, player.getLocation(), clone);
			}
		}

	}

	@Override
	public void loadGameState(@NotNull Document document) {
		this.jokerUsed.clear();
		this.currentTarget.clear();
		this.foundTargets.clear();

		List<Document> players = document.getDocumentList("players");
		for (Document player : players) {
			UUID uuid = player.getUUID("uuid");

			T currentTarget = getTargetFromDocument(player, "currentTarget");

			if (currentTarget != null) {
				this.currentTarget.put(uuid, currentTarget);
			}
			List<T> foundItems = getListFromDocument(player, "foundTargets");
			this.foundTargets.put(uuid, foundItems);

			int jokerUsed = player.getInt("jokerUsed");
			this.jokerUsed.put(uuid, jokerUsed);
		}

		if (isEnabled()) {
			if (ChallengeAPI.isStarted()) {
				broadcastFiltered(this::setRandomTargetIfCurrentlyNone);
			}
			scoreboard.update();
			broadcastFiltered(this::updateJokersInInventory);
		}
	}

	@Override
	public void writeGameState(@NotNull Document document) {
		List<Document> playersDocuments = new LinkedList<>();
		for (Map.Entry<UUID, T> entry : currentTarget.entrySet()) {
			List<T> foundItems = this.foundTargets.get(entry.getKey());
			int jokerUsed = this.jokerUsed.getOrDefault(entry.getKey(), 0);
			GsonDocument playerDocument = new GsonDocument();
			playerDocument.set("uuid", entry.getKey());
			setTargetInDocument(playerDocument, "currentTarget", entry.getValue());
			if (foundItems != null) {
				setFoundListInDocument(playerDocument, "foundTargets", foundItems);
			}
			playerDocument.set("jokerUsed", jokerUsed);
			playersDocuments.add(playerDocument);
		}

		document.set("players", playersDocuments);
	}

	public void setTargetInDocument(Document document, String key, T target) {
		document.set(key, target);
	}

	public void setFoundListInDocument(Document document, String key, List<T> target) {
		document.set(key, target);
	}

	public abstract T getTargetFromDocument(Document document, String key);

	public abstract List<T> getListFromDocument(Document document, String key);

	public void setRandomTargetIfCurrentlyNone(Player player) {
		if (currentTarget.containsKey(player.getUniqueId())) {
			return;
		}
		setRandomTarget(player);
	}

	public void setRandomTarget(Player player) {

		LinkedList<T> list = new LinkedList<>(Arrays.asList(targetsPossibleToFind));
		if (!getSetting("dupedTargets").getAsBoolean()) {
			list.removeAll(foundTargets.getOrDefault(player.getUniqueId(), new LinkedList<>()));
		}

		if (!list.isEmpty()) {
			T target = globalRandom.choose(list);
			currentTarget.put(player.getUniqueId(), target);
			getNewTargetMessage()
					.send(player, Prefix.CHALLENGES, getTargetMessageReplacement(target));
			SoundSample.PLING.play(player);
		} else {
			currentTarget.remove(player.getUniqueId());
			SoundSample.BASS_OFF.play(player);
		}
		scoreboard.update();

	}

	protected abstract Message getNewTargetMessage();

	protected abstract Message getTargetFoundMessage();

	public void handleTargetFound(Player player) {
		T foundTarget = currentTarget.get(player.getUniqueId());
		if (foundTarget != null) {
			List<T> list = foundTargets
					.computeIfAbsent(player.getUniqueId(), uuid -> new LinkedList<>());
			list.add(foundTarget);
			getTargetFoundMessage().send(player, Prefix.CHALLENGES, getTargetMessageReplacement(foundTarget));
		}
		setRandomTarget(player);
	}

	public abstract Object getTargetMessageReplacement(T target);

	public abstract String getTargetName(T target);

	@Override
	public void getWinnersOnEnd(@NotNull List<Player> winners) {

		Bukkit.getScheduler().runTask(plugin, () -> {
			int place = 0;
			int placeValue = -1;

			List<Map.Entry<UUID, List<T>>> list = foundTargets.entrySet().stream()
					.sorted(Comparator.comparingInt(value -> value.getValue().size()))
					.collect(Collectors.toList());
			Collections.reverse(list);

			getLeaderboardTitleMessage().broadcast(Prefix.CHALLENGES);

			for (Map.Entry<UUID, List<T>> entry : list) {
				if (entry.getValue().size() != placeValue) {
					place++;
					placeValue = entry.getValue().size();
				}
				UUID uuid = entry.getKey();
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				ChatColor color = getPlaceColor(place);
				Message.forName("force-battle-leaderboard-entry")
						.broadcast(Prefix.CHALLENGES, color, place, NameHelper.getName(offlinePlayer), entry.getValue().size());
			}

		});

	}

	public void sendResult(@NotNull Player player) {
		Bukkit.getScheduler().runTask(plugin, () -> {
			int place = 0;
			int placeValue = -1;

			List<Map.Entry<UUID, List<T>>> list = foundTargets.entrySet().stream()
					.sorted(Comparator.comparingInt(value -> value.getValue().size()))
					.collect(Collectors.toList());
			Collections.reverse(list);

			getLeaderboardTitleMessage().broadcast(Prefix.CHALLENGES);

			for (Map.Entry<UUID, List<T>> entry : list) {
				if (entry.getValue().size() != placeValue) {
					place++;
					placeValue = entry.getValue().size();
				}
				UUID uuid = entry.getKey();
				OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
				ChatColor color = getPlaceColor(place);
				Message.forName("force-battle-leaderboard-entry")
						.send(player, Prefix.CHALLENGES, color, place, NameHelper.getName(offlinePlayer), entry.getValue().size());
			}

		});
	}

	protected abstract Message getLeaderboardTitleMessage();

	protected ChatColor getPlaceColor(int place) {
		switch (place) {
			case 1:
				return ChatColor.GOLD;
			case 2:
				return ChatColor.YELLOW;
			case 3:
				return ChatColor.RED;
			default:
				return ChatColor.GRAY;
		}
	}

	private int getUsableJokers(UUID uuid) {
		return Math.max(0, getJokers() - jokerUsed.getOrDefault(uuid, 0));
	}

	public void handleJokerUse(Player player) {
		if (currentTarget.get(player.getUniqueId()) == null) {
			setRandomTargetIfCurrentlyNone(player);
			return;
		}
		int jokerUsed = this.jokerUsed.getOrDefault(player.getUniqueId(), 0);
		jokerUsed++;
		this.jokerUsed.put(player.getUniqueId(), jokerUsed);
		handleTargetFound(player);
		updateJokersInInventory(player);
	}

	@TimerTask(status = TimerStatus.RUNNING, async = false)
	public void onStart() {
		broadcastFiltered(this::setRandomTargetIfCurrentlyNone);
		broadcastFiltered(this::updateJokersInInventory);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(PlayerJoinEvent event) {
		if (!shouldExecuteEffect()) return;
		if (ignorePlayer(event.getPlayer())) return;
		setRandomTargetIfCurrentlyNone(event.getPlayer());
		updateJokersInInventory(event.getPlayer());
	}

	@ScheduledTask(ticks = 1, async = false, timerPolicy = TimerPolicy.ALWAYS)
	public void onTick() {
		if (!isEnabled()) return;

		if (!scoreboard.isShown() && showScoreboard()) {
			scoreboard.show();
		} else if (scoreboard.isShown() && !showScoreboard()) {
			scoreboard.hide();
		}
		broadcastFiltered(this::updateJokersInInventory);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event) {
		if (!shouldExecuteEffect()) return;
		if (ignorePlayer(event.getPlayer())) return;
		if (event.getAction() != Action.RIGHT_CLICK_AIR
				&& event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (jokerItem.isSimilar(event.getItem())) {
			handleJokerUse(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDropItem(PlayerDropItemEvent event) {
		if (!shouldExecuteEffect()) return;
		if (ignorePlayer(event.getPlayer())) return;
		if (jokerItem.isSimilar(event.getItemDrop().getItemStack())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onDeath(PlayerDeathEvent event) {
		if (!shouldExecuteEffect()) return;
		if (ignorePlayer(event.getEntity())) return;
		event.getDrops().removeIf(itemStack -> jokerItem.isSimilar(itemStack));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onRespawn(PlayerRespawnEvent event) {
		if (!shouldExecuteEffect()) return;
		if (ignorePlayer(event.getPlayer())) return;
		Bukkit.getScheduler().runTask(plugin, () -> {
			updateJokersInInventory(event.getPlayer());
		});
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent event) {
		if (!shouldExecuteEffect()) return;
		if (ignorePlayer(event.getPlayer())) return;
		if (jokerItem.isSimilar(event.getItemInHand())) {
			event.setCancelled(true);
		}
	}

	private int getJokers() {
		return getSetting("jokers").getAsInt();
	}

	private boolean showScoreboard() {
		return getSetting("showScoreboard").getAsBoolean();
	}

}
