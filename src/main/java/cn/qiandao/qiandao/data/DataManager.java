package cn.qiandao.qiandao.data;

import cn.qiandao.qiandao.QianDaoPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 数据管理器 - 负责 data.yml 的加载与保存
 */
public class DataManager {

    private final QianDaoPlugin plugin;
    private final File dataFile;
    /** 玩家数据缓存: uuid字符串 -> 数据 */
    private final Map<String, PlayerData> playerMap = new HashMap<>();

    public DataManager(QianDaoPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    /** 获取玩家数据, 不存在则自动创建 */
    public PlayerData get(UUID uuid) {
        return get(uuid.toString());
    }

    public PlayerData get(String uuid) {
        synchronized (playerMap) {
            PlayerData data = playerMap.get(uuid);
            if (data == null) {
                // 玩家名待上线后通过监听器更新
                data = new PlayerData(uuid, uuid);
                playerMap.put(uuid, data);
            }
            return data;
        }
    }

    /** 获取所有玩家数据(用于排名) */
    public Collection<PlayerData> all() {
        synchronized (playerMap) {
            return new ArrayList<>(playerMap.values());
        }
    }

    /** 从 data.yml 加载玩家数据 */
    public void load() {
        playerMap.clear();
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String uuid : players.getKeys(false)) {
            try {
                ConfigurationSection sec = players.getConfigurationSection(uuid);
                PlayerData data = new PlayerData(uuid, sec.getString("name", "?"));
                data.getCheckDays().addAll(sec.getStringList("check-days"));
                data.getBuqianDays().addAll(sec.getStringList("buqian-days"));
                data.setTotal(sec.getInt("total", 0));
                data.getAwarded().addAll(sec.getIntegerList("awarded"));
                data.setOnlineDate(sec.getString("online-date", ""));
                data.setOnlineMinutes(sec.getInt("online-minutes", 0));
                data.setOnlineCardsToday(sec.getInt("online-cards-today", 0));
                playerMap.put(uuid, data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "加载玩家 " + uuid + " 的数据失败, 已跳过", e);
            }
        }
        plugin.getLogger().info("已加载 " + playerMap.size() + " 名玩家的签到数据");
    }

    /** 保存数据(主线程调用) */
    public void save() {
        try {
            YamlConfiguration yaml = new YamlConfiguration();
            Map<String, PlayerData.Snapshot> snapshots = new HashMap<>();
            // 先取所有数据的快照, 防止保存过程中数据被修改
            synchronized (playerMap) {
                for (Map.Entry<String, PlayerData> entry : playerMap.entrySet()) {
                    snapshots.put(entry.getKey(), entry.getValue().snapshot());
                }
            }
            for (Map.Entry<String, PlayerData.Snapshot> entry : snapshots.entrySet()) {
                String uuid = entry.getKey();
                PlayerData.Snapshot s = entry.getValue();
                ConfigurationSection sec = yaml.createSection("players." + uuid);
                sec.set("name", s.name);
                sec.set("check-days", new ArrayList<>(s.checkDays));
                sec.set("buqian-days", new ArrayList<>(s.buqianDays));
                sec.set("total", s.total);
                sec.set("awarded", s.awarded);
                sec.set("online-date", s.onlineDate);
                sec.set("online-minutes", s.onlineMinutes);
                sec.set("online-cards-today", s.onlineCardsToday);
            }
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存玩家数据失败", e);
        }
    }

    /** 异步保存(避免卡顿主线程) */
    public void saveAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                save();
            }
        }.runTaskAsynchronously(plugin);
    }
}