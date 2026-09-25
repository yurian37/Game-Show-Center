import React from 'react';

// Registry of raw SVG strings or direct paths for all 92 icons
const emojiToNameMap = {
  '🏆': 'trophy',
  '⚙': 'gear',
  '⚙️': 'gear',
  '🎯': 'target',
  '⚠': 'warning',
  '⚠️': 'warning',
  '✨': 'sparkles',
  '🔒': 'lock',
  '🔄': 'refresh',
  '🏁': 'flag',
  '⚡': 'lightning',
  '🚀': 'rocket',
  '⚔': 'swords',
  '⚔️': 'swords',
  '👑': 'crown',
  '💡': 'bulb',
  '⏱': 'stopwatch',
  '👁': 'eye',
  '👁️': 'eye',
  '⏳': 'hourglass',
  '⌛': 'hourglass',
  '🖼': 'image',
  '🖼️': 'image',
  '✓': 'check',
  '✅': 'check-circle',
  '💾': 'save',
  '⏸': 'pause',
  '▶': 'play',
  '🎮': 'gamepad',
  '🌐': 'globe',
  '🌍': 'globe',
  '📁': 'folder',
  '📂': 'folder-open',
  '🙈': 'monkey-hide',
  '🎭': 'masks',
  '🔍': 'search',
  '🔎': 'search-plus',
  '➕': 'plus',
  '✕': 'close',
  '❌': 'close',
  '✖': 'close',
  '⏹': 'stop',
  '📖': 'book',
  '🗑': 'trash',
  '🗑️': 'trash',
  '🎵': 'music',
  '🌪': 'vortex',
  '🌀': 'vortex',
  '🚧': 'construction',
  '🚫': 'prohibited',
  '📊': 'chart',
  '🔊': 'speaker',
  '🔇': 'speaker-mute',
  '🎰': 'slot-machine',
  '👤': 'user',
  '👥': 'users',
  '🥇': 'medal-gold',
  '🥈': 'medal-silver',
  '🥉': 'medal-bronze',
  '🔤': 'letters-abc',
  '🔠': 'letters-case',
  '🎲': 'dice',
  '📸': 'camera-flash',
  '📷': 'camera',
  '⏮': 'previous',
  '↩': 'back',
  '🠔': 'back',
  '📋': 'clipboard',
  '🔗': 'link',
  '📄': 'document',
  '🤝': 'handshake',
  '🌫': 'fog',
  '🎨': 'palette',
  '⭐': 'star',
  '★': 'star',
  '🔥': 'fire',
  '💀': 'skull',
  '📜': 'scroll',
  '❓': 'question',
  '🧭': 'compass',
  '🏛': 'monument',
  '🧱': 'bricks',
  '😢': 'sad',
  '📢': 'megaphone',
  '🎉': 'party',
  '📥': 'inbox',
  '🏠': 'home',
  '🖥': 'desktop',
  '❤': 'heart',
  '🖤': 'heart',
  '💳': 'credit-card',
  '🔑': 'key',
  '🧩': 'puzzle',
  '🔴': 'circle-dot',
  '🔵': 'circle-dot',
  '🟡': 'circle-dot',
  '⚪': 'circle-outline',
  '●': 'circle-filled',
  '○': 'circle-outline',
  '▼': 'caret-down',
  '➔': 'arrow-right',
  '👇': 'arrow-down',
  '🧊': 'ice',
  '🍊': 'citrus',
  '🧪': 'flask',
  '〰': 'wave',
  '〰️': 'wave',
  '🇪🇸': 'flag-es',
  '🇺🇸': 'flag-us',
  '🇫🇷': 'flag-fr',
  '🇧🇷': 'flag-br'
};

// Import all SVGs as raw or URL via Vite glob import
const svgModules = import.meta.glob('../assets/emojis/*.svg', { eager: true, query: '?raw', import: 'default' });

// Cache parsed SVG elements
const svgCache = {};

export function resolveIconName(nameOrEmoji) {
  if (!nameOrEmoji) return 'gear';
  return emojiToNameMap[nameOrEmoji] || nameOrEmoji;
}

export default function SvgEmoji({ name, emoji, size, className = '', style = {}, ...props }) {
  const iconName = resolveIconName(emoji || name);
  const path = `../assets/emojis/${iconName}.svg`;
  const rawSvg = svgModules[path];

  const sizeStyle = size ? { width: typeof size === 'number' ? `${size}px` : size, height: typeof size === 'number' ? `${size}px` : size } : {};

  if (!rawSvg) {
    return <span className={`inline-block ${className}`} style={{ ...sizeStyle, ...style }} {...props}>{emoji || name}</span>;
  }

  return (
    <span
      className={`inline-flex items-center justify-center align-middle shrink-0 select-none ${className}`}
      style={{
        display: 'inline-flex',
        verticalAlign: '-0.125em',
        width: '1em',
        height: '1em',
        color: 'inherit',
        fill: 'currentColor',
        ...sizeStyle,
        ...style
      }}
      dangerouslySetInnerHTML={{ __html: rawSvg }}
      {...props}
    />
  );
}

// Regex matching all mapped emojis
const EMOJI_REGEX = /([\uD800-\uDBFF][\uDC00-\uDFFF]|[\u2600-\u27bf\u2300-\u23ff\u2b50-\u2b55\u203c-\u2049\u2194-\u21aa\u25aa-\u25fe\u2700-\u27bf\u21a9\u2705\u274c\u2716\u2795\u2702-\u27b0\u3030])/g;

/**
 * Replaces emoji characters inside a text string with <SvgEmoji /> components.
 */
export function renderTextWithEmojis(text, className = '') {
  if (typeof text !== 'string') return text;
  const parts = [];
  let lastIndex = 0;
  let match;

  EMOJI_REGEX.lastIndex = 0;
  while ((match = EMOJI_REGEX.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    const emojiChar = match[0];
    const iconName = emojiToNameMap[emojiChar];
    if (iconName) {
      parts.push(<SvgEmoji key={match.index} emoji={emojiChar} className={className} />);
    } else {
      parts.push(emojiChar);
    }
    lastIndex = EMOJI_REGEX.lastIndex;
  }

  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }

  return parts.length > 0 ? parts : text;
}
