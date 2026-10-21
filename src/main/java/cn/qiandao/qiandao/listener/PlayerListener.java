package cn.qiandao.qiandao.listener;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.data.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

/**
 * 玩家事件监听
 * - 进服: 更新玩家名/创建右侧排名计分板
 * - 退服: 移除计分板
 * - 右键补签卡: 打开补签面板
 */
public class PlayerListener implements Listener {

    private final QianDaoPlugin plugin;

    public PlayerListener(QianDaoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 更新玩家名(改名后保持data.yml中名称同步)
        PlayerData pd = plugin.getDataManager().get(player.getUniqueId());
        if (!pd.getName().equals(player.getName())) {
            pd.setName(player.getName());
        }
        plugin.getRankingManager().createBoard(player);
        plugin.getRankingManager().refreshAll();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getRankingManager().removeBoard(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 仅处理右键
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || !plugin.getCardManager().isCard(item)) {
            return;
        }
        // 右键使用补签卡 -> 打开补签面板(取消原交互, 防止误放置/使用)
        event.setCancelled(true);
        plugin.getGuiManager().openBuqian(event.getPlayer());
    }
}