package cn.qiandao.qiandao.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 单个玩家的签到数据
 * 所有方法加锁, 保证异步保存(快照)与主线程修改互不冲突
 */
public class PlayerData {

    /** 数据快照 - 用于异步保存时隔离读 */
    public static class Snapshot {
        public final String name;
        public final Set<String> checkDays;
        public final Set<String> buqianDays;
        public final int total;
        public final List<Integer> awarded;
        public final String onlineDate;
        public final int onlineMinutes;
        public final int onlineCardsToday;

        Snapshot(String name, Set<String> checkDays, Set<String> buqianDays, int total,
                 List<Integer> awarded, String onlineDate, int onlineMinutes, int onlineCardsToday) {
            this.name = name;
            this.checkDays = checkDays;
            this.buqianDays = buqianDays;
            this.total = total;
            this.awarded = awarded;
            this.onlineDate = onlineDate;
            this.onlineMinutes = onlineMinutes;
            this.onlineCardsToday = onlineCardsToday;
        }
    }

    private final String uuid;
    private volatile String name;
    /** 正常签到的日期集合 (yyyy-MM-dd) */
    private final Set<String> checkDays = new HashSet<>();
    /** 补签的日期集合 (yyyy-MM-dd) */
    private final Set<String> buqianDays = new HashSet<>();
    /** 累计签到次数 */
    private int total;
    /** 本连续周期内已发放的额外奖励档位 (7/14/30), 断签归零时清空 */
    private final List<Integer> awarded = new ArrayList<>();
    /** 在线奖励统计日期 (yyyy-MM-dd) */
    private String onlineDate = "";
    /** 当日累计在线分钟数 */
    private int onlineMinutes;
    /** 当日已通过在线获得的补签卡数量 */
    private int onlineCardsToday;

    public PlayerData(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public synchronized String getUuid() {
        return uuid;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized Set<String> getCheckDays() {
        return checkDays;
    }

    public synchronized Set<String> getBuqianDays() {
        return buqianDays;
    }

    public synchronized int getTotal() {
        return total;
    }

    public synchronized void setTotal(int total) {
        this.total = total;
    }

    public synchronized void addTotal(int delta) {
        this.total += delta;
    }

    public synchronized List<Integer> getAwarded() {
        return awarded;
    }

    public synchronized String getOnlineDate() {
        return onlineDate;
    }

    public synchronized void setOnlineDate(String onlineDate) {
        this.onlineDate = onlineDate;
    }

    public synchronized int getOnlineMinutes() {
        return onlineMinutes;
    }

    public synchronized void setOnlineMinutes(int onlineMinutes) {
        this.onlineMinutes = onlineMinutes;
    }

    public synchronized int getOnlineCardsToday() {
        return onlineCardsToday;
    }

    public synchronized void setOnlineCardsToday(int onlineCardsToday) {
        this.onlineCardsToday = onlineCardsToday;
    }

    /** 生成只读快照, 供异步保存使用 */
    public synchronized Snapshot snapshot() {
        return new Snapshot(name,
                new HashSet<>(checkDays),
                new HashSet<>(buqianDays),
                total,
                new ArrayList<>(awarded),
                onlineDate,
                onlineMinutes,
                onlineCardsToday);
    }
}