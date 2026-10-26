# 【ZHBO】plugin_QianDao_1.1.4 · 每日签到插件

> Minecraft **1.12.2**（Spigot / Paper）服务端签到插件 —— 七日周期奖励、连续签到额外奖励、补签卡、签到排行榜、经济插件联动，全部配置化。

![version](https://img.shields.io/badge/version-1.1.4-2ea44f)
![platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper%201.12.2-orange)
![java](https://img.shields.io/badge/Java-8-blue)
![config](https://img.shields.io/badge/%E5%85%A8%E9%85%8D%E7%BD%AE%E5%8C%96-YAML-informational)
[![download](https://img.shields.io/badge/%E4%B8%8B%E8%BD%BD-ZHBO--plugin__QianDao__1.1.4.jar-2ea44f?logo=github)](https://github.com/zhaobuobuo114/ZHBO_plugin_QianDao_1.1.4/releases/latest)
[![docs](https://img.shields.io/badge/%E6%96%87%E6%A1%A3-%E5%9C%A8%E7%BA%BF%E6%9F%A5%E7%9C%8B-3fb950)](https://zhaobuobuo114.github.io/plugin-wiki/qiantao/)

> 📖 **在线文档**：<https://zhaobuobuo114.github.io/plugin-wiki/qiantao/>
> 文档已迁到独立的插件文档总站，本站只放代码与发布包。

> 👤 **作者**：zhaobuobuo ｜ 📧 **联系方式**：1071576270@QQ.COM

---

## 目录

- [简介](#简介)
- [功能特性](#功能特性)
- [环境要求](#环境要求)
- [安装](#安装)
- [指令与权限](#指令与权限)
- [界面说明](#界面说明)
- [配置文件详解](#配置文件详解)
- [经济联动](#经济联动)
- [数据存储](#数据存储)
- [编译构建](#编译构建)
- [常见问题](#常见问题)
- [更新日志](#更新日志)
- [插件定制](#插件定制)

---

## 简介

`【ZHBO】plugin_QianDao_1.1.4` 是一套开箱即用的每日签到系统。玩家通过 `/qd` 打开图形化签到面板，一键领取当天奖励；插件以 **7 天为一个奖励周期**，第 7 天为终极大奖，第 8 天重新从第 1 天循环（连续天数继续累计）。漏签可以用**补签卡**补救，连续签到 7 / 14 / 30 天还有额外奖励与全服广播。

所有奖励物品、金币数量、提示文本、音效粒子均可通过 `config.yml` 修改，**不需要改代码**。奖励通过控制台指令发放，因此可以接入任意物品 / 经济 / 点券插件。

## 功能特性

| 功能 | 说明 |
| --- | --- |
| 每日签到 | 每天限签一次，默认凌晨 4:00 刷新（时区与刷新时间可配置） |
| 七日周期 | 第 1~7 天逐天递增，第 7 天终极大奖，第 8 天回到第 1 天，连续天数继续累计 |
| 连续奖励 | 连续 7 / 14 / 30 天额外奖励，可配置全服广播；断签后连续天数归零重算 |
| 补签卡 | 右键使用打开补签面板，可补最近 N 天内的漏签日期，补签后连续天数恢复 |
| 在线奖励 | 累计在线达到设定时长自动发放补签卡，每日有上限 |
| 签到排行 | 屏幕右侧计分板实时显示前 10 名（按连续天数、再按累计次数排序） |
| 排行榜 GUI | `/qd rank` 打开图形化排行榜，展示名次 / 连续天数 / 累计次数 |
| 金币联动 | 支持 ZPoints、PlayerPoints、Vault（EssentialsX 等），未安装时自动跳过 |
| 视听反馈 | 签到标题、升级音效、粒子特效、今日槽位附魔发光，均可开关 |
| 数据安全 | 玩家数据写入 `data.yml`，定时异步保存，自动清理 60 天前的旧记录 |

## 环境要求

- 服务端：**Spigot / Paper 1.12.2**（其他版本不保证兼容，材质名与音效名为 1.12.2 规范）
- Java：**8** 及以上
- 可选前置（按需）：
  - [Vault](https://www.spigotmc.org/resources/vault.34315/) + 经济插件（EssentialsX 等）
  - [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745/)
  - ZPoints（点券插件）

> 以上前置均为 **softdepend**，不装也能正常签到，只是对应金币奖励自动跳过。

## 安装

1. 到 [Releases](https://github.com/zhaobuobuo114/ZHBO_plugin_QianDao_1.1.4/releases/latest) 下载插件本体 `ZHBO-plugin_QianDao_1.1.4.jar`（源工程与本地构建产物名为 `【ZHBO】plugin_QianDao_1.1.4.jar`，GitHub 附件不支持书名号会自动改写），放入服务端 `plugins/` 目录（也可按 [编译构建](#编译构建) 自行打包，产物为 `【ZHBO】plugin_QianDao_1.1.4.jar`）。
2. 启动服务器，插件会自动生成 `plugins/ZHBO_plugin_QianDao_1.1.4/config.yml`。
3. 按需修改 `config.yml`（奖励、金币、刷新时间等）。
4. 执行 `/qd reload` 重载，或重启服务器。
5. 玩家执行 `/qd` 即可打开签到面板。

> **升级提示**：新版本可能新增配置项。升级后请先备份并删除旧 `config.yml`，让插件重新生成，再把自定义内容迁移回去。
> **目录变更**：插件已更名为 `【ZHBO】plugin_QianDao_1.1.4`（插件内部名 `ZHBO_plugin_QianDao_1.1.4`），数据目录由 `plugins/QianDao/` 变为 `plugins/ZHBO_plugin_QianDao_1.1.4/`。升级时请把旧的 `config.yml` 与 `data.yml` 整体迁移到新目录。

## 指令与权限

主命令 `/qiandao`，别名 `/qd`、`/sign`。

| 指令 | 说明 | 权限 |
| --- | --- | --- |
| `/qd` | 打开签到面板 | `qiandao.use` |
| `/qd rank` | 打开签到排行榜 | `qiandao.use` |
| `/qd help` | 查看帮助（管理员会额外显示管理指令） | `qiandao.use` |
| `/qd givecard <玩家> [数量]` | 发放补签卡，数量 1~2304 | `qiandao.admin` |
| `/qd reload` | 重载配置文件 | `qiandao.admin` |

| 权限节点 | 说明 | 默认 |
| --- | --- | --- |
| `qiandao.use` | 使用签到功能 | 所有人 |
| `qiandao.admin` | 管理命令（发卡 / 重载） | OP |

命令支持 Tab 补全：子命令按权限补全，`givecard` 会补全在线玩家名。

## 界面说明

**签到主面板（45 格）**

- 第 5 格：玩家头颅，显示连续签到天数、累计签到次数、持有补签卡数量
- 第 20~26 格：七日奖励展示，今日可领取的槽位附魔发光
- 第 32 格：补签卡展示与当前持有数量
- 底部按钮：补签漏签日期 / 签到排行榜 / 关闭面板

**补签面板（27 格）**

- 展示最近 N 天内可补签的漏签日期（最多 7 个）
- 点击日期进入二次确认，确认后消耗 1 张补签卡
- 同一天不可重复补签，补签不影响当天正常签到

**排行榜面板（45 格）**

- 按连续签到天数降序，相同天数按累计签到次数降序
- 展示玩家头颅、名次、连续天数、累计次数

## 配置文件详解

配置文件位于 `plugins/ZHBO_plugin_QianDao_1.1.4/config.yml`，全部支持 `&` 颜色代码。

### general · 基础设置

```yaml
general:
  timezone: "Asia/Shanghai"   # 计算"今天"所用时区
  refresh-hour: 4             # 每日刷新时间（24 小时制）
  refresh-minute: 0
  auto-save-seconds: 300      # 玩家数据自动保存间隔（秒）
```

### cycle · 七日周期奖励

```yaml
cycle:
  1:
    icon: "WOOD"              # GUI 图标材质（1.12.2 材质名）
    icon-data: 0              # 可选，子 ID / 耐久值，如 5 = 龙头
    name: "&f第1天"
    display:                  # GUI 中的奖励说明，不填则直接显示 commands
      - "&7铁锭 x5"
    commands:                 # 以控制台身份执行，%player% 替换为玩家名
      - "give %player% iron_ingot 5"
```

- 第 1~7 天必须配置，第 7 天通常放终极大奖。
- `commands` 可写任意控制台指令，例如 `eco give %player% 1000`、`points give %player% 100`、`crate key %player% vote 1`。

### milestones · 连续签到额外奖励

```yaml
milestones:
  7:
    display:
      - "&7金锭 x8"
    commands:
      - "give %player% gold_ingot 8"
    broadcast: "&e[签到] &6%player% &e已连续签到 &6&l7天 &e, 获得额外小奖励!"
```

- 支持任意天数键（默认示例为 7 / 14 / 30），与周期奖励**叠加**发放。
- `broadcast` 留空 `""` 则不广播；`%player%` 会被替换。
- 同一轮连续签到内，每个里程碑只发放一次。

### buqian · 补签设置

```yaml
buqian:
  enabled: true
  max-days: 3                 # 只能补最近 N 天，建议 ≤ 7（界面最多展示 7 个）
  give-cycle-reward: true     # 补签是否发放该日对应的周期奖励
  item:
    material: "PAPER"
    name: "&a&l补签卡"
    glow: true
    lore:
      - "&7右键使用"
      - "&7可补签最近3天内漏签的日期"
      - "&7补签后连续天数恢复"
```

### economy · 金币奖励

```yaml
economy:
  type: "zpoints"                       # none | zpoints | playerpoints | vault
  cycle-gold: [10, 12, 15, 18, 20, 25, 50]  # 周期第 1~7 天的金币
  streak-bonus:
    7: 50
    14: 100
    30: 300
  buqian-gold: false                    # 补签是否发放金币（按第 1 天数量）
```

### online-reward · 在线奖励

```yaml
online-reward:
  enabled: true
  interval-minutes: 60        # 累计在线多少分钟获得 1 张补签卡
  daily-max: 1                # 每天最多通过在线奖励获得几张
```

统计随每日刷新时间重置。

### ranking · 签到排行

```yaml
ranking:
  scoreboard-enable: true     # 右侧计分板开关
  scoreboard-title: "&6&l签到排行榜"
  amount: 10                  # 上榜人数
```

排名规则：连续签到天数降序 → 累计签到次数降序 → 玩家名。使用每位玩家独立计分板，不干扰服务器主计分板。

### effects / sign-title · 音效与特效

```yaml
effects:
  glow-today: true
  sound-enable: true
  sound-sign: "ENTITY_PLAYER_LEVELUP"
  sound-buqian: "ENTITY_PLAYER_LEVELUP"
  sound-online-card: "ENTITY_ITEM_PICKUP"
  sound-gui-click: "UI_BUTTON_CLICK"
  sound-gui-error: "ENTITY_VILLAGER_NO"
  particle-enable: true
  particle-type: "FIREWORKS_SPARK"
  particle-amount: 30

sign-title:
  enable: true
  main: "&6&l签到成功"
  sub: "&e周期第{cycle}天奖励已发放"
```

音效名与粒子名参考 [1.12.2 音效文档](https://helpch.at/docs/1.12.2/)。

### messages · 消息文本

所有提示均在 `messages` 下，支持 `&` 颜色代码与占位符：

| 占位符 | 含义 |
| --- | --- |
| `{streak}` | 当前连续签到天数 |
| `{cycle}` | 当前周期第几天（1~7） |
| `{gold}` | 本次获得的金币数 |
| `{date}` | 补签日期（yyyy-MM-dd） |
| `{player}` / `{card}` | 目标玩家 / 补签卡数量 |
| `{usage}` | 正确用法提示 |

`help-user` 为普通玩家可见帮助，`help-admin` 仅管理员可见。

## 经济联动

`economy.type` 支持四种取值，插件启动时会检测对应前置是否已启用，**未检测到则自动跳过金币奖励并在控制台提示**，不影响签到本身。

| type | 前置插件 | 发放内容 |
| --- | --- | --- |
| `none` | — | 不发放金币 |
| `zpoints` | ZPoints | 点券，可在 ZPoints 商店 / 抽卡中使用 |
| `playerpoints` | PlayerPoints | 点券 |
| `vault` | Vault + 经济插件（EssentialsX 等） | 经济账户金钱 |

## 数据存储

- 路径：`plugins/ZHBO_plugin_QianDao_1.1.4/data.yml`
- 结构：以玩家 UUID 为键，保存玩家名、已签到日期、已补签日期、累计次数、已发放里程碑、在线统计
- 保存：按 `general.auto-save-seconds` 异步定时保存，停服时同步保存
- 清理：自动移除 60 天前的日期记录，避免文件无限膨胀

> 备份 / 迁移服务器时，复制 `config.yml` 与 `data.yml` 即可。

## 编译构建

```bash
git clone <本仓库地址>
cd ZHBO_plugin_QianDao_1.1.4
mvn clean package
```

产物：`target/【ZHBO】plugin_QianDao_1.1.4.jar`

构建说明：

- 使用 **Maven**，JDK 8，目标字节码 1.8。
- `plugins/` 目录下的 `Vault-1.7.3.jar`、`PlayerPoints-2.1.5.jar`、`zpoints-1.3.0.jar` 是 **system scope 依赖**，仅用于编译期引用，不会被打进最终 jar，运行时由服务端提供。
- 如需更换 API 版本，修改 `pom.xml` 中的 `spigot-api` 版本即可（材质与音效名需同步适配）。

## 常见问题

**Q：签到成功但没收到金币？**
A：检查控制台启动日志中的 `金币联动: xxx` 输出。若显示 `已关闭`，说明 `economy.type` 配置的经济插件未安装或未启用；改成已安装的类型，或设为 `none`。

**Q：改了奖励物品没生效？**
A：确认执行了 `/qd reload`，并检查 `cycle` 下的 `commands` 是否写对；`%player%` 必须保留，指令由控制台执行。

**Q：补签面板里没有可选日期？**
A：补签只能补「最近 `max-days` 天内、且在该玩家首次签到之后」的漏签日期。新玩家没有历史记录，自然没有可补签日期。

**Q：升级后部分配置项丢失 / 报错？**
A：删除 `plugins/ZHBO_plugin_QianDao_1.1.4/config.yml` 让插件重新生成，再把自定义值迁回新文件。

**Q：右侧排行榜不显示？**
A：确认 `ranking.scoreboard-enable: true`，并检查是否被其他计分板插件顶掉；执行 `/qd reload` 可重建计分板。

**Q：支持 1.13+ 吗？**
A：本版本面向 1.12.2 编写，材质名为 1.12.2 规范（如 `SKULL_ITEM:5`）。如需其他版本适配，见下方定制服务。

## 更新日志

### v1.1.4

- 插件与仓库更名为 `【ZHBO】plugin_QianDao_1.1.4`（插件内部名 `ZHBO_plugin_QianDao_1.1.4`）
- 插件作者信息统一为 `zhaobuobuo`
- 新增联系方式 `1071576270@QQ.COM`（插件元数据、启动日志、`/qd help`、README）
- 金币配置改为按天直填：`economy.cycle-gold`（第1天默认 10 金币）
- 七日周期奖励、连续 7 / 14 / 30 天额外奖励与全服广播
- 补签卡系统：右键打开补签面板、二次确认、连续天数恢复
- 在线奖励自动发卡（每日上限）
- 签到排行榜 GUI 与右侧实时计分板
- ZPoints / PlayerPoints / Vault 三种经济联动
- 签到标题、音效、粒子、今日槽位发光
- 数据异步保存与 60 天旧记录清理

## 插件定制

> **承接所有版本 Minecraft 服务端插件定制**
> 签到 / 抽奖 / 副本 / 经济 / GUI / 数据统计等均可定制，支持 1.7.10 ~ 最新版本。
>
> **联系方式**
> 作者：`zhaobuobuo`
> 邮箱：`1071576270@QQ.COM`

如果这个插件对你有帮助，欢迎 Star ⭐ 支持一下。
