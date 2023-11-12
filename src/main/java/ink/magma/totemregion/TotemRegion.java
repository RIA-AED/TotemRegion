package ink.magma.totemregion;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class TotemRegion extends JavaPlugin implements Listener, TabExecutor {
    Configuration configuration;
    ConfigurationSection regions;
    Location pos1;
    Location pos2;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        configuration = getConfig();
        regions = configuration.getConfigurationSection("regions");

        Bukkit.getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginCommand("totemregion") != null) {
            Bukkit.getPluginCommand("totemregion").setExecutor(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerUsingTotem(EntityResurrectEvent event) {
        // 需要是玩家
        if (event.getEntity() instanceof Player player) {
            Location playerLocation = player.getLocation();
            if (isInRegion(playerLocation) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player player) {
            // 如果在一个区域里
            if (isInRegion(player.getLocation()) != null) {
                // 判断回血原因
                List<EntityRegainHealthEvent.RegainReason> reason = Arrays.asList(EntityRegainHealthEvent.RegainReason.SATIATED, EntityRegainHealthEvent.RegainReason.REGEN);
                if (reason.contains(event.getRegainReason())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @Nullable
    public String isInRegion(Location location) {
        for (String region : regions.getKeys(false)) {
            boolean xb = isBetween(location.getBlockX(), regions.getInt(region + ".pos1.x"), regions.getInt(region + ".pos2.x"));
            boolean yb = isBetween(location.getBlockY(), regions.getInt(region + ".pos1.y"), regions.getInt(region + ".pos2.y"));
            boolean zb = isBetween(location.getBlockZ(), regions.getInt(region + ".pos1.z"), regions.getInt(region + ".pos2.z"));
            boolean sameWorld = Objects.requireNonNull(location.getWorld())
                    .getName().equals(regions.getString(region + ".world"));
            if (xb && yb && zb && sameWorld) {
                return region;
            }
        }
        return null;
    }


    public boolean isBetween(int number, int first, int second) {
        if (first == second) return false;
        if (first > second) {
            int a = first;
            first = second;
            second = a;
        }
        // 现在, second 一定大于 first
        if (number >= first && number <= second) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) return false;
        if (!sender.hasPermission("totemregion.admin")) {
            sender.sendMessage("您没有权限");
        }
        Player player = (Player) sender;
        if (args.length == 0) return false;


        // pos1 / pos2
        if (Objects.equals(args[0], "pos1")) {
            pos1 = player.getLocation();
            sender.sendMessage("已将您的位置设置为 pos1");
            return true;
        }
        if (Objects.equals(args[0], "pos2")) {
            pos2 = player.getLocation();
            sender.sendMessage("已将您的位置设置为 pos2");
            return true;
        }

        // create
        if (Objects.equals(args[0], "create")) {
            if (args.length != 2) {
                return false;
            }
            if (pos1 == null || pos2 == null) {
                sender.sendMessage("pos1 或 pos2 未设置");
                return true;
            }

            String regionName = args[1];

            configuration.set("regions." + regionName + ".pos1.x", pos1.getBlockX());
            configuration.set("regions." + regionName + ".pos1.y", pos1.getBlockY());
            configuration.set("regions." + regionName + ".pos1.z", pos1.getBlockZ());
            configuration.set("regions." + regionName + ".pos2.x", pos2.getBlockX());
            configuration.set("regions." + regionName + ".pos2.y", pos2.getBlockY());
            configuration.set("regions." + regionName + ".pos2.z", pos2.getBlockZ());
            configuration.set("regions." + regionName + ".world", pos2.getWorld().getName());

            sender.sendMessage("已保存区域: " + regionName);
            sender.sendMessage(MessageFormat.format(
                    "({0} {1} {2}) ({3} {4} {5}) 在世界 {6}",
                    pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ(),
                    pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ(),
                    pos2.getWorld().getName())
            );
            saveConfig();
            return true;
        }

        // list
        if (Objects.equals(args[0], "list")) {
            sender.sendMessage("以下是限制图腾的区域名称:");
            for (String region : regions.getKeys(false)) {
                sender.sendMessage(MessageFormat.format(
                        "区域: {0} - ({1} {2} {3}) ({4} {5} {6}) 在世界 {7}",
                        region,
                        regions.getInt(region + ".pos1.x"), regions.getInt(region + ".pos1.y"), regions.getInt(region + ".pos1.z"),
                        regions.getInt(region + ".pos2.x"), regions.getInt(region + ".pos2.y"), regions.getInt(region + ".pos2.z"),
                        regions.getString(region + ".world")
                ));
            }
        }

        // remove
        if (Objects.equals(args[0], "remove")) {
            if (args.length != 2) {
                return false;
            }
            String regionName = args[1];
            if (configuration.get("regions." + regionName) == null) {
                sender.sendMessage("此 region 不存在.");
                return true;
            }

            configuration.set("regions." + regionName, null);
            sender.sendMessage("删除成功.");
            saveConfig();
        }

        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) return null;
        if (sender instanceof ConsoleCommandSender) return null;
        if (!sender.hasPermission("totemregion.admin")) return null;
        return Arrays.asList("pos1", "pos2", "create", "list", "remove");
    }
}
