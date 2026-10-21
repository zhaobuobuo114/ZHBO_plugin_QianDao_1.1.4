# QianDao · 安装部署

## 一、环境检查

| 项目 | 要求 |
| --- | --- |
| 服务端核心 | Spigot / Paper **1.12.2** |
| Java | **8** 及以上（`java -version` 查看） |
| 可选前置 | Vault、PlayerPoints、ZPoints |

> 这是 1.12.2 专用构建，材质名与音效名均按 1.12.2 规范编写，不要直接丢到 1.13+ 或 1.8 服务端使用。

## 二、安装步骤

1. 打开 [Releases](https://github.com/zhaobuobuo114/klkhk-plugin_qiandao/releases/latest)，下载 `klkhk.plugin_qiandao_1.12.2.jar`。
2. 关闭服务器（或准备重载），把 jar 放进服务端根目录的 `plugins/` 文件夹。
3. 启动服务器，控制台出现以下日志即加载成功：

```
[QianDao] QianDao 签到插件已启用 v1.1.4
[QianDao] 金币联动: ZPOINTS
```

4. 首次启动会自动生成：

```
plugins/QianDao/config.yml    配置
plugins/QianDao/data.yml      玩家数据（有人签到后才有）
```

5. 修改 `config.yml` 后执行 `/qd reload`，无需重启。

## 三、升级步骤

1. **备份** `plugins/QianDao/config.yml` 与 `data.yml`。
2. 删除旧的 `plugins/QianDao/config.yml`，替换 jar。
3. 重启服务器，让新版本生成完整配置。
4. 把备份里的自定义内容（奖励、金币、消息）迁移到新配置。
5. 执行 `/qd reload` 校验。

> 为什么不直接覆盖？新版本可能新增配置项，旧文件不会自动补全，容易出现“功能不生效但没报错”的情况。

## 四、卸载

1. 停服。
2. 删除 `plugins/QianDao/` 整个目录（如需保留数据，先备份 `data.yml`）。
3. 删除 `plugins/` 下的插件 jar。
4. 启动服务器。

## 五、目录结构

```
服务器根目录/
├─ plugins/
│  ├─ QianDao.jar
│  └─ QianDao/
│     ├─ config.yml      # 奖励、金币、消息、音效
│     └─ data.yml        # 玩家签到数据（UUID 索引）
```

## 六、从源码构建

需要 JDK 8 与 Maven：

```bash
git clone https://github.com/zhaobuobuo114/klkhk-plugin_qiandao.git
cd klkhk-plugin_qiandao
mvn clean package
```

产物为 `target/QianDao.jar`。

`plugins/` 目录下的 `Vault-1.7.3.jar`、`PlayerPoints-2.1.5.jar`、`zpoints-1.3.0.jar` 是编译期依赖（system scope），不会打进最终 jar，运行时由服务端提供。

---

下一步：[指令与权限](QianDao-指令与权限) →
