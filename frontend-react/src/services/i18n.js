// Lightweight i18n service with guaranteed fallback to English ('en')

const STORAGE_KEY = 'gsc_language_preference';

export const SUPPORTED_LANGUAGES = [
  { code: 'es', name: 'Español', flagIcon: 'flag-es' },
  { code: 'en', name: 'English', flagIcon: 'flag-us' },
  { code: 'fr', name: 'Français', flagIcon: 'flag-fr' },
  { code: 'pt', name: 'Português', flagIcon: 'flag-br' }
];

let currentLang = 'es';

try {
  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved && SUPPORTED_LANGUAGES.some(l => l.code === saved)) {
    currentLang = saved;
  }
} catch (ignored) {}

const listeners = new Set();

export function getCurrentLanguage() {
  return currentLang;
}

export function setLanguage(langCode) {
  if (!langCode || !SUPPORTED_LANGUAGES.some(l => l.code === langCode.toLowerCase())) return;
  currentLang = langCode.toLowerCase();
  try {
    localStorage.setItem(STORAGE_KEY, currentLang);
  } catch (ignored) {}
  listeners.forEach(fn => {
    try { fn(currentLang); } catch (e) { console.error(e); }
  });
}

export function subscribeLanguageChange(callback) {
  listeners.add(callback);
  return () => listeners.delete(callback);
}

/**
 * Resolves localized game property (name, description, instructions)
 * with strict fallback order:
 * 1. Requested language (e.g. 'es', 'fr', 'pt')
 * 2. English ('en')
 * 3. Default top-level property
 * 4. Fallback default string
 */
export function getLocalizedGameProperty(game, property, lang = currentLang) {
  if (!game) return '';
  const langKey = (lang || 'en').toLowerCase();
  const translations = game.translations || {};

  // 1. Try requested language
  if (translations[langKey] && translations[langKey][property]) {
    const val = translations[langKey][property];
    if (Array.isArray(val) ? val.length > 0 : String(val).trim() !== '') {
      return val;
    }
  }

  // 2. Fallback to English ('en')
  if (translations.en && translations.en[property]) {
    const val = translations.en[property];
    if (Array.isArray(val) ? val.length > 0 : String(val).trim() !== '') {
      return val;
    }
  }

  // 3. Fallback to top-level property
  if (game[property]) {
    const val = game[property];
    if (Array.isArray(val) ? val.length > 0 : String(val).trim() !== '') {
      return val;
    }
  }

  return '';
}

export function getGameName(game, lang = currentLang) {
  return getLocalizedGameProperty(game, 'name', lang) || game?.name || 'Juego';
}

export function getGameDescription(game, lang = currentLang) {
  return getLocalizedGameProperty(game, 'description', lang) || game?.description || '';
}

export function getGameInstructions(game, lang = currentLang) {
  const instr = getLocalizedGameProperty(game, 'instructions', lang);
  if (Array.isArray(instr)) {
    return instr;
  }
  if (typeof instr === 'string' && instr.trim()) {
    return instr.split('\n').filter(s => s.trim().length > 0);
  }
  return [];
}
