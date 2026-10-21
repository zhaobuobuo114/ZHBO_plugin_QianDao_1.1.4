package cn.qiandao.qiandao.util;

import cn.qiandao.qiandao.config.ConfigManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期工具类
 * 核心概念: 服务器"今天"以配置的刷新时间为界,
 * 例如刷新时间为凌晨4点, 那么 03:59 仍属于前一天
 */
public final class DateUtil {

    /** 标准日期格式: yyyy-MM-dd */
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtil() {
    }

    /**
     * 获取当前签到日期(以配置的时区 + 刷新时间为准)
     * @param config 配置管理器
     * @return 当前签到日
     */
    public static LocalDate today(ConfigManager config) {
        ZonedDateTime now = ZonedDateTime.now(config.getZoneId());
        LocalTime refresh = LocalTime.of(config.getRefreshHour(), config.getRefreshMinute());
        // 还未到刷新时间, 属于前一天
        if (now.toLocalTime().isBefore(refresh)) {
            now = now.minusDays(1);
        }
        return now.toLocalDate();
    }

    /** 将日期格式化为 yyyy-MM-dd 字符串 */
    public static String fmt(LocalDate date) {
        return date.format(FORMAT);
    }

    /** 解析 yyyy-MM-dd 字符串 */
    public static LocalDate parse(String str) {
        try {
            return LocalDate.parse(str, FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    /** 星期几的中文名称, 如 "周三" */
    public static String weekName(LocalDate date) {
        String[] week = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return week[date.getDayOfWeek().getValue() - 1];
    }
}