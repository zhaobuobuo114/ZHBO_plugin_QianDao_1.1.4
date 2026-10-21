#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Build the static documentation site (docs/) from docs-src/.

Usage:  python tools/build_wiki.py
"""

from __future__ import annotations

import json
import posixpath
import re
import shutil
import sys
from html import escape
from pathlib import Path

import markdown
from markdown.extensions.toc import TocExtension

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "docs-src"
PAGES = SRC / "pages"
OUT = ROOT / "docs"

STATUS_TEXT = {"stable": "稳定", "beta": "测试", "wip": "开发中"}


def load_site() -> dict:
    with (SRC / "site.json").open(encoding="utf-8") as fh:
        return json.load(fh)


def flatten(site: dict):
    """Return ordered list of (group, item) plus alias map."""
    flat = []
    alias = {}
    for group in site["groups"]:
        for item in group["items"]:
            flat.append((group, item))
            alias[item["title"]] = item["slug"]
            alias[Path(item["source"]).stem] = item["slug"]
            for name in item.get("alias", []):
                alias[name] = item["slug"]
    return flat, alias


LINK_RE = re.compile(r"(?<!!)\[([^\]]+)\]\(([^)\s]+)\)")


def rewrite_links(text: str, slug: str, alias: dict) -> str:
    def repl(match: re.Match) -> str:
        label, target = match.group(1), match.group(2)
        if target.startswith(("http://", "https://", "#", "mailto:")):
            return match.group(0)
        clean = target[:-3] if target.endswith(".md") else target
        dest = alias.get(clean) or alias.get(target)
        if not dest:
            return match.group(0)
        rel = posixpath.relpath(dest, posixpath.dirname(slug) or ".")
        return f"[{label}]({rel})"

    return LINK_RE.sub(repl, text)


def plain_text(md_text: str) -> str:
    text = re.sub(r"```.*?```", " ", md_text, flags=re.S)
    text = re.sub(r"\[\[[^\]]+\]\]", " ", text)
    text = re.sub(r"[#>*`|\-]{1,}", " ", text)
    text = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def build_toc(tokens, level: int = 0) -> str:
    if not tokens:
        return ""
    cls = "toc-list" if level == 0 else "toc-sub"
    parts = [f'<ul class="{cls}">']
    for tok in tokens:
        parts.append(f'<li><a href="#{escape(tok["id"])}">{escape(tok["name"])}</a>')
        if tok.get("children"):
            parts.append(build_toc(tok["children"], level + 1))
        parts.append("</li>")
    parts.append("</ul>")
    return "".join(parts)


def render_nav(site: dict, active_slug: str, root: str) -> str:
    out = []
    for group in site["groups"]:
        items = group["items"]
        is_open = group.get("open", False) or any(it["slug"] == active_slug for it in items)
        badge = group.get("badge")
        badge_html = f'<span class="nav-badge">{escape(badge)}</span>' if badge else ""

        if group.get("collapsible"):
            out.append(
                f'<details class="nav-group nav-collapse"{" open" if is_open else ""}>'
            )
            out.append(
                f'<summary class="nav-title">{escape(group["title"])}{badge_html}'
                '<svg class="nav-chevron" viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">'
                '<path d="M5 6l3 3 3-3" fill="none" stroke="currentColor" stroke-width="1.6" '
                'stroke-linecap="round" stroke-linejoin="round"/></svg></summary>'
            )
        else:
            out.append('<div class="nav-group">')
            out.append(f'<div class="nav-title">{escape(group["title"])}{badge_html}</div>')

        out.append('<ul class="nav-items">')
        for item in items:
            cls = "nav-link is-active" if item["slug"] == active_slug else "nav-link"
            out.append(
                f'<li><a class="{cls}" href="{root}{item["slug"]}">{escape(item["title"])}</a></li>'
            )
        out.append("</ul>")

        out.append("</details>" if group.get("collapsible") else "</div>")
    return "\n".join(out)


def render_stats(site: dict) -> str:
    plugins = site.get("plugins") or []
    page_count = sum(len(g["items"]) for g in site["groups"])
    latest = max((p.get("updated", "") for p in plugins), default="")
    stats = [
        (str(len(plugins)), "个插件"),
        (str(page_count), "篇文档"),
        (latest or "—", "最近更新"),
    ]
    cells = "".join(
        f'<div class="stat"><b>{escape(v)}</b><span>{escape(k)}</span></div>'
        for v, k in stats
    )
    return f'<div class="stat-row">{cells}</div>'


def render_cards(site: dict, root: str, limit: int | None = None) -> str:
    plugins = list(site.get("plugins") or [])
    if limit:
        plugins = plugins[:limit]
    cards = []
    for p in plugins:
        status = p.get("status", "stable")
        tags = "".join(
            f'<span class="pc-tag">{escape(t)}</span>' for t in p.get("tags", [])
        )
        haystack = " ".join(
            [p.get("name", ""), p.get("fullName", ""), p.get("summary", "")] + p.get("tags", [])
        )
        cards.append(
            f'<article class="plugin-card" data-text="{escape(haystack.lower())}">'
            '<div class="pc-top">'
            f'<div class="pc-name">{escape(p.get("name", ""))}'
            f'<span class="pc-ver">{escape(p.get("version", ""))}</span></div>'
            f'<span class="pc-status is-{escape(status)}">{escape(STATUS_TEXT.get(status, status))}</span>'
            "</div>"
            f'<p class="pc-desc">{escape(p.get("summary", ""))}</p>'
            f'<div class="pc-tags"><span class="pc-tag pc-tag-mc">{escape(p.get("mc", ""))}</span>{tags}</div>'
            '<div class="pc-actions">'
            f'<a class="pc-btn" href="{root}{escape(p.get("docs", ""))}">查看文档</a>'
            f'<a class="pc-btn pc-btn-ghost" href="{escape(p.get("download", ""))}" '
            'target="_blank" rel="noopener">下载</a>'
            "</div></article>"
        )
    if not cards:
        return '<div class="plugin-empty">还没有上架的插件。</div>'
    return f'<div class="plugin-grid">{"".join(cards)}</div>'


def render_filter() -> str:
    return (
        '<div class="plugin-filter">'
        '<input id="pluginFilter" type="search" '
        'placeholder="搜插件名或标签，比如「签到」「经济」" autocomplete="off">'
        '<span class="filter-count" id="pluginCount"></span>'
        "</div>"
    )


def render_meta(site: dict, plugin_id: str) -> str:
    plugin = next((p for p in site.get("plugins", []) if p["id"] == plugin_id), None)
    if not plugin:
        return ""
    status = plugin.get("status", "stable")
    return (
        '<div class="meta-bar">'
        f'<span class="meta-chip"><b>版本</b>{escape(plugin.get("version", ""))}</span>'
        f'<span class="meta-chip"><b>适用</b>Minecraft {escape(plugin.get("mc", ""))}</span>'
        f'<span class="meta-chip"><b>状态</b>{escape(STATUS_TEXT.get(status, status))}</span>'
        f'<span class="meta-chip"><b>更新</b>{escape(plugin.get("updated", ""))}</span>'
        f'<a class="meta-chip meta-chip-link" href="{escape(plugin.get("download", ""))}" '
        'target="_blank" rel="noopener">下载 jar</a>'
        "</div>"
    )


def apply_placeholders(text: str, site: dict, root: str) -> str:
    if "[[STATS]]" in text:
        text = text.replace("[[STATS]]", "\n\n" + render_stats(site) + "\n\n")
    if "[[PLUGINS_TOP]]" in text:
        text = text.replace("[[PLUGINS_TOP]]", "\n\n" + render_cards(site, root, limit=3) + "\n\n")
    if "[[PLUGINS]]" in text:
        block = render_filter() + render_cards(site, root)
        text = text.replace("[[PLUGINS]]", "\n\n" + block + "\n\n")
    for match in set(re.findall(r"\[\[META:([a-z0-9_-]+)\]\]", text)):
        text = text.replace(f"[[META:{match}]]", "\n\n" + render_meta(site, match) + "\n\n")
    return text


PAGE_TMPL = """<!DOCTYPE html>
<html lang="zh-CN" data-root="{root}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{title} · {site_title}</title>
<meta name="description" content="{desc}">
<link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'%3E%3Crect width='32' height='32' rx='7' fill='%230d1117'/%3E%3Cpath d='M9 8h9a5 5 0 0 1 0 10H9z' fill='none' stroke='%233fb950' stroke-width='2.6' stroke-linejoin='round'/%3E%3Cpath d='M9 18h11a5 5 0 0 1 0 10H9z' fill='none' stroke='%23e3b341' stroke-width='2.6' stroke-linejoin='round'/%3E%3C/svg%3E">
<link rel="stylesheet" href="{root}assets/style.css">
</head>
<body>
<header class="topbar">
  <button class="menu-btn" id="menuBtn" aria-label="菜单">
    <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true"><path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
  </button>
  <a class="brand" href="{root}index.html">
    <span class="brand-mark">QD</span>
    <span class="brand-text">{site_title}</span>
  </a>
  <div class="topbar-right">
    <button class="search-btn" id="searchBtn" type="button">
      <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true"><circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2"/><path d="M16.5 16.5 21 21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      <span>搜索文档</span>
      <kbd>/</kbd>
    </button>
    <a class="gh-link" href="{repo}" target="_blank" rel="noopener" aria-label="GitHub 仓库">
      <svg viewBox="0 0 16 16" width="18" height="18" aria-hidden="true"><path fill="currentColor" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/></svg>
    </a>
  </div>
</header>
<div class="scrim" id="scrim"></div>
<div class="layout">
  <aside class="sidebar" id="sidebar">
    <nav class="nav">{nav}</nav>
    <div class="sidebar-foot">
      <a class="btn-download" href="{releases}" target="_blank" rel="noopener">下载最新版本</a>
      <div class="sidebar-contact">定制 QQ {qq}</div>
    </div>
  </aside>
  <main class="main">
    <div class="crumb">{crumb}</div>
    <article class="markdown">{content}</article>
    <nav class="pager">{pager}</nav>
    <footer class="pagefoot">
      <div>承接所有版本 Minecraft 服务端插件定制 · 需求直说，工期和报价也直说</div>
      <div>QQ <a href="https://wpa.qq.com/msgrd?v=3&uin={qq}&site=qq&menu=yes" target="_blank" rel="noopener">{qq}</a> · 邮箱 <a href="mailto:{email}">{email}</a></div>
    </footer>
  </main>
  <aside class="toc" id="toc">
    <div class="toc-title">本页目录</div>
    {toc}
  </aside>
</div>
<div class="search-modal" id="searchModal" hidden>
  <div class="search-panel">
    <div class="search-head">
      <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true"><circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2"/><path d="M16.5 16.5 21 21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
      <input id="searchInput" type="search" placeholder="搜索文档标题、配置项、指令…" autocomplete="off">
      <button id="searchClose" type="button" aria-label="关闭">ESC</button>
    </div>
    <div class="search-results" id="searchResults"></div>
  </div>
</div>
<script src="{root}assets/app.js"></script>
</body>
</html>
"""


def main() -> int:
    site = load_site()
    flat, alias = flatten(site)

    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    shutil.copytree(SRC / "assets", OUT / "assets")

    index = []
    total = len(flat)

    for pos, (group, item) in enumerate(flat):
        src_file = PAGES / item["source"]
        if not src_file.exists():
            print(f"[missing] {src_file}")
            return 1
        raw = src_file.read_text(encoding="utf-8")
        raw = rewrite_links(raw, item["slug"], alias)

        slug = item["slug"]
        depth = len(posixpath.dirname(slug).split("/")) if posixpath.dirname(slug) else 0
        root = "../" * depth

        raw = apply_placeholders(raw, site, root)

        md = markdown.Markdown(
            extensions=[
                "fenced_code",
                "tables",
                "sane_lists",
                "attr_list",
                TocExtension(toc_depth="2-3", anchorlink=False),
            ]
        )
        body = md.convert(raw)
        body = re.sub(
            r'<a href="(https?://[^"]+)"',
            r'<a target="_blank" rel="noopener" href="\1"',
            body,
        )
        toc_html = build_toc(md.toc_tokens)

        crumb_parts = [f'<a href="{root}index.html">文档中心</a>']
        if group["title"] != "开始":
            crumb_parts.append(
                f'<span class="sep">/</span><span>{escape(group["title"])}</span>'
            )
        if not (item["slug"] == "index.html" and group["title"] == "开始"):
            crumb_parts.append(
                f'<span class="sep">/</span><span class="cur">{escape(item["title"])}</span>'
            )
        crumb = "".join(crumb_parts)

        pager = []
        if pos > 0:
            prev_item = flat[pos - 1][1]
            pager.append(
                f'<a class="pager-prev" href="{root}{prev_item["slug"]}">'
                f'<span>上一页</span><strong>{escape(prev_item["title"])}</strong></a>'
            )
        if pos + 1 < total:
            next_item = flat[pos + 1][1]
            pager.append(
                f'<a class="pager-next" href="{root}{next_item["slug"]}">'
                f'<span>下一页</span><strong>{escape(next_item["title"])}</strong></a>'
            )

        page_title = item["title"]
        if group.get("prefix"):
            page_title = f'{group["prefix"]} · {item["title"]}'

        html_out = PAGE_TMPL.format(
            root=root,
            title=escape(page_title),
            site_title=escape(site["siteTitle"]),
            desc=escape(item.get("desc", site["description"])),
            repo=site["repo"],
            releases=site["releases"],
            qq=site["qq"],
            email=site["email"],
            nav=render_nav(site, slug, root),
            crumb=crumb,
            content=body,
            pager="".join(pager),
            toc=toc_html or '<div class="toc-empty">本页暂无小标题</div>',
        )

        target = OUT / slug
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(html_out, encoding="utf-8")

        index.append(
            {
                "t": item["title"],
                "u": slug,
                "g": group["title"],
                "x": plain_text(raw)[:1200],
            }
        )
        print(f"[ok] {slug}")

    (OUT / "assets" / "search-index.json").write_text(
        json.dumps(index, ensure_ascii=False), encoding="utf-8"
    )
    (OUT / ".nojekyll").write_text("", encoding="utf-8")
    print(f"[done] {total} pages")
    return 0


if __name__ == "__main__":
    sys.exit(main())
