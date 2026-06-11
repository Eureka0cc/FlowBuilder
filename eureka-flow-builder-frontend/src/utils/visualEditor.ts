import { ref, computed, onMounted, onUnmounted } from 'vue'

export interface VisualEditorElementInfo {
  tagName: string
  id?: string
  className?: string
  textContent?: string
  selector: string
  outerHTML?: string
}

export const VISUAL_EDITOR_MESSAGE_TYPE = {
  ENABLE: 'VISUAL_EDITOR_ENABLE',
  DISABLE: 'VISUAL_EDITOR_DISABLE',
  CLEAR_SELECTION: 'VISUAL_EDITOR_CLEAR_SELECTION',
  ELEMENT_SELECTED: 'VISUAL_EDITOR_ELEMENT_SELECTED',
  READY: 'VISUAL_EDITOR_READY',
} as const

const HOVER_OUTLINE = '2px solid #69b1ff'
const SELECTED_OUTLINE = '2px solid #1677ff'
const OUTLINE_PROP = 'outline'
const OUTLINE_OFFSET_PROP = 'outlineOffset'

type IframeLike =
  | HTMLIFrameElement
  | null
  | undefined
  | { value?: HTMLIFrameElement | null }

function resolveIframeEl(iframeEl: IframeLike): HTMLIFrameElement | null {
  if (!iframeEl) return null
  if (iframeEl instanceof HTMLIFrameElement) return iframeEl
  if (typeof iframeEl === 'object' && 'value' in iframeEl) {
    return iframeEl.value ?? null
  }
  return null
}

function getIframeTargetOrigin(iframeEl: HTMLIFrameElement): string {
  try {
    return new URL(iframeEl.src, window.location.href).origin
  } catch {
    return window.location.origin
  }
}

function isAllowedMessageOrigin(origin: string, iframeEl: HTMLIFrameElement | null): boolean {
  if (origin === window.location.origin) return true
  if (!iframeEl?.src) return false
  try {
    return origin === new URL(iframeEl.src, window.location.href).origin
  } catch {
    return false
  }
}

/** iframe 内运行的可视化编辑脚本 */
export const VISUAL_EDITOR_IFRAME_SCRIPT = `
(function () {
  if (window.__visualEditorInitialized) return;
  window.__visualEditorInitialized = true;

  var MESSAGE_TYPE = ${JSON.stringify(VISUAL_EDITOR_MESSAGE_TYPE)};
  var HOVER_OUTLINE = ${JSON.stringify(HOVER_OUTLINE)};
  var SELECTED_OUTLINE = ${JSON.stringify(SELECTED_OUTLINE)};

  var enabled = false;
  var hoveredElement = null;
  var selectedElement = null;

  function truncate(str, maxLen) {
    if (!str) return '';
    return str.length > maxLen ? str.slice(0, maxLen) + '...' : str;
  }

  function generateSelector(el) {
    if (!el || el.nodeType !== 1) return '';
    if (el.id) return '#' + CSS.escape(el.id);

    var parts = [];
    var current = el;
    while (current && current.nodeType === 1 && current !== document.documentElement) {
      var selector = current.tagName.toLowerCase();
      if (current.className && typeof current.className === 'string') {
        var classes = current.className.trim().split(/\\s+/).filter(Boolean);
        if (classes.length > 0) {
          selector += '.' + classes.map(function (c) { return CSS.escape(c); }).join('.');
        }
      }
      var parent = current.parentElement;
      if (parent) {
        var siblings = Array.from(parent.children).filter(function (child) {
          return child.tagName === current.tagName;
        });
        if (siblings.length > 1) {
          var index = siblings.indexOf(current) + 1;
          selector += ':nth-of-type(' + index + ')';
        }
      }
      parts.unshift(selector);
      if (current.id) {
        parts[0] = '#' + CSS.escape(current.id);
        break;
      }
      current = parent;
    }
    return parts.join(' > ');
  }

  function collectElementInfo(el) {
    return {
      tagName: el.tagName.toLowerCase(),
      id: el.id || undefined,
      className: el.className && typeof el.className === 'string' ? el.className : undefined,
      textContent: truncate((el.textContent || '').trim(), 200) || undefined,
      selector: generateSelector(el),
      outerHTML: truncate(el.outerHTML || '', 500) || undefined,
    };
  }

  function clearHoverOutline() {
    if (hoveredElement && hoveredElement !== selectedElement) {
      hoveredElement.style[${JSON.stringify(OUTLINE_PROP)}] = '';
      hoveredElement.style[${JSON.stringify(OUTLINE_OFFSET_PROP)}] = '';
    }
    hoveredElement = null;
  }

  function clearSelectedOutline() {
    if (selectedElement) {
      selectedElement.style[${JSON.stringify(OUTLINE_PROP)}] = '';
      selectedElement.style[${JSON.stringify(OUTLINE_OFFSET_PROP)}] = '';
      selectedElement = null;
    }
  }

  function onMouseOver(e) {
    if (!enabled) return;
    var target = e.target;
    if (!(target instanceof Element) || target === document.documentElement || target === document.body) return;
    if (target === hoveredElement) return;
    clearHoverOutline();
    hoveredElement = target;
    if (target !== selectedElement) {
      target.style[${JSON.stringify(OUTLINE_PROP)}] = HOVER_OUTLINE;
      target.style[${JSON.stringify(OUTLINE_OFFSET_PROP)}] = '2px';
    }
  }

  function onMouseOut(e) {
    if (!enabled) return;
    var target = e.target;
    if (target === hoveredElement && target !== selectedElement) {
      target.style[${JSON.stringify(OUTLINE_PROP)}] = '';
      target.style[${JSON.stringify(OUTLINE_OFFSET_PROP)}] = '';
      hoveredElement = null;
    }
  }

  function onClick(e) {
    if (!enabled) return;
    var target = e.target;
    if (!(target instanceof Element)) return;
    e.preventDefault();
    e.stopPropagation();

    clearHoverOutline();
    clearSelectedOutline();
    selectedElement = target;
    target.style[${JSON.stringify(OUTLINE_PROP)}] = SELECTED_OUTLINE;
    target.style[${JSON.stringify(OUTLINE_OFFSET_PROP)}] = '2px';

    window.parent.postMessage(
      { type: MESSAGE_TYPE.ELEMENT_SELECTED, payload: collectElementInfo(target) },
      '*'
    );
  }

  function enable() {
    if (enabled) return;
    enabled = true;
    document.addEventListener('mouseover', onMouseOver, true);
    document.addEventListener('mouseout', onMouseOut, true);
    document.addEventListener('click', onClick, true);
  }

  function disable() {
    if (!enabled) return;
    enabled = false;
    document.removeEventListener('mouseover', onMouseOver, true);
    document.removeEventListener('mouseout', onMouseOut, true);
    document.removeEventListener('click', onClick, true);
    clearHoverOutline();
    clearSelectedOutline();
  }

  function clearSelection() {
    clearHoverOutline();
    clearSelectedOutline();
  }

  window.addEventListener('message', function (event) {
    var data = event.data;
    if (!data || !data.type) return;

    switch (data.type) {
      case MESSAGE_TYPE.ENABLE:
        enable();
        break;
      case MESSAGE_TYPE.DISABLE:
        disable();
        break;
      case MESSAGE_TYPE.CLEAR_SELECTION:
        clearSelection();
        break;
    }
  });

  window.parent.postMessage({ type: MESSAGE_TYPE.READY }, '*');
})();
`

function resetIframeEditorState(iframeEl: HTMLIFrameElement): void {
  try {
    const win = iframeEl.contentWindow as (Window & { __visualEditorInitialized?: boolean }) | null
    if (win) {
      win.__visualEditorInitialized = false
    }
  } catch {
    // 跨域 iframe 无法访问，后续注入会失败
  }
}

function injectIframeScript(iframeEl: HTMLIFrameElement | null): boolean {
  if (!iframeEl) return false
  try {
    const doc = iframeEl.contentDocument
    if (!doc) return false

    const mountTarget = doc.body || doc.documentElement
    if (!mountTarget) return false

    const existing = doc.getElementById('visual-editor-script')
    if (existing) {
      existing.remove()
      resetIframeEditorState(iframeEl)
    }

    const script = doc.createElement('script')
    script.id = 'visual-editor-script'
    script.textContent = VISUAL_EDITOR_IFRAME_SCRIPT
    mountTarget.appendChild(script)
    return true
  } catch {
    return false
  }
}

function postToIframe(iframeEl: HTMLIFrameElement | null, type: string): void {
  if (!iframeEl?.contentWindow) return
  const targetOrigin = getIframeTargetOrigin(iframeEl)
  iframeEl.contentWindow.postMessage({ type }, targetOrigin)
}

function enableIframeEditor(iframeEl: HTMLIFrameElement | null): void {
  postToIframe(iframeEl, VISUAL_EDITOR_MESSAGE_TYPE.ENABLE)
}

function formatElementSummary(element: VisualEditorElementInfo): string {
  const parts = [`<${element.tagName}>`, element.selector]
  if (element.textContent) {
    parts.push(`"${element.textContent.slice(0, 50)}${element.textContent.length > 50 ? '...' : ''}"`)
  }
  return parts.join(' · ')
}

export function buildPromptWithElement(
  userMessage: string,
  element: VisualEditorElementInfo | null | undefined,
): string {
  if (!element) return userMessage

  const elementInfo = [
    '---',
    '【用户选中的页面元素】',
    `标签名: ${element.tagName}`,
    `CSS选择器: ${element.selector}`,
    `元素ID: ${element.id || '无'}`,
    `class: ${element.className || '无'}`,
    `文本内容: ${element.textContent || '无'}`,
  ].join('\n')

  return `${userMessage}\n\n${elementInfo}`
}

export function useVisualEditor(options?: {
  previewReady?: () => boolean
  onInjectFailed?: () => void
}) {
  const isEditMode = ref(false)
  const selectedElement = ref<VisualEditorElementInfo | null>(null)
  const boundIframeEl = ref<HTMLIFrameElement | null>(null)
  const iframeScriptReady = ref(false)

  let enableRetryTimers: ReturnType<typeof setTimeout>[] = []

  const selectedElementSummary = computed(() =>
    selectedElement.value ? formatElementSummary(selectedElement.value) : '',
  )

  const canUseVisualEditor = computed(() => options?.previewReady?.() ?? false)

  function clearEnableRetryTimers() {
    enableRetryTimers.forEach(clearTimeout)
    enableRetryTimers = []
  }

  function scheduleEnableRetries(iframeEl: HTMLIFrameElement | null) {
    clearEnableRetryTimers()
    ;[50, 200, 500, 1000, 2000].forEach((ms) => {
      const timer = setTimeout(() => {
        if (isEditMode.value && iframeEl) {
          enableIframeEditor(iframeEl)
        }
      }, ms)
      enableRetryTimers.push(timer)
    })
  }

  function handleMessage(event: MessageEvent) {
    const iframeEl = boundIframeEl.value
    if (!isAllowedMessageOrigin(event.origin, iframeEl)) return

    const data = event.data
    if (!data?.type) return

    if (data.type === VISUAL_EDITOR_MESSAGE_TYPE.READY) {
      iframeScriptReady.value = true
      if (isEditMode.value && iframeEl) {
        enableIframeEditor(iframeEl)
      }
      return
    }

    if (data.type === VISUAL_EDITOR_MESSAGE_TYPE.ELEMENT_SELECTED) {
      selectedElement.value = data.payload as VisualEditorElementInfo
    }
  }

  function bindIframe(iframeEl: IframeLike) {
    const el = resolveIframeEl(iframeEl)
    if (!el) return
    boundIframeEl.value = el
    iframeScriptReady.value = false

    const setup = () => {
      const tryInject = (attempt: number) => {
        if (injectIframeScript(el)) {
          if (isEditMode.value) {
            scheduleEnableRetries(el)
          }
          return
        }
        if (attempt < 8) {
          setTimeout(() => tryInject(attempt + 1), 150 * (attempt + 1))
        } else {
          options?.onInjectFailed?.()
        }
      }
      tryInject(0)
    }

    if (el.contentDocument?.readyState === 'complete') {
      setup()
    } else {
      el.addEventListener('load', setup, { once: true })
    }
  }

  function enterEditMode(iframeEl: IframeLike) {
    const el = resolveIframeEl(iframeEl)
    if (!el) {
      options?.onInjectFailed?.()
      return
    }

    isEditMode.value = true
    bindIframe(el)
    scheduleEnableRetries(el)
  }

  function exitEditMode(iframeEl: IframeLike) {
    isEditMode.value = false
    selectedElement.value = null
    iframeScriptReady.value = false
    clearEnableRetryTimers()
    postToIframe(resolveIframeEl(iframeEl), VISUAL_EDITOR_MESSAGE_TYPE.DISABLE)
  }

  function clearSelectedElement(iframeEl: IframeLike) {
    selectedElement.value = null
    postToIframe(resolveIframeEl(iframeEl), VISUAL_EDITOR_MESSAGE_TYPE.CLEAR_SELECTION)
  }

  function toggleEditMode(iframeEl: IframeLike) {
    if (isEditMode.value) {
      exitEditMode(iframeEl)
    } else {
      enterEditMode(iframeEl)
    }
  }

  onMounted(() => {
    window.addEventListener('message', handleMessage)
  })

  onUnmounted(() => {
    window.removeEventListener('message', handleMessage)
    clearEnableRetryTimers()
  })

  function reattachIframe(iframeEl: IframeLike) {
    bindIframe(iframeEl)
    if (isEditMode.value) {
      scheduleEnableRetries(resolveIframeEl(iframeEl))
    }
  }

  return {
    isEditMode,
    selectedElement,
    selectedElementSummary,
    canUseVisualEditor,
    bindIframe,
    reattachIframe,
    enterEditMode,
    exitEditMode,
    clearSelectedElement,
    toggleEditMode,
    buildPromptWithElement: (userMessage: string) =>
      buildPromptWithElement(userMessage, selectedElement.value),
  }
}
