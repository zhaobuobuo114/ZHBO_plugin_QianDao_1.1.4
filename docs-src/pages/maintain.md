# 文档维护指南

这份文档写给以后要往站点里加插件的自己。整套站点是静态生成的：没有数据库，没有后台，改完跑一条命令就发布。

---

## 一、站点是怎么工作的

```text
docs-src/                 文档源文件（编辑这里）
├─ site.json              站点配置：标题、插件清单、导航分组
├─ pages/
│  ├─ home.md             首页
│  ├─ plugins.md          插件库
│  ├─ custom.md           插件定制
│  ├─ maintain.md         本文
│  ├─ _template/          新插件模板
│  └─ qiantao/            每个插件一个目录
│     ├─ intro.md
│     ├─ install.md
│     └─ ...

tools/build_wiki.py       构建：Markdown → docs/ 静态站
docs/                     GitHub Pages 发布目录（自动生成，不要手改）
```

构建命令：

```bash
python tools/build_wiki.py
```

## 二、新增一个插件（四步）

### 第 1 步：在 `site.json` 的 `plugins` 里登记

```json
{
  "id": "dailyquest",
  "name": "DailyQuest",
  "fullName": "DailyQuest 每日任务",
  "version": "v1.0.0",
  "mc": "1.12.2",
  "status": "stable",
  "updated": "2026-10-21",
  "summary": "一句话说清它解决什么问题，别写形容词。",
  "tags": ["任务", "活跃度", "奖励"],
  "docs": "dailyquest/index.html",
  "download": "https://github.com/.../releases/latest"
}
```

`status` 只能是 `stable` / `beta` / `wip`，分别显示为「稳定 / 测试 / 开发中」。

登记后，首页和 [插件库](插件总览) 的卡片会自动出现，不用手写 HTML。

### 第 2 步：复制模板

```bash
cp -r docs-src/pages/_template docs-src/pages/dailyquest
```

模板里有四个文件：介绍、安装、指令、FAQ。改内容即可。

### 第 3 步：在 `groups` 里加一个分组

```json
{
  "title": "DailyQuest 每日任务",
  "badge": "v1.0.0",
  "prefix": "DailyQuest",
  "collapsible": true,
  "items": [
    { "title": "插件介绍", "slug": "dailyquest/index.html", "source": "dailyquest/intro.md", "alias": ["DailyQuest-介绍"] },
    { "title": "安装部署", "slug": "dailyquest/install.html", "source": "dailyquest/install.md", "alias": ["DailyQuest-安装"] },
    { "title": "指令与权限", "slug": "dailyquest/commands.html", "source": "dailyquest/commands.md", "alias": ["DailyQuest-指令"] },
    { "title": "常见问题", "slug": "dailyquest/faq.html", "source": "dailyquest/faq.md", "alias": ["DailyQuest-FAQ"] }
  ]
}
```

| 字段 | 作用 |
| --- | --- |
| `title` | 侧边栏分组名 |
| `badge` | 分组右侧的版本小标签 |
| `prefix` | 页面浏览器标题的前缀，如「DailyQuest · 安装部署」 |
| `collapsible` | 是否折叠，插件多了建议开 |
| `open` | 默认展开（可选） |
| `slug` | 生成的 HTML 路径，首个子页面习惯用 `index.html` |
| `source` | 对应的 Markdown 文件（相对 `pages/`） |
| `alias` | 站内链接别名，写 `[安装](DailyQuest-安装)` 就能跳 |

### 第 4 步：构建并发布

```bash
python tools/build_wiki.py
git add -A
git commit -m "docs: 新增 DailyQuest 文档"
git push
```

推送后 GitHub Pages 自动重新发布，大约一分钟。

## 三、页面里能用的占位符

在 Markdown 里单独一行写占位符，构建时会替换成生成的 HTML（前后要留空行）：

| 占位符 | 生成内容 |
| --- | --- |
| `[[STATS]]` | 插件数 / 文档数 / 最近更新 |
| `[[PLUGINS]]` | 带搜索框的插件卡片网格（插件库用） |
| `[[PLUGINS_TOP]]` | 前 3 个插件卡片（首页用） |
| `[[META:qiantao]]` | 该插件的版本、适用版本、状态、更新时间、下载按钮 |

目前只有首页、插件库和插件介绍页用到，其他页面不需要。

## 四、写作规范

- **先给结论，再给原因**。比如「升级前先删 config.yml」放在前面，为什么放后面。
- **配置项写清默认值**，读者不用回去翻文件。
- **命令用表格**：指令 / 作用 / 权限，三列够了。
- **坑要标出来**。容易翻车的地方单独写一句，别埋在段落里。
- **站内链接用别名**，如 `[配置详解](QianDao-配置详解)`，构建时自动转成正确路径。
- **外部链接自动新开标签页**，不用手写 `target`。

## 五、本地预览

直接双击 `docs/index.html` 能看排版；搜索和插件筛选依赖 `fetch`，要起个本地服务：

```bash
python -m http.server 8000 -d docs
```

然后访问 `http://localhost:8000`。

## 六、常见问题

**改了 Markdown，网站没变？**
先跑 `python tools/build_wiki.py`，再提交 `docs/`。只改 `docs-src/` 不构建，线上不会变。

**页面 404？**
检查 `site.json` 里的 `slug` 与生成路径是否一致，`source` 文件是否存在。

**导航顺序不对？**
`groups` 和 `items` 的数组顺序就是显示顺序。

**想换配色？**
改 `docs-src/assets/style.css` 顶部的 CSS 变量（`--accent`、`--bg`、`--panel`）。

**卡片上的版本号和实际不符？**
改 `site.json` 里的 `plugins[].version`，这是唯一数据源。

---

[返回首页](Home) · [插件库](插件总览)
