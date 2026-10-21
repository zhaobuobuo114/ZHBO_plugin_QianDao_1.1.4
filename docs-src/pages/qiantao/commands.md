# QianDao · 指令与权限

> 玩家只需要记住一个 `/qd`。管理相关的只有发卡和重载两个，日常用不上。

## 玩家指令

主命令 `/qiandao`，别名 `/qd`、`/sign` —— 三个都能用，习惯哪个用哪个。

| 指令 | 作用 |
| --- | --- |
| `/qd` | 打开签到面板 |
| `/qd rank` | 打开签到排行榜 |
| `/qd help` | 查看帮助（管理员会多出两行管理指令） |

## 管理指令

| 指令 | 作用 | 权限 |
| --- | --- | --- |
| `/qd givecard <玩家> [数量]` | 发放补签卡，数量 1~2304，默认 1 | `qiandao.admin` |
| `/qd reload` | 重载 `config.yml`，重建排行榜计分板 | `qiandao.admin` |

```bash
/qd givecard Steve 3      # 给 Steve 发 3 张补签卡
/qd reload                # 改完配置热重载
```

> `givecard` 只能发给在线玩家，支持 Tab 补全名字。离线玩家请等他上线，或者用活动发放。

## 权限节点

| 权限 | 说明 | 默认 |
| --- | --- | --- |
| `qiandao.use` | 使用签到与排行榜 | 所有人 |
| `qiandao.admin` | 管理命令（发卡 / 重载），帮助中可见管理项 | OP |

LuckPerms 授权：

```bash
/lp group default permission set qiandao.use true
/lp group admin permission set qiandao.admin true
```

## Tab 补全行为

- 第 1 个参数：补 `rank`、`help`；有 `qiandao.admin` 时额外补 `givecard`、`reload`
- 第 2 个参数：只在 `givecard` 下补在线玩家名

补全是按权限走的，普通玩家看不到管理指令，少一个误触的机会。

## 相关页面

- [配置详解](QianDao-配置详解) — 重载前先看配置怎么改
- [常见问题](QianDao-常见问题) — 指令没反应先看这里
