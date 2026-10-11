(function () {
  'use strict';
  if (window.__browxerSniffer) return;
  window.__browxerSniffer = true;

  var MAX_BLOB = 50 * 1024 * 1024; // honest cap — OOM protection
  var CHUNK = 512 * 1024;
  var MEDIA_EXT = /\.(mp4|m4v|webm|mkv|mov|m3u8|mpd|mp3|m4a|aac|ogg|opus|flac)(\?|$)/i;
  var seen = {};

  function abs(u) { try { return new URL(u, location.href).href; } catch (e) { return null; } }
  function push(items) {
    // dedupe inside the page to keep the bridge message small
    var fresh = items.filter(function (it) {
      if (!it.url || seen[it.url]) return false;
      seen[it.url] = 1; return true;
    });
    if (fresh.length) { try { Browxer.onMediaFound(JSON.stringify(fresh)); } catch (e) {} }
  }

  function scanVideo(v, source) {
    var items = [];
    var src = v.src || v.currentSrc;
    if (src && !/^blob:/.test(src)) {
      items.push({ url: abs(src), type: v.type || 'video/mp4', source: source,
        width: v.videoWidth || -1, height: v.videoHeight || -1,
        duration: isFinite(v.duration) ? v.duration : -1,
        poster: v.poster ? abs(v.poster) : '', pageUrl: location.href });
    }
    v.querySelectorAll('source').forEach(function (s) {
      if (s.src && !/^blob:/.test(s.src))
        items.push({ url: abs(s.src), type: s.type || 'video', source: source,
          width: v.videoWidth || -1, height: v.videoHeight || -1,
          duration: isFinite(v.duration) ? v.duration : -1, pageUrl: location.href });
    });
    if (items.length) push(items);
  }

  function scanMeta() {
    var items = [];
    ['og:video', 'og:video:secure_url', 'og:image', 'twitter:player:stream'].forEach(function (name) {
      document.querySelectorAll('meta[property="' + name + '" i], meta[name="' + name + '" i]')
        .forEach(function (m) {
          var c = m.content || m.getAttribute('content');
          if (c) items.push({ url: abs(c),
            type: name.indexOf('image') >= 0 ? 'image' : 'video',
            source: 'OG_META', pageUrl: location.href });
        });
    });
    document.querySelectorAll('script[type="application/ld+json"]').forEach(function (s) {
      try {
        var data = JSON.parse(s.textContent);
        (Array.isArray(data) ? data : [data]).forEach(function (obj) {
          if (obj && obj['@type'] === 'VideoObject') {
            var url = obj.contentUrl || obj.embedUrl;
            if (url) items.push({ url: abs(url),
              type: obj.encodingFormat || 'video', source: 'JSON_LD',
              poster: obj.thumbnailUrl ? abs(obj.thumbnailUrl) : '',
              pageUrl: location.href });
          }
        });
      } catch (e) { /* malformed ld+json — ignore */ }
    });
    if (items.length) push(items);
  }

  function scanImages() {
    var items = [];
    document.querySelectorAll('img').forEach(function (img) {
      var src = img.currentSrc || img.src;
      if (!src) return;
      var isBig = (img.naturalWidth >= 800 && img.naturalHeight >= 800) ||
        /\.(jpe?g|png|webp)(\?|$)/i.test(src);
      if (isBig)
        items.push({ url: abs(src), type: 'image', source: 'DOM_IMAGE',
          width: img.naturalWidth, height: img.naturalHeight, pageUrl: location.href });
    });
    if (items.length) push(items);
  }

  function scanPreloads() {
    document.querySelectorAll('link[rel="preload"], link[rel="prefetch"]').forEach(function (l) {
      var as = (l.as || '').toLowerCase();
      if ((as === 'video' || as === 'audio' || as === 'fetch') && l.href && MEDIA_EXT.test(l.href))
        push([{ url: abs(l.href), type: as === 'audio' ? 'audio' : 'video',
          source: 'NETWORK_XHR', pageUrl: location.href }]);
    });
  }

  // ---- Layer 3: blob capture (512 KB chunks, 50 MB cap, honest overflow report) ----
  var nativeCreate = URL.createObjectURL;
  URL.createObjectURL = function (blob) {
    var url = nativeCreate.call(URL, blob);
    try {
      var size = blob && blob.size ? blob.size : 0;
      var isMedia = blob &&
        (String(blob.type).startsWith('video/') || String(blob.type).startsWith('audio/'));
      if (size && isMedia && size <= MAX_BLOB) {
        var blobId = 'bx_' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
        blob.arrayBuffer().then(function (buf) {
          var bytes = new Uint8Array(buf), off = 0, idx = 0;
          (function sendChunk() {
            if (off >= bytes.length) return;
            var end = Math.min(off + CHUNK, bytes.length);
            var bin = '';
            for (var i = off; i < end; i += 8192)
              bin += String.fromCharCode.apply(null, bytes.subarray(i, Math.min(i + 8192, end)));
            Browxer.onBlobChunk(
              JSON.stringify({ blobId: blobId, size: size, type: blob.type,
                               chunkIndex: idx++, pageUrl: location.href }),
              btoa(bin));
            off = end;
            setTimeout(sendChunk, 0); // yield — never block the page
          })();
        });
      } else if (size && isMedia) {
        // Honest: refuse oversized blobs instead of crashing with OOM
        Browxer.onBlobTooLarge(JSON.stringify({ size: size, type: blob.type }));
      }
    } catch (e) { /* never break the host page */ }
    return url;
  };

  // ---- fetch / XHR hooks ----
  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function (input, init) {
      var url = typeof input === 'string' ? input : (input && input.url) || '';
      return origFetch.apply(this, arguments).then(function (resp) {
        if (url && MEDIA_EXT.test(url)) {
          push([{ url: abs(url),
            type: resp.headers.get('content-type') || 'video',
            source: 'NETWORK_XHR',
            size: +(resp.headers.get('content-length') || -1),
            pageUrl: location.href }]);
        }
        return resp;
      });
    };
  }
  var origOpen = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (method, url) {
    this.addEventListener('load', function () {
      if (url && MEDIA_EXT.test(url)) {
        push([{ url: abs(url),
          type: this.getResponseHeader('content-type') || 'video',
          source: 'NETWORK_XHR', pageUrl: location.href }]);
      }
    });
    return origOpen.apply(this, arguments);
  };

  // ---- Same-origin iframe recursion only (respect same-origin policy) ----
  function injectIframes() {
    document.querySelectorAll('iframe').forEach(function (f) {
      var d;
      try { d = f.contentDocument; } catch (e) { return; } // cross-origin -> skip
      if (!d || !d.defaultView || d.defaultView.__browxerSniffer) return;
      d.defaultView.__browxerSniffer = true;
      d.querySelectorAll('video, audio').forEach(function (v) {
        var src = v.src || v.currentSrc;
        if (src) {
          try {
            Browxer.onIframeMediaFound(JSON.stringify([{
              url: new URL(src, location.href).href,
              type: v.type || 'video', source: 'DOM_VIDEO',
              width: v.videoWidth || -1, height: v.videoHeight || -1,
              pageUrl: location.href }]));
          } catch (e) {}
        }
      });
    });
  }

  // ---- Debounced MutationObserver for SPA pages ----
  var scheduled = false;
  function scheduleScan() {
    if (scheduled) return;
    scheduled = true;
    setTimeout(function () { scheduled = false; scanAll(); }, 400);
  }

  function scanAll() {
    document.querySelectorAll('video').forEach(function (v) { scanVideo(v, 'DOM_VIDEO'); });
    document.querySelectorAll('audio').forEach(function (a) {
      var src = a.src || a.currentSrc;
      if (src && !/^blob:/.test(src))
        push([{ url: abs(src), type: a.type || 'audio/mpeg', source: 'DOM_AUDIO',
          duration: isFinite(a.duration) ? a.duration : -1, pageUrl: location.href }]);
    });
    scanMeta(); scanImages(); scanPreloads(); injectIframes();
  }

  scanAll();
  new MutationObserver(scheduleScan).observe(document.documentElement, {
    childList: true, subtree: true, attributes: true, attributeFilter: ['src']
  });
  setTimeout(scanAll, 1500); // lazy players
  setTimeout(scanAll, 4000);
})();
