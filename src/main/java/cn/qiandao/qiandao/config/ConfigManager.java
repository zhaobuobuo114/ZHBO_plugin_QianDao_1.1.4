package cn.qiandao.qiandao.config;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * 配置管理器 - 统一读取 config.yml 中的各项设置
 */
public class ConfigManager {

    /** 额外奖励档位结构 */
    public static class Milestone {
        public final List<String> display;
        public final List<String> commands;
        public final String broadcast;

        Milestone(List<String> display, List<String> commands, String broadcast) {
            this.display = display;
            this.commands = commands;
            this.broadcast = broadcast;
        }
    }

    private final QianDaoPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(QianDaoPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** 重新读取配置 */
    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ---------------- 基础设置 ----------------

    public ZoneId getZoneId() {
        String tz = config.getString("general.timezone", "Asia/Shanghai");
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "时区配置无效: " + tz + " , 已回退到 Asia/Shanghai");
            return ZoneId.of("Asia/Shanghai");
        }
    }

    public int getRefreshHour() {
        return config.getInt("general.refresh-hour", 4);
    }

    public int getRefreshMinute() {
        return config.getInt("general.refresh-minute", 0);
    }

    public int getAutoSaveSeconds() {
        return Math.max(30, config.getInt("general.auto-save-seconds", 300));
    }

    // ---------------- 周期奖励 ----------------

    /** 获取周期某一天的GUI图标物品 */
    public ItemStack getCycleIcon(int day) {
        ConfigurationSection sec = config.getConfigurationSection("cycle." + day);
        if (sec == null) {
            return new ItemStack(Material.BARRIER);
        }
        Material material = getMaterial(sec.getString("icon"), Material.STONE);
        // 图标子ID(如 SKULL_ITEM + 5 = 龙头)
        int data = sec.getInt("icon-data", 0);
        return new ItemBuilder(material)
                .data(data)
                .name(sec.getString("name", "&f第" + day + "天"))
                .build();
    }

    /** 获取周期某一天在GUI中展示的奖励说明 */
    public List<String> getCycleDisplay(int day) {
        List<String> display = config.getStringList("cycle." + day + ".display");
        if (display.isEmpty()) {
            display = config.getStringList("cycle." + day + ".commands");
        }
        return display;
    }

    /** 获取周期某一天的奖励指令 */
    public List<String> getCycleCommands(int day) {
        return config.getStringList("cycle." + day + ".commands");
    }

    /** 已配置的周期奖励天数(通常为 1~7) */
    public List<Integer> getCycleDays() {
        List<Integer> days = new ArrayList<>();
        ConfigurationSection sec = config.getConfigurationSection("cycle");
        if (sec == null) {
            return days;
        }
        for (String key : sec.getKeys(false)) {
            try {
                days.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
        days.sort(Integer::compareTo);
        return days;
    }

    // ---------------- 连续奖励档位 ----------------

    /** 获取某个档位的额外奖励, 未配置则返回 null */
    public Milestone getMilestone(int days) {
        ConfigurationSection sec = config.getConfigurationSection("milestones." + days);
        if (sec == null) {
            return null;
        }
        List<String> display = sec.getStringList("display");
        List<String> commands = sec.getStringList("commands");
        String broadcast = sec.getString("broadcast", "");
        return new Milestone(display, commands, broadcast);
    }

    // ---------------- 补签设置 ----------------

    public boolean isBuqianEnabled() {
        return config.getBoolean("buqian.enabled", true);
    }

    public int getBuqianMaxDays() {
        return Math.max(1, config.getInt("buqian.max-days", 3));
    }

    public boolean isBuqianGiveCycleReward() {
        return config.getBoolean("buqian.give-cycle-reward", true);
    }

    /** 构建补签卡物品 */
    public ItemStack createBuqianCard() {
        ConfigurationSection sec = config.getConfigurationSection("buqian.item");
        Material material = getMaterial(sec != null ? sec.getString("material") : null, Material.PAPER);
        ItemBuilder builder = new ItemBuilder(material).name(sec != null ? sec.getString("name", "&a&l补签卡") : "&a&l补签卡");
        if (sec != null && sec.getBoolean("glow", true)) {
            builder.glow();
        }
        if (sec != null) {
            List<String> lore = sec.getStringList("lore");
            if (!lore.isEmpty()) {
                builder.lore(lore);
            }
        }
        return builder.build();
    }

    // ---------------- 在线奖励 ----------------

    public boolean isOnlineRewardEnabled() {
        return config.getBoolean("online-reward.enabled", true);
    }

    public int getOnlineIntervalMinutes() {
        return Math.max(1, config.getInt("online-reward.interval-minutes", 60));
    }

    public int getOnlineDailyMax() {
        return config.getInt("online-reward.daily-max", 1);
    }

    // ---------------- 排名设置 ----------------

    public boolean isScoreboardEnabled() {
        return config.getBoolean("ranking.scoreboard-enable", true);
    }

    public String getScoreboardTitle() {
        return config.getString("ranking.scoreboard-title", "&6&l签到排行榜");
    }

    public int getRankAmount() {
        return Math.min(14, Math.max(1, config.getInt("ranking.amount", 10)));
    }

    // ---------------- 经济联动 ----------------

    /** 经济系统类型(none/zpoints/playerpoints/vault) */
    public String getEconomyType() {
        return config.getString("economy.type", "none");
    }

    /** 周期第N天(1~7)签到获得的金币数, 未配置或越界返回 0 */
    public int getCycleGold(int day) {
        List<Integer> gold = config.getIntegerList("economy.cycle-gold");
        if (gold == null || gold.isEmpty() || day < 1 || day > gold.size()) {
            return 0;
        }
        return Math.max(0, gold.get(day - 1));
    }

    /** 连续签到里程碑额外金币(7/14/30), 未配置返回 0 */
    public int getStreakBonus(int days) {
        return Math.max(0, config.getInt("economy.streak-bonus." + days, 0));
    }

    /** 补签时是否发放基础金币(周期加成不发) */
    public boolean isBuqianGold() {
        return config.getBoolean("economy.buqian-gold", false);
    }

    // ---------------- 音效与特效 ----------------

    /** 今日可签到槽位是否附魔发光 */
    public boolean isGlowToday() {
        return config.getBoolean("effects.glow-today", true);
    }

    /** 音效总开关 */
    public boolean isSoundEnabled() {
        return config.getBoolean("effects.sound-enable", true);
    }

    /** 读取配置中的音效名, 无效时回退 */
    public Sound getSound(String path, Sound fallback) {
        String name = config.getString(path);
        if (name == null || name.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "音效名无效: " + name + " (配置路径 " + path + "), 已回退到 " + fallback);
            return fallback;
        }
    }

    /** 粒子特效总开关 */
    public boolean isParticleEnabled() {
        return config.getBoolean("effects.particle-enable", true);
    }

    /** 粒子类型, 配置无效时返回 null */
    public Particle getParticleType() {
        String name = config.getString("effects.particle-type");
        if (name == null || name.trim().isEmpty()) {
            return Particle.FIREWORKS_SPARK;
        }
        try {
            return Particle.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "粒子类型无效: " + name + " , 已回退到 FIREWORKS_SPARK");
            return Particle.FIREWORKS_SPARK;
        }
    }

    /** 粒子数量(1~200) */
    public int getParticleAmount() {
        return Math.max(1, Math.min(200, config.getInt("effects.particle-amount", 30)));
    }

    // ---------------- 标题 ----------------

    public boolean isTitleEnabled() {
        return config.getBoolean("sign-title.enable", true);
    }

    public String getTitleMain() {
        return config.getString("sign-title.main", "&6&l签到成功");
    }

    public String getTitleSub() {
        return config.getString("sign-title.sub", "&e周期第{cycle}天奖励已发放");
    }

    // ---------------- 消息 ----------------

    /** 获取消息文本(不含前缀, 未翻译颜色) */
    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }

    /** 获取消息前缀 */
    public String getPrefix() {
        return config.getString("messages.prefix", "&8[&6签到&8] &7");
    }

    /** 获取普通玩家帮助列表 */
    public List<String> getHelpUser() {
        return config.getStringList("messages.help-user");
    }

    /** 获取管理员帮助列表 */
    public List<String> getHelpAdmin() {
        return config.getStringList("messages.help-admin");
    }

    // ---------------- 内部工具 ----------------

    /** 安全解析材质名 */
    public Material getMaterial(String name, Material fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Material.valueOf(name.trim().toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "材质名无效: " + name + " , 已回退到 " + fallback);
            return fallback;
        }
    }
}