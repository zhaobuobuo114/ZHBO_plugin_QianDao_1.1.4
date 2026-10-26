package cn.qiandao.qiandao.command;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 主命令 /qiandao (别名 /qd /sign)
 * - /qd                    打开签到面板
 * - /qd rank               查看排行榜
 * - /qd givecard <玩家> [数量]  发放补签卡(管理员/活动使用)
 * - /qd reload             重载配置(管理员)
 */
public class QdCommand implements CommandExecutor, TabCompleter {

    private final QianDaoPlugin plugin;

    public QdCommand(QianDaoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // /qd - 打开签到面板
            Player player = requirePlayer(sender);
            if (player == null) {
                plugin.sendMessage(sender, "player-only");
                return true;
            }
            if (!hasPerm(sender, "qiandao.use")) {
                return true;
            }
            plugin.getGuiManager().openMain(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "rank": {
                Player player = requirePlayer(sender);
                if (player == null) {
                    plugin.sendMessage(sender, "player-only");
                    return true;
                }
                if (!hasPerm(sender, "qiandao.use")) {
                    return true;
                }
                plugin.getGuiManager().openRank(player);
                return true;
            }
            case "givecard": {
                if (!hasPerm(sender, "qiandao.admin")) {
                    return true;
                }
                if (args.length < 2) {
                    plugin.sendMessage(sender, "wrong-usage",
                            "usage", "/qd givecard <玩家> [数量]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    plugin.sendMessage(sender, "player-not-found");
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Math.max(1, Math.min(2304, Integer.parseInt(args[2])));
                    } catch (NumberFormatException ignored) {
                    }
                }
                plugin.getCardManager().give(target, amount);
                plugin.sendMessage(sender, "card-give",
                        "player", target.getName(), "card", String.valueOf(amount));
                plugin.sendMessage(target, "card-receive", "card", String.valueOf(amount));
                return true;
            }
            case "reload": {
                if (!hasPerm(sender, "qiandao.admin")) {
                    return true;
                }
                plugin.reload();
                plugin.sendMessage(sender, "reload-success");
                return true;
            }
            case "help": {
                if (!hasPerm(sender, "qiandao.use")) {
                    return true;
                }
                // 普通玩家只显示基础指令
                for (String line : plugin.getConfigManager().getHelpUser()) {
                    sender.sendMessage(ItemBuilder.color(line));
                }
                // 管理员额外显示管理指令
                if (sender.hasPermission("qiandao.admin")) {
                    for (String line : plugin.getConfigManager().getHelpAdmin()) {
                        sender.sendMessage(ItemBuilder.color(line));
                    }
                }
                // 底部版本号
                sender.sendMessage(ItemBuilder.color("&8&m------------------------------"));
                sender.sendMessage(ItemBuilder.color("&7【ZHBO】plugin_QianDao &e&l v"
                        + plugin.getDescription().getVersion()));
                sender.sendMessage(ItemBuilder.color("&7作者: &fzhaobuobuo &8| &7联系: &f1071576270@QQ.COM"));
                return true;
            }
            default:
                plugin.sendMessage(sender, "wrong-usage", "usage", "/qd [rank|givecard|reload|help]");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            addIfStarts(result, args[0], "rank");
            addIfStarts(result, args[0], "help");
            // 管理指令只对管理员补全
            if (sender.hasPermission("qiandao.admin")) {
                addIfStarts(result, args[0], "givecard");
                addIfStarts(result, args[0], "reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("givecard")
                && sender.hasPermission("qiandao.admin")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                addIfStarts(result, args[1], p.getName());
            }
        }
        return result;
    }

    private void addIfStarts(List<String> list, String input, String candidate) {
        if (candidate.toLowerCase().startsWith(input.toLowerCase())) {
            list.add(candidate);
        }
    }

    private Player requirePlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }

    /** 权限检查, 无权限时自动提示 */
    private boolean hasPerm(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        plugin.sendMessage(sender, "no-permission");
        return false;
    }
}