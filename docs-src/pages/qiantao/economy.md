# QianDao · 经济联动

签到除了发物品，还能直接发**点券或金币**。插件本身不实现经济系统，而是调用服务器上已有的经济插件。

## 支持的类型

`config.yml` 中 `economy.type` 可选四种：

| type | 前置插件 | 发放内容 |
| --- | --- | --- |
| `none` | — | 不发放（关闭金币） |
| `zpoints` | ZPoints | 点券，可在 ZPoints 商店 / 抽卡中使用 |
| `playerpoints` | PlayerPoints | 点券 |
| `vault` | Vault + 经济插件（EssentialsX 等） | 经济账户金钱 |

## 配置示例

### ZPoints（推荐）

```yaml
economy:
  type: "zpoints"
  cycle-gold: [10, 12, 15, 18, 20, 25, 50]
  streak-bonus:
    7: 50
    14: 100
    30: 300
  buqian-gold: false
```

### PlayerPoints

```yaml
economy:
  type: "playerpoints"
```

### Vault（EssentialsX 金钱）

```yaml
economy:
  type: "vault"
```

需要先安装 Vault 和一个经济实现（如 EssentialsX），并在 `plugins/Vault/` 或其他经济插件中确认已正常挂钩。

## 启动检测

插件启动时会检测对应前置是否 **已加载并启用**：

```
[QianDao] 金币联动: ZPOINTS
```

如果配置了但没装对应插件，控制台会提示并自动降级为不发金币：

```
[QianDao] economy.type 配置为 ZPOINTS 但未检测到插件 ZPoints, 金币奖励已自动跳过
```

签到、补签、排行榜等功能不受影响，只是金币部分跳过。

## 金币发放时机

| 场景 | 发放数量 |
| --- | --- |
| 每日签到 | `cycle-gold` 中对应周期天的数量 |
| 连续签到里程碑 | `streak-bonus` 中对应天数的额外金币 |
| 补签 | 默认不发；`buqian-gold: true` 时按第 1 天数量发放 |

## 与物品奖励的关系

金币和物品**同时发放**，互不冲突。周期奖励走 `cycle.commands`，金币走经济 API。

如果你希望完全用指令发钱，把 `economy.type` 设为 `none`，然后在 `cycle.commands` 里写经济插件指令即可：

```yaml
commands:
  - "eco give %player% 1000"
```

## 疑难排查

**金币没到账？**

1. 看控制台启动日志里 `金币联动:` 后面的值。
2. 确认前置插件在 `plugins/` 且未被禁用。
3. `/qd reload` 后重新测试。
4. Vault 模式下确认经济插件已注册 Economy 服务。

**数值太大/太小？**

直接改 `cycle-gold` 与 `streak-bonus`，不需要重启，`/qd reload` 即可。

---

返回：[配置详解](QianDao-配置详解) · [常见问题](QianDao-常见问题)
