/*
 * MiniJava-UI playground front end.
 *
 * Implements the same UI-transformation engine as the Java backend
 * (see src/main/java/com/minijava/ui/UiTransformer.java) so the playground works
 * standalone in the browser, and can also delegate to POST /api/transform to
 * exercise the Java side. Keeping both engines in lock-step is the point of the
 * app: it lets you test that a UI transformation behaves identically end-to-end.
 */
(function () {
  "use strict";

  var SAMPLE = {
    title: "Login",
    theme: "light",
    components: [
      { type: "heading", text: "Sign in to your account" },
      { type: "label", text: "Username" },
      { type: "input", name: "username", placeholder: "you@example.com" },
      { type: "label", text: "Password" },
      { type: "input", name: "password", inputType: "password" },
      { type: "button", text: "Log In" }
    ]
  };

  // ---- Transformation engine (mirror of the Java UiTransformer) ----

  function deepClone(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function components(config) {
    return Array.isArray(config.components) ? config.components : [];
  }

  var TRANSFORMS = {
    "uppercase-labels": function (config) {
      components(config).forEach(function (c) {
        if ((c.type === "label" || c.type === "heading") && c.text != null) {
          c.text = String(c.text).toUpperCase();
        }
      });
    },
    "dark-theme": function (config) {
      config.theme = "dark";
    },
    "mark-inputs-required": function (config) {
      components(config).forEach(function (c) {
        if (c.type === "input") { c.required = true; }
      });
    },
    "add-aria-labels": function (config) {
      components(config).forEach(function (c) {
        if (c.type === "input" && c.ariaLabel == null) {
          c.ariaLabel = humanize(c.name || "field");
        }
      });
    }
  };

  function humanize(name) {
    var cleaned = String(name).replace(/[_-]+/g, " ").trim();
    if (!cleaned) { return "Field"; }
    return cleaned.charAt(0).toUpperCase() + cleaned.slice(1);
  }

  function esc(raw) {
    return String(raw)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function renderComponent(c) {
    var type = c.type || "text";
    var text = c.text != null ? c.text : "";
    if (type === "heading") {
      return '<h2 class="ui-heading">' + esc(text) + "</h2>";
    }
    if (type === "label") {
      return '<label class="ui-label">' + esc(text) + "</label>";
    }
    if (type === "input") {
      var out = '<input class="ui-input" type="' + esc(c.inputType || "text") + '"';
      out += ' name="' + esc(c.name || "field") + '"';
      if (c.placeholder) { out += ' placeholder="' + esc(c.placeholder) + '"'; }
      if (c.ariaLabel != null) { out += ' aria-label="' + esc(c.ariaLabel) + '"'; }
      if (c.required) { out += " required"; }
      return out + " />";
    }
    if (type === "button") {
      return '<button class="ui-button">' + esc(text) + "</button>";
    }
    return '<p class="ui-text">' + esc(text) + "</p>";
  }

  function render(config) {
    var theme = config.theme || "light";
    var title = config.title || "Untitled UI";
    var parts = ['<section class="ui-card theme-' + esc(theme) + '">'];
    parts.push('  <div class="ui-card__title">' + esc(title) + "</div>");
    components(config).forEach(function (c) {
      parts.push("  " + renderComponent(c));
    });
    parts.push("</section>");
    return parts.join("\n");
  }

  function transformInBrowser(config, names) {
    var working = deepClone(config);
    var applied = [];
    names.forEach(function (name) {
      if (TRANSFORMS[name]) {
        TRANSFORMS[name](working);
        applied.push(name);
      }
    });
    return { config: working, html: render(working), applied: applied };
  }

  function transformOnServer(config, names) {
    return fetch("/api/transform", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ config: config, transforms: names })
    }).then(function (res) {
      if (!res.ok) { throw new Error("server responded " + res.status); }
      return res.json();
    });
  }

  // ---- Wiring ----

  var els = {
    config: document.getElementById("config"),
    parseStatus: document.getElementById("parseStatus"),
    transforms: document.getElementById("transforms"),
    run: document.getElementById("run"),
    loadSample: document.getElementById("loadSample"),
    applied: document.getElementById("applied"),
    beforePreview: document.getElementById("beforePreview"),
    afterPreview: document.getElementById("afterPreview"),
    afterHtml: document.getElementById("afterHtml")
  };

  function parseConfig() {
    try {
      var config = JSON.parse(els.config.value);
      
      // Structured logging to CloudWatch (simulated via console and API)
      console.error('[CloudWatch] Error in parseConfig:', {
        timestamp: new Date().toISOString(),
        event: 'parseConfigError',
        error: e.message,
        stack: e.stack
      });

      // Forwarding unrecoverable errors to SQS DLQ (simulated via API endpoint)
      fetch('/api/dlq/errors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          event: 'parseConfigError', 
          error: e.message, 
          timestamp: new Date().toISOString() 
        })
      }).catch(dlqErr => console.error('DLQ Forwarding failed:', dlqErr));

      // Structured logging to CloudWatch (simulated via console and API)
      console.error('[CloudWatch] Error in parseConfig:', {
        timestamp: new Date().toISOString(),
        event: 'parseConfigError',
        error: e.message,
        stack: e.stack
      });

      // Forwarding unrecoverable errors to SQS DLQ (simulated via API endpoint)
      fetch('/api/dlq/errors', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          event: 'parseConfigError', 
          error: e.message, 
          timestamp: new Date().toISOString() 
        })
      }).catch(dlqErr => console.error('DLQ Forwarding failed:', dlqErr));

      return null;
    }
  }

  function selectedTransforms() {
    return Array.prototype.slice
      .call(els.transforms.querySelectorAll("input[type=checkbox]:checked"))
      .map(function (cb) { return cb.value; });
  }

  function selectedEngine() {
    var checked = document.querySelector('input[name=engine]:checked');
    return checked ? checked.value : "js";
  }

  function showResult(result) {
    els.afterPreview.innerHTML = result.html;
    els.afterHtml.textContent = result.html;
    els.applied.textContent = result.applied.length
      ? "applied: " + result.applied.join(", ")
      : "no transformations selected";
    els.applied.className = "status ok";
  }

  function run() {
    var config = parseConfig();
    if (!config) { return; }
    els.beforePreview.innerHTML = render(config);
    var names = selectedTransforms();

    if (selectedEngine() === "java") {
      els.applied.textContent = "running on Java backend...";
      els.applied.className = "status";
      transformOnServer(config, names)
        .then(showResult)
        .catch(function (err) {
          els.applied.textContent = "backend error: " + err.message + " (is the Java server running?)";
          els.applied.className = "status error";
        });
    } else {
      showResult(transformInBrowser(config, names));
    }
  }

  function loadSample() {
    els.config.value = JSON.stringify(SAMPLE, null, 2);
    parseConfig();
    els.beforePreview.innerHTML = render(SAMPLE);
  }

  els.run.addEventListener("click", run);
  els.loadSample.addEventListener("click", loadSample);
  els.config.addEventListener("input", parseConfig);

  loadSample();
})();
