package cn.qiandao.qiandao.manager;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.config.ConfigManager;
import cn.qiandao.qiandao.util.NBTUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * 补签卡管理器
 * 补签卡来源:
 * 1. 在线奖励(累计在线达到配置时长自动发放, 见 OnlineReward 定时任务)
 * 2. 活动发放(管理员执行 /qd givecard)
 */
public class CardManager {

    /** 补签卡 NBT 标识, 防止玩家伪造 */
    private static final String CARD_TAG = "QianDao_BuQianCard";

    private final QianDaoPlugin plugin;
    private final ConfigManager config;

    public CardManager(QianDaoPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    /** 创建一张(或一叠)补签卡 */
    public ItemStack createCard(int amount) {
        ItemStack item = config.createBuqianCard();
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return NBTUtil.setTag(item, CARD_TAG, "true");
    }

    /** 判断物品是否为补签卡 */
    public boolean isCard(ItemStack item) {
        return NBTUtil.hasTag(item, CARD_TAG);
    }

    /** 统计玩家背包中的补签卡数量 */
    public int count(Player player) {
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isCard(item)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    /**
     * 扣减 1 张补签卡(优先扣主手)
     * @return 是否扣减成功
     */
    public boolean takeOne(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack hand = inv.getItemInMainHand();
        // 优先扣主手
        if (isCard(hand)) {
            if (hand.getAmount() > 1) {
                hand.setAmount(hand.getAmount() - 1);
                return true;
            }
            inv.setItemInMainHand(null);
            return true;
        }
        // 其他位置
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isCard(contents[i])) {
                ItemStack it = contents[i];
                if (it.getAmount() > 1) {
                    it.setAmount(it.getAmount() - 1);
                    return true;
                }
                inv.clear(i);
                return true;
            }
        }
        return false;
    }

    /** 给予玩家补签卡, 背包放不下则掉落在脚边 */
    public void give(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        Map<Integer, ItemStack> left = inv.addItem(createCard(amount));
        if (!left.isEmpty()) {
            for (ItemStack stack : left.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
            plugin.sendMessage(player, "card-inventory-full");
        }
    }
}