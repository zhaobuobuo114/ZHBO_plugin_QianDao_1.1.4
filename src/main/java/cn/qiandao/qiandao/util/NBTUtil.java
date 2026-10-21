package cn.qiandao.qiandao.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/**
 * NBT 工具类 (1.12.2 专用)
 * 1.12.2 的 Bukkit API 不支持自定义 NBT 标签, 通过反射调用 NMS 实现,
 * 用于给补签卡打上无法伪造的标识
 */
public final class NBTUtil {

    private static String version;

    private NBTUtil() {
    }

    /** 获取服务器 NMS 版本, 如 v1_12_R1 */
    private static String version() {
        if (version == null) {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            version = pkg.substring(pkg.lastIndexOf('.') + 1);
        }
        return version;
    }

    private static Class<?> obcClass(String name) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + version() + "." + name);
    }

    private static Class<?> nmsClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + version() + "." + name);
    }

    /**
     * 给物品设置字符串类型的 NBT 标签
     * @return 携带标签的新物品; 失败时返回原物品
     */
    public static ItemStack setTag(ItemStack item, String key, String value) {
        try {
            Class<?> craftItemStack = obcClass("inventory.CraftItemStack");
            Class<?> nmsItemStack = nmsClass("ItemStack");
            Class<?> nbtTagCompound = nmsClass("NBTTagCompound");

            Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            Method asBukkitCopy = craftItemStack.getMethod("asBukkitCopy", nmsItemStack);

            // 转为 NMS 物品并挂载标签
            Object nms = asNMSCopy.invoke(null, item);
            Object tag = nmsItemStack.getMethod("getTag").invoke(nms);
            if (tag == null) {
                tag = nbtTagCompound.newInstance();
            }
            nbtTagCompound.getMethod("setString", String.class, String.class).invoke(tag, key, value);
            nmsItemStack.getMethod("setTag", nbtTagCompound).invoke(nms, tag);

            // 转回 Bukkit 物品(新实例, 已携带NBT标签)
            return (ItemStack) asBukkitCopy.invoke(null, nms);
        } catch (Exception e) {
            return item;
        }
    }

    /** 判断物品是否携带指定 NBT 标签 */
    public static boolean hasTag(ItemStack item, String key) {
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            return false;
        }
        try {
            Class<?> craftItemStack = obcClass("inventory.CraftItemStack");
            Class<?> nmsItemStack = nmsClass("ItemStack");
            Class<?> nbtTagCompound = nmsClass("NBTTagCompound");

            Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", ItemStack.class);
            Object nms = asNMSCopy.invoke(null, item);
            Object tag = nmsItemStack.getMethod("getTag").invoke(nms);
            if (tag == null) {
                return false;
            }
            return (Boolean) nbtTagCompound.getMethod("hasKey", String.class).invoke(tag, key);
        } catch (Exception e) {
            return false;
        }
    }
}