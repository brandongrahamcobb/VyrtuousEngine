package com.brandongcobb.lucy.commands;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FlyCommand implements CommandExecutor {

    public static final Map<UUID, PlayerSpeedData> speedMap = new ConcurrentHashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        if (args.length == 0) {
            boolean now = !p.getAllowFlight();
            p.setAllowFlight(now);
            p.setFlying(now);
            p.sendMessage(now ? "Flight enabled." : "Flight disabled.");
            return true;
        }
        boolean isFly;
        float speed;
        Player target;
        try {
            if (args.length == 1) {
                isFly = p.isFlying();
                speed = parseSpeed(args[0]);
                target = p;
            } else if (args.length == 2) {
                isFly = parseFlyMode(args[0]);
                speed = parseSpeed(args[1]);
                target = p;
            } else {
                // args >= 3
                isFly = parseFlyMode(args[0]);
                speed = parseSpeed(args[1]);
                List<Player> found = Bukkit.getServer().matchPlayer(args[2]);
                if (found.isEmpty()) {
                    sender.sendMessage("No player matched “" + args[2] + "”.");
                    return true;
                }
                target = found.get(0);
            }
        } catch (IllegalArgumentException iae) {
            sender.sendMessage("Usage: /fly [fly|walk] <speed> [player]");
            return true;
        }
        target.setAllowFlight(isFly);
        target.setFlying(isFly);
        UUID id = target.getUniqueId();
        PlayerSpeedData data = speedMap.computeIfAbsent(id, k -> new PlayerSpeedData());
        if (isFly) {
            data.flySpeed = speed;
            target.setFlySpeed(clampSpeed(speed, true));
        } else {
            data.walkSpeed = speed;
            target.setWalkSpeed(clampSpeed(speed, false));
        }
        sender.sendMessage(
                "Set " + (isFly ? "fly" : "walk") +
                        " speed of " + target.getName() + " to " + speed
        );
        return true;
    }

    private float parseSpeed(String s) {
        try {
            float v = Float.parseFloat(s);
            if (v < 0f || v > 10f) throw new IllegalArgumentException();
            return v;
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    private boolean parseFlyMode(String s) {
        s = s.toLowerCase();
        if (s.startsWith("f")) return true;
        if (s.startsWith("w") || s.startsWith("r")) return false;
        throw new IllegalArgumentException();
    }

    private float clampSpeed(float us, boolean fly) {
        float def = fly ? 0.1f : 0.2f;
        if (us < 1f) return def * us;
        float ratio = ((us - 1f) / 9f) * (1f - def);
        return def + ratio;
    }

    private static class PlayerSpeedData {
        float walkSpeed = 0.2f;
        float flySpeed = 0.1f;
    }
}
