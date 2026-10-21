package cn.qiandao.qiandao.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 物品构建工具类 - 链式构建 GUI 展示物品
 */
public final class ItemBuilder {

    private final ItemStack item;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
    }

    /** 基于已有物品构建(克隆, 不修改原物品) */
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
    }

    /** 设置耐久值(1.12.2 中用于染色玻璃、羊毛颜色等子类型) */
    public ItemBuilder data(int data) {
        item.setDurability((short) data);
        return this;
    }

    /** 设置展示名称, 支持 & 颜色代码 */
    public ItemBuilder name(String name) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return this;
    }

    /** 设置描述, 支持 & 颜色代码 */
    public ItemBuilder lore(List<String> lore) {
        ItemMeta meta = item.getItemMeta();
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(color(line));
        }
        meta.setLore(colored);
        item.setItemMeta(meta);
        return this;
    }

    /** 设置描述(可变参数), 支持 & 颜色代码 */
    public ItemBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    /** 添加附魔光效(纯视觉效果, 隐藏附魔属性) */
    public ItemBuilder glow() {
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return item;
    }

    /** 翻译 & 颜色代码 */
    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}