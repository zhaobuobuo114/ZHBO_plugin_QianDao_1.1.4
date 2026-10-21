package cn.qiandao.qiandao.util;

import cn.qiandao.qiandao.QianDaoPlugin;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * 音效与粒子特效工具类
 * 所有音效/粒子类型/开关均可通过 config.yml 的 effects 节点配置
 */
public final class EffectUtil {

    private EffectUtil() {
    }

    /** 签到成功: 音效 + 粒子特效 */
    public static void signEffect(QianDaoPlugin plugin, Player player) {
        sound(plugin, player, "effects.sound-sign", Sound.ENTITY_PLAYER_LEVELUP);
        particle(plugin, player);
    }

    /** 补签成功: 音效 + 粒子特效 */
    public static void buqianEffect(QianDaoPlugin plugin, Player player) {
        sound(plugin, player, "effects.sound-buqian", Sound.ENTITY_PLAYER_LEVELUP);
        particle(plugin, player);
    }

    /** 在线奖励发放补签卡音效 */
    public static void onlineCardEffect(QianDaoPlugin plugin, Player player) {
        sound(plugin, player, "effects.sound-online-card", Sound.ENTITY_ITEM_PICKUP);
    }

    /** GUI 普通点击音效 */
    public static void guiClick(QianDaoPlugin plugin, Player player) {
        sound(plugin, player, "effects.sound-gui-click", Sound.UI_BUTTON_CLICK);
    }

    /** GUI 无效操作音效 */
    public static void guiError(QianDaoPlugin plugin, Player player) {
        sound(plugin, player, "effects.sound-gui-error", Sound.ENTITY_VILLAGER_NO);
    }

    /** 播放音效(受 sound-enable 总开关控制) */
    private static void sound(QianDaoPlugin plugin, Player player, String path, Sound fallback) {
        if (!plugin.getConfigManager().isSoundEnabled()) {
            return;
        }
        player.playSound(player.getLocation(),
                plugin.getConfigManager().getSound(path, fallback), 1.0f, 1.0f);
    }

    /** 播放粒子特效(受 particle-enable 总开关控制) */
    private static void particle(QianDaoPlugin plugin, Player player) {
        if (!plugin.getConfigManager().isParticleEnabled()) {
            return;
        }
        Particle particle = plugin.getConfigManager().getParticleType();
        int amount = plugin.getConfigManager().getParticleAmount();
        player.getWorld().spawnParticle(particle,
                player.getLocation().add(0, 1, 0), amount, 0.5, 0.5, 0.5);
    }
}