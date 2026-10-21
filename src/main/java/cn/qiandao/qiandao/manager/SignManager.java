package cn.qiandao.qiandao.manager;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.config.ConfigManager;
import cn.qiandao.qiandao.data.PlayerData;
import cn.qiandao.qiandao.util.DateUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 签到核心逻辑管理器
 * - 每日一次签到, 七日一周期, 第7天最好, 第8天回到第1天(连续天数继续累计)
 * - 断签后连续天数归零, 重新计算
 * - 连续7/14/30天发放额外奖励(可配置)
 * - 补签: 消耗补签卡, 只能补最近N天内漏签日期, 补签后连续天数恢复
 */
public class SignManager {

    private final QianDaoPlugin plugin;
    private final ConfigManager config;

    public SignManager(QianDaoPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /**
     * 今日签到
     * @return 新的连续签到天数; -1 = 今天已签到过
     */
    public int sign(Player player, PlayerData pd) {
        LocalDate today = DateUtil.today(config);
        String todayStr = DateUtil.fmt(today);

        // 今天已经签到过(正常签到或补签)
        if (pd.getCheckDays().contains(todayStr) || pd.getBuqianDays().contains(todayStr)) {
            return -1;
        }

        int oldStreak = getStreak(pd);

        pd.getCheckDays().add(todayStr);
        pd.addTotal(1);
        prune(pd, today);

        // 断签判定: 签到前连续天数已为0 -> 断签归零, 重新计算
        // 同时清空上一轮已发放的额外奖励记录, 新的连续周期从头计算
        if (oldStreak == 0) {
            pd.getAwarded().clear();
        }

        int newStreak = getStreak(pd);

        // 周期奖励: 第8天起重新从第1天开始(连续天数继续累计)
        int cycleDay = ((newStreak - 1) % 7) + 1;
        grantCycle(player, cycleDay);

        // 金币奖励: 周期第N天对应的金币(与物品奖励同时发放)
        int gold = config.getCycleGold(cycleDay);
        if (gold > 0 && plugin.getEconomyManager().give(player, gold)) {
            plugin.sendMessage(player, "gold-granted", "gold", String.valueOf(gold));
        }

        // 连续7/14/30天额外奖励
        checkMilestones(player, pd, newStreak);
        return newStreak;
    }

    /**
     * 补签指定日期
     * @return 新的连续签到天数; -1 = 日期无效或已签过; -2 = 补签功能未开启
     */
    public int buqian(Player player, PlayerData pd, LocalDate date) {
        if (!config.isBuqianEnabled()) {
            return -2;
        }
        LocalDate today = DateUtil.today(config);

        // 只能补签过去(昨天及更早)、且位于最近N天内的日期
        if (date == null || !date.isBefore(today)) {
            return -1;
        }
        LocalDate earliest = today.minusDays(config.getBuqianMaxDays());
        LocalDate first = getFirstCheckIn(pd);
        if (date.isBefore(earliest) || (first != null && date.isBefore(first))) {
            return -1;
        }

        String str = DateUtil.fmt(date);
        // 同一天不能重复补签(已正常签到或已补签)
        if (pd.getCheckDays().contains(str) || pd.getBuqianDays().contains(str)) {
            return -1;
        }

        int oldStreak = getStreak(pd);

        pd.getBuqianDays().add(str);
        prune(pd, today);

        // 补签视为当天已签到: 发放该日对应的周期奖励(可配置)
        if (config.isBuqianGiveCycleReward()) {
            int streakAtDate = getStreakEndingAt(pd, date);
            int cycleDay = ((streakAtDate - 1) % 7) + 1;
            grantCycle(player, cycleDay);
        }

        int newStreak = getStreak(pd);

        // 补签金币(发放第1天对应的金币数)
        if (config.isBuqianGold()) {
            int gold = config.getCycleGold(1);
            if (gold > 0 && plugin.getEconomyManager().give(player, gold)) {
                plugin.sendMessage(player, "gold-granted", "gold", String.valueOf(gold));
            }
        }

        checkMilestones(player, pd, newStreak);
        return newStreak;
    }

    /** 获取玩家当前有效连续签到天数(链以今天或昨天结尾才有效, 否则断签归零) */
    public int getStreak(PlayerData pd) {
        Set<LocalDate> dates = getAllDates(pd);
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate last = Collections.max(dates);
        LocalDate today = DateUtil.today(config);
        // 连续链必须以今天或昨天为结尾, 否则视为断签
        if (!last.equals(today) && !last.equals(today.minusDays(1))) {
            return 0;
        }
        return countBack(dates, last);
    }

    /** 以指定日期为末尾的连续签到天数 */
    private int getStreakEndingAt(PlayerData pd, LocalDate end) {
        Set<LocalDate> dates = getAllDates(pd);
        if (!dates.contains(end)) {
            return 0;
        }
        return countBack(dates, end);
    }

    /** 今天是否已签到(正常或补签) */
    public boolean isSignedToday(PlayerData pd) {
        String todayStr = DateUtil.fmt(DateUtil.today(config));
        return pd.getCheckDays().contains(todayStr) || pd.getBuqianDays().contains(todayStr);
    }

    /**
     * 获取可补签的漏签日期列表(从最近到最远)
     * 规则: 最近N天内、第一次签到之后、且当日既未正常签到也未补签
     */
    public List<LocalDate> getMissedDays(PlayerData pd) {
        List<LocalDate> missed = new ArrayList<>();
        if (!config.isBuqianEnabled()) {
            return missed;
        }
        LocalDate first = getFirstCheckIn(pd);
        if (first == null) {
            return missed; // 从未签到, 没有漏签可言
        }
        LocalDate today = DateUtil.today(config);
        Set<String> checkDays = pd.getCheckDays();
        Set<String> buqianDays = pd.getBuqianDays();
        int maxDays = config.getBuqianMaxDays();
        for (int i = maxDays; i >= 1; i--) {
            LocalDate d = today.minusDays(i);
            if (d.isBefore(first)) {
                continue;
            }
            String str = DateUtil.fmt(d);
            if (!checkDays.contains(str) && !buqianDays.contains(str)) {
                missed.add(d);
            }
        }
        return missed;
    }

    // ---------------- 内部逻辑 ----------------

    /** 计算以 end 为末尾、向前连续存在的天数 */
    private int countBack(Set<LocalDate> dates, LocalDate end) {
        int n = 0;
        LocalDate d = end;
        while (dates.contains(d)) {
            n++;
            d = d.minusDays(1);
        }
        return n;
    }

    /** 玩家所有签到过的日期(正常 + 补签), 解析异常自动忽略 */
    private Set<LocalDate> getAllDates(PlayerData pd) {
        Set<LocalDate> result = new HashSet<>();
        for (String s : pd.getCheckDays()) {
            LocalDate d = DateUtil.parse(s);
            if (d != null) {
                result.add(d);
            }
        }
        for (String s : pd.getBuqianDays()) {
            LocalDate d = DateUtil.parse(s);
            if (d != null) {
                result.add(d);
            }
        }
        return result;
    }

    /** 第一次签到日期(正常或补签中最小的日期) */
    private LocalDate getFirstCheckIn(PlayerData pd) {
        LocalDate first = null;
        for (String s : pd.getCheckDays()) {
            LocalDate d = DateUtil.parse(s);
            if (d != null && (first == null || d.isBefore(first))) {
                first = d;
            }
        }
        for (String s : pd.getBuqianDays()) {
            LocalDate d = DateUtil.parse(s);
            if (d != null && (first == null || d.isBefore(first))) {
                first = d;
            }
        }
        return first;
    }

    /** 发放某一天的周期奖励(以控制台执行配置中的指令) */
    private void grantCycle(Player player, int cycleDay) {
        List<String> commands = config.getCycleCommands(cycleDay);
        if (commands.isEmpty()) {
            return;
        }
        executeCommands(player, commands);
    }

    /** 检查并发放连续签到额外奖励(7/14/30天) */
    private void checkMilestones(Player player, PlayerData pd, int newStreak) {
        int[] thresholds = {7, 14, 30};
        for (int t : thresholds) {
            if (newStreak < t || pd.getAwarded().contains(t)) {
                continue;
            }
            ConfigManager.Milestone ms = config.getMilestone(t);
            if (ms == null || ms.commands.isEmpty()) {
                continue;
            }
            pd.getAwarded().add(t);
            executeCommands(player, ms.commands);
            plugin.sendMessage(player, "milestone-reward", "streak", String.valueOf(t));
            // 里程碑额外金币
            int goldBonus = config.getStreakBonus(t);
            if (goldBonus > 0 && plugin.getEconomyManager().give(player, goldBonus)) {
                plugin.sendMessage(player, "gold-granted", "gold", String.valueOf(goldBonus));
            }
            // 全服广播
            if (ms.broadcast != null && !ms.broadcast.isEmpty()) {
                String msg = ms.broadcast.replace("%player%", player.getName());
                Bukkit.broadcastMessage(cn.qiandao.qiandao.util.ItemBuilder.color(msg));
            }
        }
    }

    /** 执行奖励指令(将 %player% 替换为玩家名) */
    private void executeCommands(Player player, List<String> commands) {
        for (String cmd : commands) {
            if (cmd == null || cmd.trim().isEmpty()) {
                continue;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
        }
    }

    /** 清理超过60天的旧日期数据, 防止文件无限膨胀 */
    private void prune(PlayerData pd, LocalDate today) {
        LocalDate keep = today.minusDays(60);
        pd.getCheckDays().removeIf(s -> {
            LocalDate d = DateUtil.parse(s);
            return d != null && d.isBefore(keep);
        });
        pd.getBuqianDays().removeIf(s -> {
            LocalDate d = DateUtil.parse(s);
            return d != null && d.isBefore(keep);
        });
    }
}