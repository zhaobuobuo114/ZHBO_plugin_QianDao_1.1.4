# QianDao · 指令与权限

## 玩家指令

主命令 `/qiandao`，可用别名 `/qd`、`/sign`。

| 指令 | 作用 |
| --- | --- |
| `/qd` | 打开签到面板 |
| `/qd rank` | 打开签到排行榜 |
| `/qd help` | 查看帮助（管理员会多显示管理指令） |

## 管理指令

| 指令 | 作用 | 权限 |
| --- | --- | --- |
| `/qd givecard <玩家> [数量]` | 发放补签卡，数量 1~2304，默认 1 | `qiandao.admin` |
| `/qd reload` | 重载 `config.yml`，重建排行榜计分板 | `qiandao.admin` |

示例：

```
/qd givecard Steve 3     给 Steve 发 3 张补签卡
/qd reload               改完配置后热重载
```

> `givecard` 只能给在线玩家，且支持 Tab 补全玩家名。

## 权限节点

| 权限 | 说明 | 默认 |
| --- | --- | --- |
| `qiandao.use` | 使用签到与排行榜 | 所有人 |
| `qiandao.admin` | 管理命令（发卡 / 重载），帮助中可见管理项 | OP |

LuckPerms 授权示例：

```
/lp group default permission set qiandao.use true
/lp group admin permission set qiandao.admin true
```

## Tab 补全行为

- 第 1 个参数：补全 `rank`、`help`；有 `qiandao.admin` 时额外补全 `givecard`、`reload`
- 第 2 个参数：仅在 `givecard` 下补全在线玩家名

## 相关页面

- [配置详解](QianDao-配置详解)
- [常见问题](QianDao-常见问题)
