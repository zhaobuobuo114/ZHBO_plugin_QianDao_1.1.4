package cn.qiandao.qiandao.manager;

import cn.qiandao.qiandao.QianDaoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

/**
 * 经济联动管理器 - 签到金币发放
 * 按 config.yml 的 economy.type 选择经济系统:
 * - zpoints:       ZPoints 插件的点券(金币), 可直接在 ZPoints 商店/抽卡中使用
 * - playerpoints:  PlayerPoints 插件的点券
 * - vault:         挂载 Vault 的经济插件(如 EssentialsX / Economy 的金钱)
 * 对应插件未安装时自动跳过金币奖励, 不影响签到
 */
public class EconomyManager {

    /** 经济系统类型 */
    public enum EconomyType {
        NONE, ZPOINTS, PLAYERPOINTS, VAULT
    }

    private final QianDaoPlugin plugin;
    private EconomyType type = EconomyType.NONE;
    /** PlayerPoints API 实例缓存 */
    private org.black_ixx.playerpoints.PlayerPointsAPI pointsApi;

    public EconomyManager(QianDaoPlugin plugin) {
        this.plugin = plugin;
    }

    /** 初始化: 读取配置类型并检测对应插件是否已加载 */
    public void init() {
        String configured = plugin.getConfigManager().getEconomyType();
        EconomyType want;
        try {
            want = EconomyType.valueOf(configured.toUpperCase());
        } catch (Exception e) {
            want = EconomyType.NONE;
        }
        String needPlugin = null;
        switch (want) {
            case ZPOINTS:
                needPlugin = "ZPoints";
                break;
            case PLAYERPOINTS:
                needPlugin = "PlayerPoints";
                break;
            case VAULT:
                needPlugin = "Vault";
                break;
            default:
                break;
        }
        if (needPlugin != null && !isLoaded(needPlugin)) {
            plugin.getLogger().log(Level.WARNING,
                    "economy.type 配置为 " + want + " 但未检测到插件 " + needPlugin
                            + " , 金币奖励已自动跳过 (请安装该插件, 或把 economy.type 改为 none)");
            want = EconomyType.NONE;
        }
        this.type = want;
        if (type == EconomyType.PLAYERPOINTS) {
            org.black_ixx.playerpoints.PlayerPoints pp = (org.black_ixx.playerpoints.PlayerPoints)
                    Bukkit.getPluginManager().getPlugin("PlayerPoints");
            pointsApi = pp.getAPI();
        }
        plugin.getLogger().info("金币联动: " + (type == EconomyType.NONE ? "已关闭" : type.name()));
    }

    /** 是否启用金币奖励 */
    public boolean isEnabled() {
        return type != EconomyType.NONE;
    }

    /** 发放金币; 失败时返回 false */
    public boolean give(Player player, int amount) {
        if (type == EconomyType.NONE || player == null || amount <= 0) {
            return false;
        }
        try {
            switch (type) {
                case ZPOINTS: {
                    dev.zpoints.ZPointsPlugin zp = (dev.zpoints.ZPointsPlugin)
                            Bukkit.getPluginManager().getPlugin("ZPoints");
                    if (zp == null || !zp.getEconomy().isPointsAvailable()) {
                        return false;
                    }
                    return zp.getEconomy().givePoints(player.getUniqueId(), amount);
                }
                case PLAYERPOINTS:
                    return pointsApi != null && pointsApi.give(player.getUniqueId(), amount);
                case VAULT: {
                    RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                            Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (rsp == null) {
                        return false;
                    }
                    return rsp.getProvider().depositPlayer(player, amount).transactionSuccess();
                }
                default:
                    return false;
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "发放金币给 " + player.getName() + " 失败: " + t);
            return false;
        }
    }

    /** 插件是否已加载且启用 */
    private boolean isLoaded(String name) {
        return Bukkit.getPluginManager().isPluginEnabled(name);
    }
}