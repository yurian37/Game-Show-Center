import React, { useState, useMemo } from 'react';
import {
  getGamePlayComponent,
  getGameDefaultSetup,
  getGameInfo
} from './gamesRegistry';

/**
 * GameArenaDriver: Auxiliary Driver/Adapter layer connecting ArenaStage with individual games.
 * Decouples ArenaStage from specific games for infinite scalability.
 * Displays game rules and instructions in English.
 */
export default function GameArenaDriver({
  activeGame,
  profiles = [],
  matchData = {},
  onSelectWinner
}) {
  const [showInstructions, setShowInstructions] = useState(false);

  const gameName = activeGame?.name || '';
  const gameInfo = useMemo(() => getGameInfo(gameName), [gameName]);
  const PlayComponent = useMemo(() => getGamePlayComponent(gameName), [gameName]);

  const setupData = useMemo(() => {
    return (
      activeGame?.setupData ||
      matchData?.gamesSetup?.[gameName] ||
      matchData?.gamesSetup?.[gameName.replace(/\s+/g, '_')] ||
      activeGame?.setup ||
      getGameDefaultSetup(gameName) ||
      {}
    );
  }, [activeGame, matchData, gameName]);

  const gameTitle = gameInfo?.name || gameName;
  const gameDesc = gameInfo?.description || '';
  const instructionsList = useMemo(() => {
    if (Array.isArray(gameInfo?.instructions)) return gameInfo.instructions;
    return [];
  }, [gameInfo]);

  if (!activeGame) {
    return (
      <div className="py-12 text-center text-slate-500 text-sm font-semibold">
        No active game selected.
      </div>
    );
  }

  return (
    <div className="w-full flex flex-col items-center">
      {/* Driver Auxiliary Bar: Quick Rules / Instructions Button */}
      <div className="w-full max-w-5xl flex justify-end mb-2 px-2">
        <button
          type="button"
          onClick={() => setShowInstructions(true)}
          className="bg-[#1b2238]/80 hover:bg-[#252f4c] text-indigo-300 hover:text-indigo-200 border border-indigo-500/30 text-xs font-bold px-3.5 py-1.5 rounded-xl shadow-md transition-all flex items-center gap-2 cursor-pointer active:scale-95"
          title="View game rules and instructions"
        >
          <span>📖</span>
          <span>Game Rules & Instructions</span>
        </button>
      </div>

      {/* Embedded Game Mount */}
      <div className="w-full">
        {PlayComponent ? (
          <PlayComponent
            profiles={profiles}
            setupData={setupData}
            onSelectWinner={onSelectWinner}
          />
        ) : (
          <div className="space-y-6 max-w-xl mx-auto animate-fadeIn py-8 text-center">
            <div className="w-20 h-20 mx-auto rounded-3xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-4xl shadow-inner">
              🎮
            </div>
            <div>
              <h3 className="text-2xl font-black text-indigo-300 mb-2">{gameTitle}</h3>
              <p className="text-xs text-slate-400 leading-relaxed max-w-md mx-auto">{gameDesc}</p>
            </div>
            <div className="inline-flex items-center gap-3 bg-[#141929] px-4 py-2 rounded-xl border border-slate-800 text-xs text-slate-300 font-mono">
              <span>Author: <strong className="text-purple-400">{gameInfo?.author || 'Yuyi Studios'}</strong></span>
              <span>•</span>
              <span>Plan: <strong className="text-amber-400 uppercase">{gameInfo?.plan || 'basic'}</strong></span>
            </div>
          </div>
        )}
      </div>

      {/* Instructions Modal */}
      {showInstructions && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn">
          <div className="w-full max-w-lg bg-[#121624] border-2 border-indigo-500/40 rounded-3xl p-6 shadow-2xl flex flex-col gap-4 relative">
            {/* Header with Title */}
            <div className="flex items-start justify-between border-b border-slate-800 pb-3 gap-3">
              <div>
                <span className="text-[10px] font-black uppercase tracking-widest text-indigo-400 block mb-1">
                  How to Play
                </span>
                <h3 className="text-lg font-black text-white flex items-center gap-2">
                  <span>🎮 {gameTitle}</span>
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowInstructions(false)}
                className="text-slate-400 hover:text-white p-1 rounded-lg text-sm cursor-pointer transition-colors"
                title="Close"
              >
                ✕
              </button>
            </div>

            {/* Description */}
            {gameDesc && (
              <p className="text-xs text-slate-300 leading-relaxed italic bg-[#0f121d] p-3 rounded-xl border border-slate-800/80">
                "{gameDesc}"
              </p>
            )}

            {/* Rules / Steps List */}
            <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
              <h4 className="text-[11px] font-black uppercase text-slate-400 tracking-wider">
                Game Rules & Mechanics:
              </h4>

              {instructionsList.length > 0 ? (
                <ul className="space-y-2 text-xs text-slate-200">
                  {instructionsList.map((step, idx) => (
                    <li key={idx} className="flex items-start gap-2.5 bg-[#181f34]/60 p-2.5 rounded-xl border border-slate-800/60">
                      <span className="w-5 h-5 rounded-full bg-indigo-600/30 text-indigo-300 font-bold text-[10px] flex items-center justify-center shrink-0 mt-0.5">
                        {idx + 1}
                      </span>
                      <span className="leading-relaxed">{step}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <div className="text-xs text-slate-500 py-4 text-center">
                  No additional instructions available for this game.
                </div>
              )}
            </div>

            {/* Footer Close Button */}
            <div className="pt-2 border-t border-slate-800 flex justify-end">
              <button
                type="button"
                onClick={() => setShowInstructions(false)}
                className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold px-6 py-2 rounded-xl text-xs uppercase tracking-wider transition-colors shadow-lg cursor-pointer"
              >
                Got it
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
