/* CityHub 图标：24px 网格、1.8px 圆角描边，颜色随主题继承。
 * 动态模板使用 icon('name')；静态 HTML 使用 <span data-icon="name"></span>。
 * 图标只作视觉辅助，纯图标按钮需在按钮上提供 aria-label。
 */
const UI_ICONS = Object.freeze({
  pin: '<path d="M20 10c0 6-8 11-8 11S4 16 4 10a8 8 0 1 1 16 0Z"/><circle cx="12" cy="10" r="2.5"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  home: '<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z"/>',
  store: '<path d="m3 9 2-6h14l2 6M3 9a3 3 0 0 0 6 0 3 3 0 0 0 6 0 3 3 0 0 0 6 0M5 12v9h14v-9M9 21v-6h6v6"/>',
  utensils: '<path d="M4 3v5a3 3 0 0 0 6 0V3M7 3v18M20 21V3c-4 2-5 6-5 10h5"/>',
  mic: '<rect x="9" y="2" width="6" height="12" rx="3"/><path d="M5 10v2a7 7 0 0 0 14 0v-2M12 19v3M8 22h8"/>',
  scissors: '<circle cx="5" cy="6" r="3"/><circle cx="5" cy="18" r="3"/><path d="m7.5 7.5 13 13m-13-4 13-13"/>',
  dumbbell: '<path d="M3 9v6M6 6v12M18 6v12M21 9v6M6 12h12M3 12h3m12 0h3"/>',
  leaf: '<path d="M20 3C9 2 3 6 4 13c1 7 8 9 13 4 3-3 3-8 3-14ZM4 21 15 10"/>',
  sparkles: '<path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5ZM4 2v4M2 4h4M20 18v4m-2-2h4"/>',
  smile: '<circle cx="12" cy="12" r="9"/><path d="M8 9h.01M16 9h.01M8 14a4 4 0 0 0 8 0"/>',
  wine: '<path d="M7 3h10l1 6a6 6 0 0 1-12 0ZM6 9h12M12 15v6M8 21h8"/>',
  music: '<path d="M9 18V5l12-2v13M9 9l12-2"/><ellipse cx="6" cy="18" rx="3" ry="3"/><ellipse cx="18" cy="16" rx="3" ry="3"/>',
  bed: '<path d="M3 18v3m18-3v3M3 18V6m18 12v-7a2 2 0 0 0-2-2h-8v9M3 18h18"/><circle cx="7" cy="11" r="2"/>',
  coffee: '<path d="M4 8h12v8a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4ZM16 8h2a3 3 0 0 1 0 6h-2M7 2v3m6-3v3M2 22h18"/>',
  bag: '<path d="M5 7h14l2 14H3ZM9 8V5a3 3 0 0 1 6 0v3"/>',
  heart: '<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0l-1 1-1-1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/>',
  star: '<path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9Z"/>',
  message: '<path d="M21 11.5a8.5 8.5 0 0 1-8.5 8.5H4l-2 2V11.5a9.5 9.5 0 0 1 19 0Z"/><path d="M7 9h9M7 13h6"/>',
  ticket: '<path d="M3 7a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v3a2 2 0 0 0 0 4v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-3a2 2 0 0 0 0-4ZM15 5v2m0 4v2m0 4v2"/>',
  zap: '<path d="m13 2-9 12h7l-1 8 10-12h-8Z"/>',
  file: '<path d="M14 2H5a1 1 0 0 0-1 1v18a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V8ZM14 2v6h6M8 12h8M8 16h6"/>',
  edit: '<path d="m16 3 5 5M4 16 17 3a2.1 2.1 0 0 1 3 3L7 19l-5 1ZM13 21h8"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 11h18m-14 5 3 3 6-6"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v2"/>',
  users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2m20 0v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8"/><circle cx="9" cy="7" r="4"/>',
  lock: '<rect x="4" y="10" width="16" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 15v2"/>',
  inbox: '<path d="m3 13 3-9h12l3 9v7H3ZM3 13h5l2 3h4l2-3h5"/>',
  alert: '<circle cx="12" cy="12" r="9"/><path d="M12 7v6m0 4h.01"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6m0-10h.01"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  'arrow-right': '<path d="M4 12h16m-6-6 6 6-6 6"/>',
  'arrow-left': '<path d="M20 12H4m6-6-6 6 6 6"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4m11.4 11.4 1.4 1.4M2 12h2m16 0h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8Z"/>',
})

function icon(name, className = '') {
  const key = Object.prototype.hasOwnProperty.call(UI_ICONS, name) ? name : 'store'
  const classes = className.replace(/[^a-zA-Z0-9_\s-]/g, '')
  return `<svg class="ui-icon ui-icon-${key}${classes ? ' ' + classes : ''}" xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">${UI_ICONS[key]}</svg>`
}

// 按分类名称匹配，统一兼容旧数据中的 Emoji 或图片路径；未知分类使用商店图标。
function shopTypeIcon(type) {
  const categories = [
    [/美食|餐饮|小吃|火锅|烧烤/, 'utensils'],
    [/ktv|唱歌/i, 'mic'],
    [/美发|理发/, 'scissors'],
    [/健身|运动/, 'dumbbell'],
    [/按摩|足疗|spa/i, 'leaf'],
    [/丽人|美容|美睫|美甲/, 'sparkles'],
    [/亲子|游乐/, 'smile'],
    [/酒吧/, 'wine'],
    [/轰趴|娱乐/, 'music'],
    [/酒店|住宿|民宿/, 'bed'],
    [/咖啡|茶饮|甜品/, 'coffee'],
    [/购物|商场/, 'bag'],
  ]
  const category = categories.find(([pattern]) => pattern.test(type.name || ''))
  return icon(category ? category[1] : 'store')
}

// 各页面在正文末尾加载本文件，静态标记可立即渲染，无需监听动态 DOM。
document.querySelectorAll('[data-icon]').forEach((element) => {
  element.outerHTML = icon(element.dataset.icon, element.className)
})
