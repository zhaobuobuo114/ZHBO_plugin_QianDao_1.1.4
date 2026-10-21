# 插件名称 · 指令与权限

## 玩家指令

| 指令 | 作用 |
| --- | --- |
| `/cmd` | 说明 |

## 管理指令

| 指令 | 作用 | 权限 |
| --- | --- | --- |
| `/cmd reload` | 重载配置 | `插件名.admin` |

## 权限节点

| 权限 | 说明 | 默认 |
| --- | --- | --- |
| `插件名.use` | 使用基础功能 | 所有人 |
| `插件名.admin` | 管理命令 | OP |

LuckPerms 授权示例：

```
/lp group default permission set 插件名.use true
/lp group admin permission set 插件名.admin true
```
