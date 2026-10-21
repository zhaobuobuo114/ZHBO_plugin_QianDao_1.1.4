package cn.qiandao.qiandao.gui;

import cn.qiandao.qiandao.QianDaoPlugin;
import cn.qiandao.qiandao.data.PlayerData;
import cn.qiandao.qiandao.manager.RankingManager;
import cn.qiandao.qiandao.util.DateUtil;
import cn.qiandao.qiandao.util.EffectUtil;
import cn.qiandao.qiandao.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI 管理器 - 签到主面板 / 补签面板 / 确认面板 / 排行榜面板
 */
public class GuiManager {

    /** 本插件所有面板的持有者基类 */
    public static abstract class QdHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    public static class MainHolder extends QdHolder {
    }

    public static class BuqianHolder extends QdHolder {
    }

    public static class RankHolder extends QdHolder {
    }

    public static class ConfirmHolder extends QdHolder {
        public final LocalDate date;

        public ConfirmHolder(LocalDate date) {
            this.date = date;
        }
    }

    private static final Material GLASS = Material.STAINED_GLASS_PANE;
    private static final short GLASS_DATA = 15;

    private final QianDaoPlugin plugin;

    public GuiManager(QianDaoPlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== 打开面板 ====================

    /** 打开签到主面板 */
    public void openMain(Player player) {
        PlayerData pd = plugin.getDataManager().get(player.getUniqueId());
        int streak = plugin.getSignManager().getStreak(pd);
        boolean signedToday = plugin.getSignManager().isSignedToday(pd);
        int cards = plugin.getCardManager().count(player);
        List<Integer> cycleDays = plugin.getConfigManager().getCycleDays();
        // 下一个要签到的周期天
        int nextCycle = (streak % 7) + 1;

        Inventory inv = Bukkit.createInventory(new MainHolder(), 45,
                ItemBuilder.color("&8签到面板"));
        MainHolder holder = (MainHolder) inv.getHolder();
        holder.setInventory(inv);

        // 头部信息(玩家头颅)
        inv.setItem(4, buildHead(player, streak, pd.getTotal(), cards));

        // 七日奖励展示
        for (int day : cycleDays) {
            if (day < 1 || day > 7) {
                continue; // 仅展示第1~7天
            }
            inv.setItem(19 + day - 1, buildDayItem(day, streak, nextCycle, signedToday));
        }

        // 补签卡信息
        ItemStack cardShow = plugin.getCardManager().createCard(1);
        ItemBuilder cardBuilder = new ItemBuilder(cardShow);
        List<String> cardLore = new ArrayList<>();
        cardLore.addAll(cardShow.getItemMeta().hasLore()
                ? cardShow.getItemMeta().getLore() : new ArrayList<>());
        cardLore.add("");
        cardLore.add("&7当前持有: &a" + cards + " &7张");
        cardBuilder.lore(cardLore);
        inv.setItem(31, cardBuilder.build());

        // 底部按钮
        inv.setItem(37, new ItemBuilder(Material.PAPER)
                .name("&a补签漏签日期")
                .lore("&7点击选择要补签的日期",
                        "&7每次补签消耗 &e1 &7张补签卡")
                .build());
        inv.setItem(39, new ItemBuilder(Material.BOOK)
                .name("&6签到排行榜")
                .lore("&7点击查看全服签到排名")
                .build());
        inv.setItem(41, new ItemBuilder(Material.BARRIER)
                .name("&c关闭面板")
                .build());

        fillGlass(inv);
        player.closeInventory();
        player.openInventory(inv);
    }

    /** 打开补签面板 */
    public void openBuqian(Player player) {
        if (!plugin.getConfigManager().isBuqianEnabled()) {
            plugin.sendMessage(player, "buqian-disabled");
            return;
        }
        PlayerData pd = plugin.getDataManager().get(player.getUniqueId());
        List<LocalDate> missed = plugin.getSignManager().getMissedDays(pd);
        if (missed.isEmpty()) {
            plugin.sendMessage(player, "buqian-none");
            return;
        }

        Inventory inv = Bukkit.createInventory(new BuqianHolder(), 27,
                ItemBuilder.color("&8选择补签日期"));
        ((BuqianHolder) inv.getHolder()).setInventory(inv);

        // 顶部补签卡显示
        ItemStack card = plugin.getCardManager().createCard(1);
        List<String> cardLore = card.getItemMeta().hasLore()
                ? new ArrayList<>(card.getItemMeta().getLore()) : new ArrayList<>();
        cardLore.add("");
        cardLore.add("&7持有: &a" + plugin.getCardManager().count(player) + " &7张");
        inv.setItem(4, new ItemBuilder(card).lore(cardLore).build());

        // 漏签日期(最多展示7个)
        int show = Math.min(7, missed.size());
        for (int i = 0; i < show; i++) {
            LocalDate d = missed.get(i);
            inv.setItem(10 + i, new ItemBuilder(Material.PAPER)
                    .name("&e补签 &f" + DateUtil.fmt(d) + " &7(" + DateUtil.weekName(d) + ")")
                    .lore("&7补签该日需消耗 &a1 &7张补签卡",
                            "&7补签后视为该日已签到, 连续天数恢复",
                            "&7• 点击进入确认")
                    .build());
        }

        // 中间说明
        inv.setItem(13, new ItemBuilder(Material.SIGN)
                .name("&6补签说明")
                .lore("&7• 右键补签卡可直接打开本面板",
                        "&7• 只能补签最近 &e" + plugin.getConfigManager().getBuqianMaxDays() + " &7天内漏签的日期",
                        "&7• 同一天不能重复补签",
                        "&7• 补签不影响当天正常签到")
                .build());

        // 底部按钮
        inv.setItem(21, new ItemBuilder(Material.ARROW)
                .name("&a返回签到面板")
                .build());
        inv.setItem(23, new ItemBuilder(Material.BARRIER)
                .name("&c关闭面板")
                .build());

        fillGlass(inv);
        player.closeInventory();
        player.openInventory(inv);
    }

    /** 打开补签确认面板 */
    public void openConfirm(Player player, LocalDate date) {
        Inventory inv = Bukkit.createInventory(new ConfirmHolder(date), 9,
                ItemBuilder.color("&8确认补签"));
        ((ConfirmHolder) inv.getHolder()).setInventory(inv);

        inv.setItem(3, new ItemBuilder(Material.WOOL)
                .data(13)
                .name("&a确认补签")
                .lore("&7日期: &e" + DateUtil.fmt(date),
                        "&7消耗: &e1 &7张补签卡")
                .build());
        inv.setItem(4, new ItemBuilder(Material.PAPER)
                .name("&e补签日期")
                .lore("&f" + DateUtil.fmt(date) + " &7(" + DateUtil.weekName(date) + ")",
                        "&7补签后视为该日已签到")
                .build());
        inv.setItem(5, new ItemBuilder(Material.WOOL)
                .data(14)
                .name("&c取消")
                .build());

        fillGlass(inv);
        player.closeInventory();
        player.openInventory(inv);
    }

    /** 打开排行榜面板 */
    public void openRank(Player player) {
        List<RankingManager.RankEntry> all = plugin.getRankingManager().computeRank();
        Inventory inv = Bukkit.createInventory(new RankHolder(), 45,
                ItemBuilder.color("&6签到排行榜"));
        ((RankHolder) inv.getHolder()).setInventory(inv);

        inv.setItem(4, new ItemBuilder(Material.BOOK)
                .name("&6签到排行榜")
                .lore("&7按连续签到天数排名, 相同天数按累计次数",
                        "&8QianDao v" + plugin.getDescription().getVersion())
                .build());

        // 前10名
        int show = Math.min(10, all.size());
        String[] topColor = {"§6", "§7", "§b"};
        for (int i = 0; i < show; i++) {
            RankingManager.RankEntry e = all.get(i);
            String color = i < topColor.length ? topColor[i] : "§f";
            ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwner(e.name);
            meta.setDisplayName(ItemBuilder.color(color + "第" + (i + 1) + "名 §r§f" + e.name));
            List<String> lore = new ArrayList<>();
            lore.add(ItemBuilder.color("&7连续签到: &a" + e.streak + " &7天"));
            lore.add(ItemBuilder.color("&7累计签到: &a" + e.total + " &7次"));
            meta.setLore(lore);
            skull.setItemMeta(meta);
            inv.setItem(9 + i, skull);
        }

        // 我的排名
        PlayerData pd = plugin.getDataManager().get(player.getUniqueId());
        int myRank = plugin.getRankingManager().getPlayerRank(player, all);
        if (myRank > 0) {
            inv.setItem(31, new ItemBuilder(Material.PAPER)
                    .name("&e我的排名: &6#" + myRank)
                    .lore("&7连续签到: &a" + plugin.getSignManager().getStreak(pd) + " &7天",
                            "&7累计签到: &a" + pd.getTotal() + " &7次")
                    .build());
        } else {
            inv.setItem(31, new ItemBuilder(Material.PAPER)
                    .name("&7我的排名: &8未上榜")
                    .lore("&7快去签到吧~")
                    .build());
        }

        inv.setItem(39, new ItemBuilder(Material.ARROW)
                .name("&a返回签到面板")
                .build());
        inv.setItem(41, new ItemBuilder(Material.BARRIER)
                .name("&c关闭面板")
                .build());

        fillGlass(inv);
        player.closeInventory();
        player.openInventory(inv);
    }

    // ==================== 点击处理 ====================

    /** 处理本插件面板内的点击事件 */
    public void handleClick(Player player, QdHolder holder, int rawSlot) {
        if (holder instanceof MainHolder) {
            handleMainClick(player, rawSlot);
        } else if (holder instanceof BuqianHolder) {
            handleBuqianClick(player, rawSlot);
        } else if (holder instanceof ConfirmHolder) {
            handleConfirmClick(player, (ConfirmHolder) holder, rawSlot);
        } else if (holder instanceof RankHolder) {
            handleRankClick(player, rawSlot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        EffectUtil.guiClick(plugin, player);
        PlayerData pd = plugin.getDataManager().get(player.getUniqueId());
        boolean signedToday = plugin.getSignManager().isSignedToday(pd);
        int streak = plugin.getSignManager().getStreak(pd);
        int nextCycle = (streak % 7) + 1;

        // 周期奖励槽位(19~25)
        if (slot >= 19 && slot <= 25) {
            int day = slot - 18;
            if (signedToday || day != nextCycle) {
                // 已签到或不是今天该领取的天数
                EffectUtil.guiError(plugin, player);
                return;
            }
            // 执行签到
            int newStreak = plugin.getSignManager().sign(player, pd);
            if (newStreak <= 0) {
                plugin.sendMessage(player, "sign-already");
                EffectUtil.guiError(plugin, player);
            } else {
                int cycleDay = ((newStreak - 1) % 7) + 1;
                plugin.sendMessage(player, "sign-success",
                        "streak", String.valueOf(newStreak),
                        "cycle", String.valueOf(cycleDay));
                // 标题提示
                if (plugin.getConfigManager().isTitleEnabled()) {
                    player.sendTitle(
                            ItemBuilder.color(plugin.getConfigManager().getTitleMain()),
                            ItemBuilder.color(plugin.getConfigManager().getTitleSub()
                                    .replace("{cycle}", String.valueOf(cycleDay))
                                    .replace("{streak}", String.valueOf(newStreak))),
                            10, 50, 10);
                }
                // 音效 + 粒子特效(可在配置中关闭)
                EffectUtil.signEffect(plugin, player);
                plugin.getRankingManager().refreshAll();
                openMain(player); // 刷新面板
            }
            return;
        }
        switch (slot) {
            case 37: // 补签按钮
                openBuqian(player);
                break;
            case 39: // 排行榜
                openRank(player);
                break;
            case 41: // 关闭
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void handleBuqianClick(Player player, int slot) {
        EffectUtil.guiClick(plugin, player);
        PlayerData pd = plugin.getDataManager().get(player.getUniqueId());
        List<LocalDate> missed = plugin.getSignManager().getMissedDays(pd);
        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            if (index < missed.size()) {
                openConfirm(player, missed.get(index));
            }
            return;
        }
        switch (slot) {
            case 21:
                openMain(player);
                break;
            case 23:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    private void handleConfirmClick(Player player, ConfirmHolder holder, int slot) {
        EffectUtil.guiClick(plugin, player);
        if (slot == 3) {
            // 确认补签
            if (!plugin.getConfigManager().isBuqianEnabled()) {
                plugin.sendMessage(player, "buqian-disabled");
                player.closeInventory();
                return;
            }
            if (!plugin.getCardManager().takeOne(player)) {
                plugin.sendMessage(player, "buqian-no-card");
                EffectUtil.guiError(plugin, player);
                player.closeInventory();
                return;
            }
            int newStreak = plugin.getSignManager().buqian(player,
                    plugin.getDataManager().get(player.getUniqueId()), holder.date);
            if (newStreak <= 0) {
                // 日期已失效(已签到/超期), 退还补签卡
                plugin.getCardManager().give(player, 1);
                plugin.sendMessage(player, "buqian-none");
                EffectUtil.guiError(plugin, player);
                openMain(player);
                return;
            }
            plugin.sendMessage(player, "buqian-success",
                    "date", DateUtil.fmt(holder.date),
                    "streak", String.valueOf(newStreak));
            // 音效 + 粒子特效(可在配置中关闭)
            EffectUtil.buqianEffect(plugin, player);
            plugin.getRankingManager().refreshAll();
            // 还有漏签日期则回到补签面板, 否则回到主面板
            if (plugin.getSignManager().getMissedDays(
                    plugin.getDataManager().get(player.getUniqueId())).isEmpty()) {
                openMain(player);
            } else {
                openBuqian(player);
            }
        } else if (slot == 5) {
            // 取消, 返回补签面板
            openBuqian(player);
        }
    }

    private void handleRankClick(Player player, int slot) {
        EffectUtil.guiClick(plugin, player);
        switch (slot) {
            case 39:
                openMain(player);
                break;
            case 41:
                player.closeInventory();
                break;
            default:
                break;
        }
    }

    // ==================== 物品构建 ====================

    /** 玩家头颅信息 */
    private ItemStack buildHead(Player player, int streak, int total, int cards) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setOwner(player.getName());
        meta.setDisplayName(ItemBuilder.color("&6" + player.getName() + " &e的签到面板"));
        int hour = plugin.getConfigManager().getRefreshHour();
        int minute = plugin.getConfigManager().getRefreshMinute();
        List<String> lore = new ArrayList<>();
        lore.add(ItemBuilder.color("&7连续签到: &a" + streak + " &7天"));
        lore.add(ItemBuilder.color("&7累计签到: &a" + total + " &7次"));
        lore.add(ItemBuilder.color("&7补签卡: &a" + cards + " &7张"));
        lore.add("");
        lore.add(ItemBuilder.color("&7每日 &e" + String.format("%02d:%02d", hour, minute) + " &7刷新"));
        lore.add(ItemBuilder.color("&7周期: &e7天一轮&7, 断签连续天数归零"));
        lore.add("");
        lore.add(ItemBuilder.color("&8QianDao v" + plugin.getDescription().getVersion()));
        meta.setLore(lore);
        head.setItemMeta(meta);
        return head;
    }

    /** 周期某一天的展示物品 */
    private ItemStack buildDayItem(int day, int streak, int nextCycle, boolean signedToday) {
        ItemStack icon = plugin.getConfigManager().getCycleIcon(day);
        ItemBuilder builder = new ItemBuilder(icon);
        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfigManager().getCycleDisplay(day)) {
            lore.add("&7" + line);
        }
        lore.add("");
        // 状态: 已签(不发光) / 今日可签(发光) / 未解锁
        String status;
        if (day == nextCycle && !signedToday) {
            status = "§e▶ 点击签到!";
            // 今日可领取的槽位附魔发光, 突出显示; 已签到的槽位不发光
            if (plugin.getConfigManager().isGlowToday()) {
                builder.glow();
            }
        } else if (day < nextCycle) {
            status = "§a✔ 已签到";
        } else {
            status = "§8○ 未解锁";
        }
        lore.add(status);
        builder.lore(lore);
        return builder.build();
    }

    /** 用黑色玻璃板填充面板空白处 */
    private void fillGlass(Inventory inv) {
        ItemStack glass = new ItemStack(GLASS, 1, GLASS_DATA);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, glass);
            }
        }
    }
}