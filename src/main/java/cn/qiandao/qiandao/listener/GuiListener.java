package cn.qiandao.qiandao.listener;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.gui.GuiManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * GUI 点击监听 - 处理签到面板内的所有点击, 并阻止物品被移动/拿取
 */
public class GuiListener implements Listener {

    private final QianDaoPlugin plugin;

    public GuiListener(QianDaoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        // 只处理本插件面板
        if (top == null || !(top.getHolder() instanceof GuiManager.QdHolder)) {
            return;
        }
        // 面板内的任何物品移动一律禁止
        event.setCancelled(true);
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= top.getSize()) {
            return; // 点击在面板之外(玩家背包/界面外)
        }
        Player player = (Player) event.getWhoClicked();
        plugin.getGuiManager().handleClick(player, (GuiManager.QdHolder) top.getHolder(), raw);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof GuiManager.QdHolder) {
            event.setCancelled(true);
        }
    }
}