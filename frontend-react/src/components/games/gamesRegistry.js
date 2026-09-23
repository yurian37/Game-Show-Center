// Dynamic auto-discovery registry for all games in basic-plan and premium-plan
// Acts as the central Pivot Registry for Online games

const descJsonModules = import.meta.glob(
  ['./basic-plan/*/description.json', './premium-plan/*/description.json'],
  { eager: true }
);

const setupJsonModules = import.meta.glob(
  ['./basic-plan/*/setup.json', './premium-plan/*/setup.json'],
  { eager: true }
);

const setupComponentModules = import.meta.glob(
  ['./basic-plan/*/Setup.jsx', './premium-plan/*/Setup.jsx'],
  { eager: true }
);

const playComponentModules = import.meta.glob(
  ['./basic-plan/*/Play.jsx', './premium-plan/*/Play.jsx'],
  { eager: true }
);

const registry = {};
const list = [];

function sanitizeKey(name) {
  return (name || '').toLowerCase().replace(/[\s_]+/g, '');
}

for (const descPath in descJsonModules) {
  const descModule = descJsonModules[descPath];
  const parsed = descModule.default || descModule;
  
  // Plan is automatically determined by directory location
  const plan = descPath.includes('/basic-plan/') ? 'basic' : 'premium';
  
  const dirPath = descPath.replace(/\/description\.json$/, '');
  const jsonPath = `${dirPath}/setup.json`;
  const setupJsxPath = `${dirPath}/Setup.jsx`;
  const playJsxPath = `${dirPath}/Play.jsx`;
  
  const setupJsonModule = setupJsonModules[jsonPath];
  const defaultSetup = setupJsonModule ? (setupJsonModule.default || setupJsonModule) : {};
  
  const setupModule = setupComponentModules[setupJsxPath];
  const Component = setupModule ? setupModule.default : null;
  const serialize = setupModule ? setupModule.serialize : null;

  const playModule = playComponentModules[playJsxPath];
  const PlayComponent = playModule ? playModule.default : null;
  
  const gameName = parsed.name || dirPath.split('/').pop();
  
  const gameObject = {
    name: gameName,
    folderName: dirPath.split('/').pop(),
    description: parsed.description || '',
    instructions: parsed.instructions || [],
    translations: parsed.translations || {},
    author: parsed.author || 'Yuyi Studios',
    index: parsed.index !== undefined ? parsed.index : 0,
    available: parsed.available !== undefined ? parsed.available : ["1vs1", "team", "freeforall"],
    plan
  };
  
  list.push(gameObject);
  
  const entry = {
    defaultSetup,
    Component,
    PlayComponent,
    serialize,
    info: gameObject
  };

  registry[gameName] = entry;
  registry[gameName.replace(/\s+/g, '_')] = entry;
  registry[sanitizeKey(gameName)] = entry;
}

// Sort games list by plan (basic, then premium) and then by index
list.sort((a, b) => {
  if (a.plan !== b.plan) {
    return a.plan === 'basic' ? -1 : 1;
  }
  return a.index - b.index;
});

export const gamesRegistry = registry;
export const gamesList = list;

export function getGameEntry(gameName) {
  if (!gameName) return null;
  return registry[gameName] || registry[gameName.replace(/\s+/g, '_')] || registry[sanitizeKey(gameName)] || null;
}

export function getGamePlayComponent(gameName) {
  return getGameEntry(gameName)?.PlayComponent || null;
}

export function getGameSetupComponent(gameName) {
  return getGameEntry(gameName)?.Component || null;
}

export function getGameDefaultSetup(gameName) {
  return getGameEntry(gameName)?.defaultSetup || {};
}

export function getGameInfo(gameName) {
  return getGameEntry(gameName)?.info || null;
}
