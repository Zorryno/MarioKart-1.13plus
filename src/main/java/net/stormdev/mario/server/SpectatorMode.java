package net.stormdev.mario.server;

import net.stormdev.mario.mariokart.MarioKart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

//Todo Bugfixing
public class SpectatorMode implements Listener {
    private static final ItemStack lobbyItem;
    private static final ItemStack teleporter;

    private static List<Player> specs = new ArrayList<>();

    private Inventory teleporterInventory;

    static {
        lobbyItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta im = lobbyItem.getItemMeta();
        im.setDisplayName(ChatColor.DARK_RED + "Exit to lobby");
        im.setLore(Arrays.asList(new String[]{ChatColor.GRAY + "Right click to use"}));
        lobbyItem.setItemMeta(im);

        teleporter = new ItemStack(Material.COMPASS);
        ItemMeta meta = teleporter.getItemMeta();
        meta.setDisplayName(MarioKart.msgs.get("server.spectator.teleporterTitle"));
        teleporter.setItemMeta(meta);
    }

    public SpectatorMode() {
        Bukkit.getPluginManager().registerEvents(this, MarioKart.plugin);
    }

    public void add(Player player) {
        if (isSpectating(player)) {
            return;
        }

        specs.add(player);

        Bukkit.getScheduler().scheduleSyncDelayedTask(MarioKart.plugin, () -> {
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);

            PotionEffect invisibility = new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 9999, true, false, false);
            player.addPotionEffect(invisibility);

            hide(player);
            player.closeInventory();
            spectateInv(player);
            updateTeleporterInventory();

            getSpecTeam().addEntry(player.getName());
        }, 1);
    }

    private void hide(Player player) {
        List<Player> alivePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        alivePlayers.removeAll(getSpecs());

        for (Player alive : alivePlayers) {
            alive.hidePlayer(MarioKart.plugin, player);
        }
        for (Player spec : specs) {
            player.showPlayer(MarioKart.plugin, spec);
        }
    }

    private void show(Player player) {
        Bukkit.getScheduler().callSyncMethod(MarioKart.plugin, () -> {
            for (Player allPlayers : new ArrayList<>(Bukkit.getOnlinePlayers())) {
                allPlayers.showPlayer(MarioKart.plugin, player);
            }
            for (Player spec : specs) {
                player.hidePlayer(MarioKart.plugin, spec);
            }
            return null;
        });
    }

    public void endSpectating() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player player : players) {
            if (isSpectating(player)) {
                stopSpectating(player);
            }
        }
    }

    public void stopSpectating(final Player player) {
        Bukkit.getScheduler().callSyncMethod(MarioKart.plugin, () -> {
            player.getInventory().clear();
            Bukkit.getScheduler().callSyncMethod(MarioKart.plugin, () -> {
                for (PotionEffect effect : player.getActivePotionEffects())
                    player.removePotionEffect(effect.getType());
                return null;
            });

            specs.remove(player);
            show(player);

            player.setAllowFlight(false);
            player.setGameMode(GameMode.SURVIVAL);

            Bukkit.getScheduler().scheduleSyncDelayedTask(MarioKart.plugin, () -> player.teleport(FullServerManager.get().lobbyLoc));
            getSpecTeam().removeEntry(player.getName());
            return null;
        });
    }

    public boolean isSpectating(Player player) {
        return specs.contains(player);
    }

    /**
     * Read only
     *
     * @return A List of Players in Spectator Mode
     */
    public static List<Player> getSpecs() {
        return new ArrayList<>(specs);
    }

    /**
     * Gets the Spectator Team
     *
     * @return the Spectator Team
     */
    private static Team getSpecTeam() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return null;

        Scoreboard boardSpec = manager.getMainScoreboard();

        Team teamSpec = boardSpec.getTeam("0Spectator") != null ? boardSpec.getTeam("0Spectator") : boardSpec.registerNewTeam("0Spectator");
        teamSpec.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        teamSpec.setAllowFriendlyFire(false);
        teamSpec.setCanSeeFriendlyInvisibles(true);
        teamSpec.setDisplayName("Ghost");
        teamSpec.setColor(ChatColor.DARK_GRAY);
        teamSpec.setPrefix(ChatColor.DARK_GRAY.toString());

        return teamSpec;
    }

    private void spectateInv(final Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, teleporter);
        player.getInventory().setItem(8, lobbyItem);
        player.updateInventory();
    }

    public void updateTeleporterInventory() {
        List<Player> alivePlayers = new ArrayList(Bukkit.getOnlinePlayers());
        alivePlayers.removeAll(specs);

        int inventorySize = specs.size() + 9 - specs.size() % 9;
        teleporterInventory = Bukkit.createInventory(null, inventorySize, MarioKart.msgs.get("server.spectator.teleporterTitle"));
        int i = 0;
        for (Player player : alivePlayers) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(player);
            meta.setDisplayName(player.getName());
            item.setItemMeta(meta);
            teleporterInventory.setItem(i, item);
            i++;
        }
        for (Player player : specs)
            player.updateInventory();
    }

    @EventHandler
    void onJoin(PlayerJoinEvent event) {
        if (isSpectating(event.getPlayer())) {
            stopSpectating(event.getPlayer());
        }
    }

    @EventHandler
    void useExit(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack inHand = event.getItem();
        if (inHand == null || !isSpectating(player)) {
            return;
        }
        if (inHand.isSimilar(lobbyItem)) {
            player.getInventory().clear();
            stopSpectating(player);
            player.teleport(FullServerManager.get().lobbyLoc); //For when they next login
            player.sendMessage(ChatColor.GRAY + "Teleporting...");
            FullServerManager.get().sendToLobby(player);
        } else if (inHand.isSimilar(teleporter)) {
            player.openInventory(teleporterInventory);
        }
    }

    @EventHandler
    void dropItem(PlayerDropItemEvent event) {
        if (isSpectating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void swapHands(PlayerSwapHandItemsEvent event) {
        if (isSpectating(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    void invClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!isSpectating(player)) {
            return;
        }

        event.setCancelled(true);
        player.updateInventory();

        if (event.getCurrentItem() != null) {
            Player target = Bukkit.getPlayer(event.getCurrentItem().getItemMeta().getDisplayName());
            if (target == null)
                return;

            player.teleport(target);
            player.sendMessage(MarioKart.msgs.get("server.spectator.teleportToPlayer").replace("%name%", target.getName()));
        }
    }
}
