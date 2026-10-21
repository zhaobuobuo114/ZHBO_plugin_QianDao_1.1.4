# 插件名称 · 安装部署

## 一、环境检查

| 项目 | 要求 |
| --- | --- |
| 服务端核心 | Spigot / Paper 1.12.2 |
| Java | 8 及以上 |
| 可选前置 | 无 |

## 二、安装步骤

1. 从 Releases 下载 jar。
2. 放入 `plugins/` 目录。
3. 启动服务器，确认控制台无报错。
4. 修改 `plugins/插件名/config.yml`。
5. 执行重载命令或重启。

## 三、升级步骤

1. 备份 `config.yml` 与 `data.yml`。
2. 删除旧 `config.yml`，替换 jar。
3. 重启生成新配置。
4. 迁回自定义内容。

## 四、目录结构

```
plugins/
├─ 插件名.jar
└─ 插件名/
   ├─ config.yml
   └─ data.yml
```
