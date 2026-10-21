package cn.qiandao.qiandao;

import cn.qiandao.qiandao.command.QdCommand;
import cn.qiandao.qiandao.config.ConfigManager;
import cn.qiandao.qiandao.data.DataManager;
import cn.qiandao.qiandao.data.PlayerData;
import cn.qiandao.qiandao.gui.GuiManager;
import cn.qiandao.qiandao.listener.GuiListener;
import cn.qiandao.qiandao.listener.PlayerListener;
import cn.qiandao.qiandao.manager.CardManager;
import cn.qiandao.qiandao.manager.EconomyManager;
import cn.qiandao.qiandao.manager.RankingManager;
import cn.qiandao.qiandao.manager.SignManager;
import cn.qiandao.qiandao.util.DateUtil;
import cn.qiandao.qiandao.util.EffectUtil;
import cn.qiandao.qiandao.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;

/**
 * QianDao 签到插件主类 (Minecraft 1.12.2)
 *
 * 功能总览:
 * - 每日一次签到, 每日凌晨4点刷新(可配置时区/刷新时间)
 * - 7天一个周期, 第7天奖励最好, 第8天回到第1天(连续天数继续累计)
 * - 连续7/14/30天额外奖励, 断签连续天数归零重新计算
 * - 补签卡(右键使用): 只能补最近N天内漏签日期, 补签后连续天数恢复
 * - 屏幕右侧签到排名计分板(可开关)
 * - 在线奖励自动发放补签卡
 */
public final class QianDaoPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DataManager dataManager;
    private SignManager signManager;
    private CardManager cardManager;
    private RankingManager rankingManager;
    private GuiManager guiManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        // 首次启动释放默认配置
        saveDefaultConfig();

        // 初始化各管理器
        configManager = new ConfigManager(this);
        dataManager = new DataManager(this);
        dataManager.load();
        signManager = new SignManager(this);
        cardManager = new CardManager(this);
        rankingManager = new RankingManager(this);
        guiManager = new GuiManager(this);
        economyManager = new EconomyManager(this);
        economyManager.init();

        // 注册命令与监听器
        QdCommand qdCommand = new QdCommand(this);
        getCommand("qiandao").setExecutor(qdCommand);
        getCommand("qiandao").setTabCompleter(qdCommand);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        // 每分钟任务: 在线奖励 + 排名计分板刷新
        new BukkitRunnable() {
            @Override
            public void run() {
                onlineRewardTick();
                rankingManager.refreshAll();
            }
        }.runTaskTimer(this, 120L, 1200L); // 延迟120tick(6秒), 每1200tick(60秒)

        // 定时自动保存(异步, 间隔取配置)
        long saveSeconds = configManager.getAutoSaveSeconds();
        new BukkitRunnable() {
            @Override
            public void run() {
                dataManager.save();
            }
        }.runTaskTimerAsynchronously(this, saveSeconds * 20L, saveSeconds * 20L);

        // 热重载场景: 为已在线的玩家补建计分板
        for (Player player : Bukkit.getOnlinePlayers()) {
            rankingManager.createBoard(player);
        }
        rankingManager.refreshAll();

        getLogger().info("QianDao 签到插件已启用 v" + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        // 保存所有玩家数据
        dataManager.save();
        for (Player player : Bukkit.getOnlinePlayers()) {
            rankingManager.removeBoard(player);
        }
        getLogger().info("QianDao 签到插件已停用");
    }

    /** 重载配置(供 /qd reload 使用) */
    public void reload() {
        configManager.reload();
        economyManager.init();
        // 计分板开关变化即时生效
        for (Player player : Bukkit.getOnlinePlayers()) {
            rankingManager.removeBoard(player);
            if (configManager.isScoreboardEnabled()) {
                rankingManager.createBoard(player);
            }
        }
        rankingManager.refreshAll();
    }

    /**
     * 在线奖励: 玩家累计在线达到配置时长, 自动发放补签卡
     * 每天有发放上限, 统计随刷新时间每日重置
     */
    private void onlineRewardTick() {
        if (!configManager.isOnlineRewardEnabled()) {
            return;
        }
        LocalDate today = DateUtil.today(configManager);
        String todayStr = DateUtil.fmt(today);
        int interval = configManager.getOnlineIntervalMinutes();
        int dailyMax = configManager.getOnlineDailyMax();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData pd = dataManager.get(player.getUniqueId());
            // 跨天重置在线统计
            if (!pd.getOnlineDate().equals(todayStr)) {
                pd.setOnlineDate(todayStr);
                pd.setOnlineMinutes(0);
                pd.setOnlineCardsToday(0);
            }
            pd.setOnlineMinutes(pd.getOnlineMinutes() + 1);
            // 达到发放条件且未超过每日上限
            if (dailyMax > 0
                    && pd.getOnlineCardsToday() < dailyMax
                    && pd.getOnlineMinutes() >= interval) {
                pd.setOnlineMinutes(pd.getOnlineMinutes() - interval);
                pd.setOnlineCardsToday(pd.getOnlineCardsToday() + 1);
                cardManager.give(player, 1);
                sendMessage(player, "card-got-online");
                EffectUtil.onlineCardEffect(this, player);
            }
        }
    }

    /**
     * 发送带前缀的彩色消息
     * @param key 消息键; 后面的参数为 {占位符},{值} 成对出现
     */
    public void sendMessage(CommandSender target, String key, String... placeholders) {
        String raw = configManager.getMessage(key);
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        if (!key.equals("help")) {
            raw = configManager.getPrefix() + raw;
        }
        target.sendMessage(ItemBuilder.color(raw));
    }

    // ---------------- Getter ----------------

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public SignManager getSignManager() {
        return signManager;
    }

    public CardManager getCardManager() {
        return cardManager;
    }

    public RankingManager getRankingManager() {
        return rankingManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}