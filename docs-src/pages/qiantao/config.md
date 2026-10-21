# QianDao · 配置详解

配置文件：`plugins/QianDao/config.yml`，修改后执行 `/qd reload` 生效。
所有文本支持 `&` 颜色代码（如 `&6` 金色、`&l` 加粗）。

---

## general · 基础设置

```yaml
general:
  timezone: "Asia/Shanghai"   # 计算"今天"所用时区
  refresh-hour: 4             # 每日刷新时间（24 小时制）
  refresh-minute: 0
  auto-save-seconds: 300      # 玩家数据自动保存间隔（秒）
```

- `timezone` 决定“今天”的判定，跨时区服务器务必按实际地区填写。
- 刷新时间建议放在凌晨低峰，`4:00` 是国内服常用值。
- 数据保存是异步的，不会卡主线程。

## cycle · 七日周期奖励

```yaml
cycle:
  1:
    icon: "WOOD"
    name: "&f第1天"
    display:
      - "&7铁锭 x5"
    commands:
      - "give %player% iron_ingot 5"
```

| 字段 | 说明 |
| --- | --- |
| `icon` | GUI 图标材质，1.12.2 材质名 |
| `icon-data` | 可选，子 ID / 耐久值，如 `5` = 龙头 |
| `name` | GUI 显示名称 |
| `display` | 奖励说明文字，不填则直接显示 `commands` |
| `commands` | 控制台身份执行的指令，`%player%` 替换为玩家名 |

- 第 1~7 天都要配，第 7 天放终极大奖。
- `commands` 可以接入任意插件，例如：

```yaml
commands:
  - "give %player% diamond 16"
  - "eco give %player% 1000"
  - "points give %player% 100"
  - "crate key %player% vote 1"
```

## milestones · 连续签到额外奖励

```yaml
milestones:
  7:
    display:
      - "&7金锭 x8"
    commands:
      - "give %player% gold_ingot 8"
    broadcast: "&e[签到] &6%player% &e已连续签到 &6&l7天 &e, 获得额外小奖励!"
```

- 键名是“连续天数”，默认示例 7 / 14 / 30，可自行增加。
- 与周期奖励**叠加**发放，不是替换。
- `broadcast` 留空 `""` 则不广播。
- 同一轮连续签到内，每个里程碑只发一次；断签后重新计算。

## buqian · 补签设置

```yaml
buqian:
  enabled: true
  max-days: 3                 # 只能补最近 N 天，建议 ≤ 7
  give-cycle-reward: true     # 补签是否补发当日周期奖励
  item:
    material: "PAPER"
    name: "&a&l补签卡"
    glow: true
    lore:
      - "&7右键使用"
      - "&7可补签最近3天内漏签的日期"
      - "&7补签后连续天数恢复"
```

- 补签界面上限 7 个日期，`max-days` 建议不超过 7。
- 补签只能补「首次签到之后」的日期，新玩家没有可补签记录。

## economy · 金币奖励

```yaml
economy:
  type: "zpoints"                            # none | zpoints | playerpoints | vault
  cycle-gold: [10, 12, 15, 18, 20, 25, 50]   # 第 1~7 天每天的金币
  streak-bonus:
    7: 50
    14: 100
    30: 300
  buqian-gold: false
```

详见 [经济联动](QianDao-经济联动)。

## online-reward · 在线奖励

```yaml
online-reward:
  enabled: true
  interval-minutes: 60        # 累计在线多少分钟得 1 张补签卡
  daily-max: 1                # 每天最多几张
```

统计随每日刷新时间重置，不跨天累计。

## ranking · 签到排行

```yaml
ranking:
  scoreboard-enable: true
  scoreboard-title: "&6&l签到排行榜"
  amount: 10
```

- 排名规则：连续天数降序 → 累计次数降序 → 玩家名。
- 每位玩家使用独立计分板，不会顶掉服务器主计分板。

## effects · 音效与粒子

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
```

音效名参考 [1.12.2 音效列表](https://helpch.at/docs/1.12.2/)。粒子数量建议 10~50，过大影响客户端帧率。

## sign-title · 签到标题

```yaml
sign-title:
  enable: true
  main: "&6&l签到成功"
  sub: "&e周期第{cycle}天奖励已发放"
```

`{cycle}` 会替换为当前周期第几天。

## messages · 消息文本

```yaml
messages:
  prefix: "&8[&6签到&8] &7"
  sign-success: "..."
  sign-already: "..."
  ...
```

可用占位符：

| 占位符 | 含义 |
| --- | --- |
| `{streak}` | 当前连续签到天数 |
| `{cycle}` | 周期第几天（1~7） |
| `{gold}` | 本次获得金币 |
| `{date}` | 补签日期 |
| `{player}` | 目标玩家名 |
| `{card}` | 补签卡数量 |
| `{usage}` | 正确用法 |

`help-user` 是普通玩家可见的帮助，`help-admin` 只有管理员能看到。

---

## 配置建议

- 奖励价值按周期递增，第 7 天明显高于前 6 天，玩家才有连签动力。
- `streak-bonus` 与 `milestones.commands` 可以一个给物品、一个给金币，避免重复。
- 经济数值先小后大：`cycle-gold` 起步 10 左右，第 7 天 50 左右，再配合里程碑 50/100/300。
- 改完先 `/qd reload`，用小号签一次验证奖励是否到账。

---

下一步：[经济联动](QianDao-经济联动) →
