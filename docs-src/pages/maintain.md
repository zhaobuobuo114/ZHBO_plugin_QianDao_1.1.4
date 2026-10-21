# 文档维护指南

这份文档说明**如何往站点里新增一个插件**。整套站点是静态生成的，不需要数据库、不需要后台。

---

## 一、站点是怎么工作的

```
docs-src/                 文档源文件（你编辑这里）
├─ site.json              站点配置：标题、导航分组、页面清单
└─ pages/
   ├─ home.md             首页
   ├─ plugins.md          插件总览
   ├─ custom.md           插件定制
   ├─ maintain.md         本文
   └─ qiantao/            每个插件一个目录
      ├─ intro.md
      ├─ install.md
      └─ ...

tools/build_wiki.py       构建脚本：Markdown → docs/ 静态站
docs/                     GitHub Pages 发布目录（自动生成，不要手改）
```

执行构建：

```bash
python tools/build_wiki.py
```

它会读取 `site.json` 与 `pages/`，生成完整的 `docs/`：每页一个 HTML、统一侧边栏、目录、页码导航和搜索索引。

## 二、新增一个插件（4 步）

### 第 1 步：建目录与页面

假设新插件叫 `DailyQuest`：

```
docs-src/pages/dailyquest/
├─ intro.md       插件介绍
├─ install.md     安装部署
├─ commands.md    指令与权限
└─ faq.md         常见问题
```

每个文件就是普通 Markdown，第一行用 `# 标题` 开始即可。

### 第 2 步：在 site.json 里加一个分组

```json
{
  "title": "DailyQuest 每日任务",
  "badge": "v1.0.0",
  "items": [
    { "title": "插件介绍", "slug": "dailyquest/index.html", "source": "dailyquest/intro.md", "alias": ["DailyQuest-介绍"] },
    { "title": "安装部署", "slug": "dailyquest/install.html", "source": "dailyquest/install.md", "alias": ["DailyQuest-安装"] },
    { "title": "指令与权限", "slug": "dailyquest/commands.html", "source": "dailyquest/commands.md", "alias": ["DailyQuest-指令"] },
    { "title": "常见问题", "slug": "dailyquest/faq.html", "source": "dailyquest/faq.md", "alias": ["DailyQuest-FAQ"] }
  ]
}
```

字段说明：

| 字段 | 作用 |
| --- | --- |
| `title` | 侧边栏与页面标题 |
| `slug` | 生成的 HTML 路径，首个子页面习惯用 `index.html` |
| `source` | 对应的 Markdown 文件（相对 `pages/`） |
| `alias` | 可选，站内链接别名，写 `[安装](DailyQuest-安装)` 就能跳转 |
| `desc` | 可选，页面描述（用于 meta 与搜索结果） |

### 第 3 步：更新插件总览

编辑 `docs-src/pages/plugins.md`，在表格里加一行：

```markdown
| [DailyQuest 每日任务](DailyQuest-介绍) | 1.12.2 | v1.0.0 | 每日任务 / 活跃度 / 奖励 |
```

### 第 4 步：构建并发布

```bash
python tools/build_wiki.py
git add -A
git commit -m "docs: 新增 DailyQuest 文档"
git push
```

推送后 GitHub Pages 会自动重新发布（约 1 分钟）。

## 三、写作规范

- 一个页面只讲一件事，标题层级从 `#` 开始，正文用 `##` / `###`
- 配置项用代码块，并标注默认值
- 命令用表格列出：指令 / 作用 / 权限
- 涉及数值的地方写清单位（秒、分钟、游戏内金币）
- 站内链接统一用别名，例如 `[配置详解](QianDao-配置详解)`，构建时会自动转成正确路径
- 外部链接会自动在新标签页打开

## 四、本地预览

直接双击 `docs/index.html` 可以看排版；搜索功能依赖 `fetch`，需要通过 HTTP 打开：

```bash
python -m http.server 8000 -d docs
```

然后访问 `http://localhost:8000`。

## 五、常见问题

**改了 Markdown 但网站没变？**
先执行 `python tools/build_wiki.py`，再提交 `docs/` 目录。

**页面 404？**
检查 `site.json` 里的 `slug` 是否与生成路径一致，以及 `source` 文件是否存在。

**导航顺序不对？**
`site.json` 中 `groups` 与 `items` 的数组顺序就是显示顺序。

**能改配色吗？**
改 `docs/assets/style.css` 顶部的 CSS 变量即可（`--accent`、`--bg`、`--panel` 等）。

---

[返回首页](Home) · [插件总览](插件总览)
