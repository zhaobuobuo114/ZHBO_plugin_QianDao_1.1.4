# QianDao · 安装部署

> 装插件最容易翻车的不是步骤，是版本。先花 30 秒确认下面两行，能省掉后面半小时。

## 一、装之前先确认

| 项目 | 要求 | 怎么查 |
| --- | --- | --- |
| 服务端核心 | Spigot / Paper **1.12.2** | 启动日志第一行，或游戏内 `/version` |
| Java | **8** 及以上 | 控制台执行 `java -version` |
| 可选前置 | Vault、PlayerPoints、ZPoints | 按需，不装也能签到 |

> 这是 1.12.2 专用构建，材质名和音效名都按 1.12.2 的规范写死。丢进 1.13+ 会报材质找不到，丢进 1.8 会报 API 不存在 —— 不是配置问题，是版本问题。

## 二、安装

1. 打开 [Releases](https://github.com/zhaobuobuo114/klkhk-plugin_qiandao/releases/latest)，下载 `klkhk.plugin_qiandao_1.12.2.jar`。
2. 把 jar 放进服务端根目录的 `plugins/`。在线人数多的话建议停服操作，别热插拔。
3. 启动服务器。看到这两行，说明插件活过来了：

```log
[QianDao] QianDao 签到插件已启用 v1.1.4
[QianDao] 金币联动: ZPOINTS
```

如果第二行是 `金币联动: 已关闭`，不是插件坏了 —— 是你没装对应经济插件，签到功能完全正常，只是不发金币。

4. 插件会自己生成这两个文件：

```text
plugins/QianDao/config.yml    奖励、金币、消息、音效
plugins/QianDao/data.yml      玩家数据（第一个人签到后才有）
```

5. 改完 `config.yml` 执行 `/qd reload`，不用重启。

## 三、升级（这一步别偷懒）

正确顺序是：**备份 → 删旧配置 → 换 jar → 重启 → 迁回自定义值 → 重载**。

1. 备份 `plugins/QianDao/config.yml` 和 `data.yml`
2. 删除旧 `config.yml`
3. 替换 jar，重启
4. 把备份里的自定义内容（奖励、金币、消息）迁到新配置
5. `/qd reload` 校验

> 为什么不直接覆盖 jar？新版本可能新增配置项，旧的 `config.yml` 不会自动补全。结果通常是「功能没生效，控制台也不报错」—— 这种问题最难查，不如一次做对。

`data.yml` 不用删，里面的玩家数据是向下兼容的。

## 四、卸载

1. 停服
2. 需要留数据就先备份 `plugins/QianDao/data.yml`
3. 删掉整个 `plugins/QianDao/` 目录
4. 删掉 `plugins/` 下的插件 jar
5. 启动服务器

## 五、目录结构

```text
服务器根目录/
├─ plugins/
│  ├─ QianDao.jar
│  └─ QianDao/
│     ├─ config.yml      # 奖励、金币、消息、音效
│     └─ data.yml        # 玩家签到数据（UUID 索引）
```

## 六、自己编译

需要 JDK 8 和 Maven：

```bash
git clone https://github.com/zhaobuobuo114/klkhk-plugin_qiandao.git
cd klkhk-plugin_qiandao
mvn clean package
```

产物在 `target/QianDao.jar`。

`plugins/` 下的 `Vault-1.7.3.jar`、`PlayerPoints-2.1.5.jar`、`zpoints-1.3.0.jar` 是编译期依赖，不会打进最终 jar，运行时由服务端提供。想换 API 版本就改 `pom.xml` 里的 `spigot-api`，但材质和音效名要跟着改。

---

下一步：[指令与权限](QianDao-指令与权限) →
