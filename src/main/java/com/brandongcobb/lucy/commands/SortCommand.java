package com.brandongcobb.lucy.commands;

import com.brandongcobb.lucy.Lucy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * /sort command: combines stacks, sorts by name or material,
 * supports player vs. targeted chest (with per-chest security),
 * cooldowns, and permission checks.
 */
public class SortCommand implements CommandExecutor {

    private final Lucy plugin = Lucy.getInstance();

    // cooldowns per player
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    // inventory types
    private static final int INV_PLAYER       = 0;
    private static final int INV_CHEST        = 1;
    private static final int INV_DOUBLE_CHEST = 2;

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String label,
                             String[] args)
    {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        // cooldown
        long now = System.currentTimeMillis();
        int cdSeconds = plugin.getConfig().getInt("cooldown.cooldown-time");
        if (cooldowns.containsKey(uuid)) {
            long elapsed = (now - cooldowns.get(uuid)) / 1000;
            if (elapsed < cdSeconds) {
                player.sendMessage(color(plugin.getConfig()
                    .getString("cooldown.cooldown-hot-message")));
                return true;
            }
        }
        cooldowns.put(uuid, now);

        // determine inventory type
        int invType = getInvType(player, args);

        // chest permission & ownership
        if (invType != INV_PLAYER) {
            Location loc = player.getTargetBlock((Set<Material>)null, 5).getLocation();
            YamlConfiguration data = loadDataConfig();
            String key = locationKey(loc);
            String owner = data.getString(key, null);
            if (owner != null && !owner.equals(uuid.toString())) {
                player.sendMessage(color(plugin.getConfig()
                    .getString("messages.secure-message-owned")));
                return true;
            }
        }

        // perform sort
        Inventory inv = (invType == INV_PLAYER)
            ? player.getInventory()
            : ((Chest) player.getTargetBlock((Set<Material>)null, 5).getState()).getInventory();

        combineStacks(inv);

        List<ItemStack> unsorted = getUnsortedList(invType, inv);
        List<ItemStack> sorted   = getSortedList(player, args, unsorted);
        addItemList(invType, inv, sorted);

        // feedback
        player.playSound(player.getLocation(),
            Sound.BLOCK_WOODEN_TRAPDOOR_CLOSE, 1f, 1f);

        if (invType == INV_PLAYER && plugin.getConfig()
            .getBoolean("messages.inv-message-send")) {
            player.sendMessage(color(plugin.getConfig()
                .getString("messages.inv-message")));
        } else if (invType != INV_PLAYER && plugin.getConfig()
            .getBoolean("messages.chest-message-send")) {
            player.sendMessage(color(plugin.getConfig()
                .getString("messages.chest-message")));
        }

        return true;
    }

    private int getInvType(Player player, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("internalPlayerSort")) {
            return INV_PLAYER;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("internalChestSort")) {
            return chestSize(player) == 54 ? INV_DOUBLE_CHEST : INV_CHEST;
        }
        Block b = player.getTargetBlock((Set<Material>)null, 5);
        if (b.getState() instanceof Chest) {
            Chest c = (Chest)b.getState();
            return c.getInventory().getSize() == 54 ? INV_DOUBLE_CHEST : INV_CHEST;
        }
        return INV_PLAYER;
    }

    private int chestSize(Player player) {
        Chest c = (Chest)player.getTargetBlock((Set<Material>)null, 5).getState();
        return c.getInventory().getSize();
    }

    private YamlConfiguration loadDataConfig() {
        File f = new File(plugin.getDataFolder(), "data.yml");
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            if (!f.exists()) f.createNewFile();
            cfg.load(f);
        } catch (IOException | InvalidConfigurationException ex) {
            ex.printStackTrace();
        }
        return cfg;
    }

    private static String locationKey(Location l) {
        return l.getWorld().getName()
            + "~" + l.getBlockX()
            + "~" + l.getBlockY()
            + "~" + l.getBlockZ();
    }

    private void combineStacks(Inventory inv) {
        ItemStack[] items = inv.getContents();
        boolean changed = false;

        for (int i = 0; i < items.length; i++) {
            ItemStack base = items[i];
            if (base == null || base.getAmount() >= base.getMaxStackSize()) continue;
            int needed = base.getMaxStackSize() - base.getAmount();

            for (int j = i + 1; j < items.length && needed > 0; j++) {
                ItemStack next = items[j];
                if (next == null || next.getType() != base.getType()) continue;

                // if either has meta, only merge if both are tipped arrows with same meta
                if (next.hasItemMeta() || base.hasItemMeta()) {
                    if (base.getType() != Material.TIPPED_ARROW) continue;
                    PotionMeta m1 = (PotionMeta)base.getItemMeta();
                    PotionMeta m2 = (PotionMeta)next.getItemMeta();
                    if (!m1.equals(m2)) continue;
                }

                int move = Math.min(needed, next.getAmount());
                base.setAmount(base.getAmount() + move);
                next.setAmount(next.getAmount() - move);
                if (next.getAmount() == 0) items[j] = null;
                needed -= move;
                changed = true;
            }
        }
        if (changed) inv.setContents(items);
    }

    private List<ItemStack> getUnsortedList(int invType, Inventory inv) {
        int start = invType == INV_PLAYER ? 9 : 0;
        int end   = inv.getSize();
        List<ItemStack> out = new ArrayList<>();
        for (int i = start; i < end; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null) {
                out.add(it);
                inv.clear(i);
            }
        }
        return out;
    }

    private ArrayList<ItemStack> getSortedList(Player player,
                                               String[] args,
                                               List<ItemStack> items)
    {
        ArrayList<ItemStack> list = new ArrayList<>(items);
        if (list.isEmpty()) return list;

        boolean matMode = player.hasMetadata("sort-type");
        String key = args.length > 0 ? args[0].toLowerCase() : "";

        switch (key) {
            case "down": case "d":
                return matMode ? sortMaterialDown(list) : sortNameUp(list);
            case "up": case "u":
                return matMode ? sortMaterialUp(list)   : sortNameDown(list);
            default:
                // toggle by comparing with last
                return sortNameUp(list);
        }
    }

    // name ↑
    private static ArrayList<ItemStack> sortNameUp(ArrayList<ItemStack> l) {
        return bubbleSort(l, false);
    }
    // name ↓
    private static ArrayList<ItemStack> sortNameDown(ArrayList<ItemStack> l) {
        return bubbleSort(l, true);
    }
    // material ↑ == same as nameUp
    private static ArrayList<ItemStack> sortMaterialUp(ArrayList<ItemStack> l) {
        return bubbleSort(l, false);
    }
    // material ↓
    private static ArrayList<ItemStack> sortMaterialDown(ArrayList<ItemStack> l) {
        return bubbleSort(l, true);
    }

    private static ArrayList<ItemStack> bubbleSort(ArrayList<ItemStack> l, boolean desc) {
        for (int i = 0; i < l.size() - 1; i++) {
            for (int j = 1; j < l.size() - i; j++) {
                String a = itemString(l.get(j-1));
                String b = itemString(l.get(j));
                int cmp = a.compareToIgnoreCase(b);
                if ((desc && cmp < 0) || (!desc && cmp > 0)) {
                    ItemStack tmp = l.get(j-1);
                    l.set(j-1, l.get(j));
                    l.set(j, tmp);
                }
            }
        }
        return l;
    }

    private static String itemString(ItemStack it) {
        if (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) {
            return it.getItemMeta().getDisplayName();
        }
        return it.getType().toString();
    }

    private void addItemList(int invType, Inventory inv, List<ItemStack> items) {
        int idx = invType == INV_PLAYER ? 9 : 0;
        for (ItemStack it : items) {
            inv.setItem(idx++, it);
        }
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }
}
