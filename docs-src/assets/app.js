(function () {
  "use strict";

  var root = document.documentElement.getAttribute("data-root") || "";
  var body = document.body;

  /* mobile nav */
  var menuBtn = document.getElementById("menuBtn");
  var scrim = document.getElementById("scrim");

  function closeNav() {
    body.classList.remove("nav-open");
  }

  if (menuBtn) {
    menuBtn.addEventListener("click", function () {
      body.classList.toggle("nav-open");
    });
  }
  if (scrim) scrim.addEventListener("click", closeNav);

  /* copy code */
  document.querySelectorAll(".markdown pre").forEach(function (pre) {
    var btn = document.createElement("button");
    btn.className = "copy-btn";
    btn.type = "button";
    btn.textContent = "复制";
    btn.addEventListener("click", function () {
      var code = pre.querySelector("code");
      var text = code ? code.innerText : pre.innerText;
      var done = function () {
        btn.textContent = "已复制";
        btn.classList.add("is-done");
        setTimeout(function () {
          btn.textContent = "复制";
          btn.classList.remove("is-done");
        }, 1600);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, function () {});
      } else {
        var ta = document.createElement("textarea");
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        try {
          document.execCommand("copy");
          done();
        } catch (e) {}
        document.body.removeChild(ta);
      }
    });
    pre.appendChild(btn);
  });

  /* toc highlight */
  var tocLinks = Array.prototype.slice.call(
    document.querySelectorAll(".toc-list a")
  );
  if (tocLinks.length && "IntersectionObserver" in window) {
    var map = {};
    tocLinks.forEach(function (a) {
      var id = decodeURIComponent(a.getAttribute("href").slice(1));
      map[id] = a;
    });
    var headings = Array.prototype.filter.call(
      document.querySelectorAll(".markdown h2[id], .markdown h3[id]"),
      function (h) {
        return map[h.id];
      }
    );
    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          tocLinks.forEach(function (a) {
            a.classList.remove("is-active");
          });
          var active = map[entry.target.id];
          if (active) active.classList.add("is-active");
        });
      },
      { rootMargin: "-80px 0px -70% 0px", threshold: 0 }
    );
    headings.forEach(function (h) {
      observer.observe(h);
    });
  }

  /* plugin filter */
  var filterInput = document.getElementById("pluginFilter");
  if (filterInput) {
    var cards = Array.prototype.slice.call(
      document.querySelectorAll(".plugin-card")
    );
    var countEl = document.getElementById("pluginCount");
    var applyFilter = function () {
      var q = filterInput.value.trim().toLowerCase();
      var shown = 0;
      cards.forEach(function (card) {
        var hay = card.getAttribute("data-text") || "";
        var hit = !q || hay.indexOf(q) > -1;
        card.style.display = hit ? "" : "none";
        if (hit) shown++;
      });
      if (countEl) countEl.textContent = shown + " / " + cards.length;
    };
    filterInput.addEventListener("input", applyFilter);
    applyFilter();
  }

  /* search */
  var modal = document.getElementById("searchModal");
  var input = document.getElementById("searchInput");
  var resultsBox = document.getElementById("searchResults");
  var searchBtn = document.getElementById("searchBtn");
  var searchClose = document.getElementById("searchClose");
  var indexData = null;
  var loading = false;
  var cursor = -1;
  var current = [];

  function openSearch() {
    if (!modal) return;
    modal.hidden = false;
    body.style.overflow = "hidden";
    input.value = "";
    render([]);
    input.focus();
    loadIndex();
  }

  function closeSearch() {
    if (!modal) return;
    modal.hidden = true;
    body.style.overflow = "";
  }

  function loadIndex() {
    if (indexData || loading) return;
    loading = true;
    fetch(root + "assets/search-index.json")
      .then(function (r) {
        return r.json();
      })
      .then(function (data) {
        indexData = data;
        loading = false;
        if (input.value.trim()) runSearch(input.value);
      })
      .catch(function () {
        loading = false;
      });
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"]/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c];
    });
  }

  function highlight(text, q) {
    if (!q) return escapeHtml(text);
    var i = text.toLowerCase().indexOf(q.toLowerCase());
    if (i < 0) return escapeHtml(text);
    return (
      escapeHtml(text.slice(0, i)) +
      "<em>" +
      escapeHtml(text.slice(i, i + q.length)) +
      "</em>" +
      escapeHtml(text.slice(i + q.length))
    );
  }

  function snippet(text, q) {
    if (!text) return "";
    var i = q ? text.toLowerCase().indexOf(q.toLowerCase()) : -1;
    if (i < 0) return text.slice(0, 68);
    var start = Math.max(0, i - 28);
    var piece = text.slice(start, start + 96);
    return (start > 0 ? "…" : "") + piece + "…";
  }

  function runSearch(raw) {
    var q = raw.trim().toLowerCase();
    if (!q || !indexData) {
      current = [];
      render([]);
      return;
    }
    var scored = [];
    indexData.forEach(function (item) {
      var title = (item.t || "").toLowerCase();
      var group = (item.g || "").toLowerCase();
      var bodyText = (item.x || "").toLowerCase();
      var score = 0;
      if (title === q) score += 120;
      else if (title.indexOf(q) === 0) score += 80;
      else if (title.indexOf(q) > -1) score += 55;
      if (group.indexOf(q) > -1) score += 18;
      var idx = bodyText.indexOf(q);
      if (idx > -1) score += 26;
      var hits = bodyText.split(q).length - 1;
      score += Math.min(hits, 12) * 2;
      if (score > 0) scored.push({ item: item, score: score, idx: idx });
    });
    scored.sort(function (a, b) {
      return b.score - a.score;
    });
    current = scored.slice(0, 12).map(function (s) {
      return s.item;
    });
    cursor = -1;
    render(current, q);
  }

  function render(list, q) {
    if (!resultsBox) return;
    if (!list.length) {
      resultsBox.innerHTML = q
        ? '<div class="search-empty">没有找到匹配的文档</div>'
        : '<div class="search-empty">输入关键词搜索标题、配置项或指令</div>';
      return;
    }
    resultsBox.innerHTML = list
      .map(function (item, i) {
        return (
          '<a class="search-item" data-i="' +
          i +
          '" href="' +
          root +
          item.u +
          '">' +
          "<b>" +
          highlight(item.t, q) +
          "</b>" +
          '<span class="s-group">' +
          escapeHtml(item.g || "") +
          "</span>" +
          '<span class="s-snippet">' +
          escapeHtml(snippet(item.x, q)) +
          "</span></a>"
        );
      })
      .join("");
  }

  function moveCursor(delta) {
    var nodes = resultsBox.querySelectorAll(".search-item");
    if (!nodes.length) return;
    cursor = (cursor + delta + nodes.length) % nodes.length;
    nodes.forEach(function (n, i) {
      n.classList.toggle("is-cursor", i === cursor);
    });
    nodes[cursor].scrollIntoView({ block: "nearest" });
  }

  if (searchBtn) searchBtn.addEventListener("click", openSearch);
  if (searchClose) searchClose.addEventListener("click", closeSearch);

  if (modal) {
    modal.addEventListener("click", function (e) {
      if (e.target === modal) closeSearch();
    });
  }

  if (input) {
    input.addEventListener("input", function () {
      runSearch(input.value);
    });
    input.addEventListener("keydown", function (e) {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        moveCursor(1);
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        moveCursor(-1);
      } else if (e.key === "Enter") {
        var node = resultsBox.querySelector(".search-item.is-cursor");
        if (node) window.location.href = node.getAttribute("href");
      }
    });
  }

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") {
      closeSearch();
      closeNav();
      return;
    }
    var tag = (document.activeElement && document.activeElement.tagName) || "";
    var typing = tag === "INPUT" || tag === "TEXTAREA";
    if (
      (e.key === "/" || ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k")) &&
      !typing
    ) {
      e.preventDefault();
      openSearch();
    }
  });
})();
