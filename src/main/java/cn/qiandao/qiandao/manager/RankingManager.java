package cn.qiandao.qiandao.manager;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.data.PlayerData;
import cn.qiandao.qiandao.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 签到排名管理器 - 屏幕右侧计分板
 * 排名规则: 按连续签到天数降序, 相同天数按累计签到次数降序
 * 1.12.2 老协议每行最多16字符, 使用 Team 前缀+后缀(32字符)的方式绕开限制
 */
public class RankingManager {

    /** 排名条目 */
    public static class RankEntry {
        public final UUID uuid;
        public final String name;
        public final int streak;
        public final int total;

        public RankEntry(UUID uuid, String name, int streak, int total) {
            this.uuid = uuid;
            this.name = name;
            this.streak = streak;
            this.total = total;
        }
    }

    /** 每位玩家的独立计分板(不干扰服务器主计分板) */
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    /** 每位玩家的行 Team 缓存 */
    private final Map<UUID, List<Team>> teams = new HashMap<>();

    private final QianDaoPlugin plugin;

    public RankingManager(QianDaoPlugin plugin) {
        this.plugin = plugin;
    }

    /** 计算全服排名 */
    public List<RankEntry> computeRank() {
        List<RankEntry> entries = new ArrayList<>();
        for (PlayerData pd : plugin.getDataManager().all()) {
            int streak = plugin.getSignManager().getStreak(pd);
            int total = pd.getTotal();
            if (streak <= 0 && total <= 0) {
                continue; // 从未签到的玩家不上榜
            }
            entries.add(new RankEntry(toUuid(pd.getUuid()), pd.getName(), streak, total));
        }
        entries.sort(Comparator
                .comparingInt((RankEntry e) -> e.streak).reversed()
                .thenComparing(Comparator.comparingInt((RankEntry e) -> e.total).reversed())
                .thenComparing(e -> e.name));
        return entries;
    }

    /** 玩家在该排名中的名次(从1开始), 未上榜返回 -1 */
    public int getPlayerRank(Player player, List<RankEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).uuid.equals(player.getUniqueId())) {
                return i + 1;
            }
        }
        return -1;
    }

    /** 安全转换 uuid 字符串 */
    private UUID toUuid(String str) {
        try {
            return UUID.fromString(str);
        } catch (Exception e) {
            return new UUID(0L, 0L);
        }
    }

    /** 刷新所有在线玩家的计分板 */
    public void refreshAll() {
        if (!plugin.getConfigManager().isScoreboardEnabled()) {
            return;
        }
        List<RankEntry> entries = computeRank();
        int amount = plugin.getConfigManager().getRankAmount();
        List<RankEntry> top = entries.subList(0, Math.min(amount, entries.size()));
        for (Player player : Bukkit.getOnlinePlayers()) {
            int myRank = getPlayerRank(player, entries);
            int myStreak = plugin.getSignManager().getStreak(plugin.getDataManager().get(player.getUniqueId()));
            render(player, top, myRank, myStreak);
        }
    }

    /** 玩家进入服务器时创建右侧计分板 */
    public void createBoard(Player player) {
        if (!plugin.getConfigManager().isScoreboardEnabled()) {
            return;
        }
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("qd", "dummy");
        obj.setDisplayName(ItemBuilder.color(plugin.getConfigManager().getScoreboardTitle()));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        // 预注册行 Team(最多约15行)
        List<Team> teamList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Team team = board.registerNewTeam("qd" + i);
            // 每行使用一个唯一的entry, 用颜色代码字符区分
            team.addEntry("§" + Integer.toHexString(i));
            teamList.add(team);
        }
        boards.put(player.getUniqueId(), board);
        teams.put(player.getUniqueId(), teamList);
        player.setScoreboard(board);
    }

    /** 玩家退出时移除计分板 */
    public void removeBoard(Player player) {
        UUID uuid = player.getUniqueId();
        boards.remove(uuid);
        teams.remove(uuid);
        // 交还给主计分板
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        if (player.getScoreboard() != null && !player.getScoreboard().equals(main)) {
            player.setScoreboard(main);
        }
    }

    /** 渲染某位玩家的计分板内容 */
    private void render(Player player, List<RankEntry> top, int myRank, int myStreak) {
        Scoreboard board = boards.get(player.getUniqueId());
        List<Team> teamList = teams.get(player.getUniqueId());
        if (board == null || teamList == null) {
            createBoard(player);
            board = boards.get(player.getUniqueId());
            teamList = teams.get(player.getUniqueId());
        }
        List<String> lines = new ArrayList<>();
        // 前N名: 金/银/铜徽章 + 普通名次
        for (int i = 0; i < top.size(); i++) {
            RankEntry e = top.get(i);
            String rankTag;
            if (i == 0) {
                rankTag = "§6§l#1";
            } else if (i == 1) {
                rankTag = "§7§l#2";
            } else if (i == 2) {
                rankTag = "§c§l#3";
            } else {
                rankTag = "§8#" + (i + 1);
            }
            lines.add(rankTag + " §r§f" + e.name + " §a" + e.streak + "天");
        }
        // 分隔线
        lines.add("§8────────");
        // 我的排名
        if (myRank > 0) {
            lines.add("§7我的排名: §e#" + myRank + " §7(§a" + myStreak + " §7天)");
        } else {
            lines.add("§7我的排名: §8未上榜");
        }
        // 版本号脚注
        lines.add("§8──── QianDao v" + plugin.getDescription().getVersion());
        // 自下而上填充(计分板中分数越高越靠上)
        int lineCount = Math.min(lines.size(), teamList.size());
        for (int i = 0; i < teamList.size(); i++) {
            Team team = teamList.get(i);
            if (i < lineCount) {
                String[] split = splitLine(lines.get(i));
                team.setPrefix(split[0]);
                team.setSuffix(split[1]);
                board.getObjective("qd").getScore("§" + Integer.toHexString(i)).setScore(lineCount - i);
            } else {
                // 清空多余的旧行
                team.setPrefix("");
                team.setSuffix("");
                board.resetScores("§" + Integer.toHexString(i));
            }
        }
    }

    /** 将一行文本拆分成前缀(≤16字符)+后缀(≤16字符), 避免从中间截断颜色代码 */
    private String[] splitLine(String line) {
        line = ItemBuilder.color(line);
        String pre = line.substring(0, Math.min(16, line.length()));
        String suf = line.length() > 16 ? line.substring(16) : "";
        if (suf.length() > 16) {
            suf = suf.substring(0, 16);
        }
        // 若前缀以裸 § 结尾, 把 § 移到后缀开头, 防止颜色代码被截断
        while (pre.endsWith("§")) {
            suf = "§" + suf;
            pre = pre.substring(0, pre.length() - 1);
        }
        return new String[]{pre, suf};
    }
}