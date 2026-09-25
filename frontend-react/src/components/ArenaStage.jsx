import React, { useState, useEffect } from 'react';
import SvgEmoji from './SvgEmoji';
import GameArenaDriver from './games/GameArenaDriver';
import soundManager from '../services/soundManager';
import ArenaRouletteModal from './ArenaRouletteModal';

export default function ArenaStage({ matchData, onNavigate }) {
  const [currentGameIndex, setCurrentGameIndex] = useState(0);
  const [isRouletteModalOpen, setIsRouletteModalOpen] = useState(false);

  // Stop any playing sound / song whenever switching minigames or unmounting
  useEffect(() => {
    soundManager.stopAll();
    return () => {
      soundManager.stopAll();
    };
  }, [currentGameIndex]);

  const selectedGames = Array.isArray(matchData?.selectedGames) ? matchData.selectedGames : [];
  const gameMode = matchData?.gameMode || '1vs1';
  const profiles = Array.isArray(matchData?.profiles) ? matchData.profiles : [];
  const activeGame = selectedGames[currentGameIndex] || null;

  // Presets de puntaje (4 botones por defecto: +10, +20, +40, -10)
  const defaultPresets = [
    { id: 1, value: '10' },
    { id: 2, value: '20' },
    { id: 3, value: '40' },
    { id: 4, value: '-10' }
  ];
  const rawScorePresets = matchData?.scorePresets;
  const scorePresets = Array.isArray(rawScorePresets) && rawScorePresets.length > 0 ? rawScorePresets : defaultPresets;
  const fourScoreButtons = scorePresets.slice(0, 4);

  // Estado para los puntajes de los competidores { [profileId]: number }
  const [scores, setScores] = useState(() => {
    const initialScores = {};
    if (Array.isArray(profiles)) {
      profiles.forEach(p => {
        if (p && p.id) {
          initialScores[p.id] = 0;
        }
      });
    }
    return initialScores;
  });

  // Competidor seleccionado para ajustar puntos
  const [selectedProfileId, setSelectedProfileId] = useState(() => {
    return (Array.isArray(profiles) && profiles[0]?.id) ? profiles[0].id : null;
  });

  const handleNextGame = () => {
    soundManager.stopAll();
    if (currentGameIndex < selectedGames.length - 1) {
      setCurrentGameIndex(prev => prev + 1);
    }
  };

  const handlePrevGame = () => {
    soundManager.stopAll();
    if (currentGameIndex > 0) {
      setCurrentGameIndex(prev => prev - 1);
    }
  };

  const handleAdjustScore = (profileId, valStr) => {
    const val = parseInt(valStr) || 0;
    setScores(prev => ({
      ...prev,
      [profileId]: (prev[profileId] || 0) + val
    }));
  };

  const handleResetScore = (profileId) => {
    setScores(prev => ({
      ...prev,
      [profileId]: 0
    }));
  };

  const [showEndMatchModal, setShowEndMatchModal] = useState(false);

  const handleEndMatch = () => {
    soundManager.stopAll();
    onNavigate('winner', {
      ...matchData,
      profiles,
      scores,
      gameMode,
      selectedGames
    });
  };

  const activeProfile = profiles.find(p => p.id === selectedProfileId) || profiles[0] || null;

  return (
    <div className="w-full max-w-6xl bg-[#141929] border border-slate-800/60 rounded-[2.5rem] p-6 md:p-10 shadow-[0_30px_70px_-15px_rgba(0,0,0,0.8)] flex flex-col animate-fadeIn text-slate-200 relative">
      
      {/* HEADER DE LA ARENA */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4 border-b border-slate-800/60 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-[10px] font-black tracking-widest text-indigo-400 uppercase bg-indigo-500/10 px-2.5 py-1 rounded-md border border-indigo-500/20 inline-flex items-center gap-1.5">
              <SvgEmoji name="circle-dot" /> Live Game Show Arena
            </span>
            <span className="text-[10px] font-black tracking-widest text-purple-400 uppercase bg-purple-500/10 px-2.5 py-1 rounded-md border border-purple-500/20">
              Mode: {gameMode}
            </span>
          </div>
          <h2 className="text-2xl md:text-3xl font-black tracking-tight mt-2 flex items-center gap-3">
            <span>{activeGame ? activeGame.name : 'Game Show Stage'}</span>
            <span className="text-sm font-bold text-slate-400 bg-slate-800 px-3 py-1 rounded-full">
              Game {currentGameIndex + 1} of {selectedGames.length}
            </span>
          </h2>
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            onClick={() => setIsRouletteModalOpen(true)}
            className="text-xs font-black text-amber-300 bg-amber-500/10 hover:bg-amber-500/20 flex items-center gap-1.5 uppercase tracking-wider transition-all px-4 py-2.5 rounded-xl border border-amber-500/30 shadow-md shadow-amber-500/10 active:scale-95 cursor-pointer"
          >
            <SvgEmoji name="slot-machine" /> Roulette
          </button>
          <button 
            onClick={() => setShowEndMatchModal(true)}
            className="text-xs font-black text-slate-950 bg-gradient-to-r from-amber-400 to-yellow-300 hover:from-amber-300 hover:to-yellow-200 flex items-center gap-1.5 uppercase tracking-wider transition-all px-4 py-2.5 rounded-xl border border-yellow-200 shadow-md shadow-amber-500/20 active:scale-95 cursor-pointer"
          >
            <SvgEmoji name="flag" /> End Match & Announce Winner
          </button>
          <button 
            onClick={() => onNavigate('settings')}
            className="text-xs font-bold text-slate-400 hover:text-slate-200 flex items-center gap-1.5 uppercase tracking-wider transition-colors bg-slate-800/50 px-4 py-2.5 rounded-xl border border-slate-700/60 cursor-pointer"
          >
            <SvgEmoji name="gear" /> Match Settings
          </button>
        </div>
      </div>

      {/* BENCH DE PARTICIPANTES (PUNTAJES SIEMPRE VISIBLES) */}
      <div className="mb-8">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-xs font-black text-indigo-300 uppercase tracking-widest">
            Competitors Bench ({profiles.length}) — <span className="text-slate-400">Click a competitor to adjust scores</span>
          </h3>
          {activeProfile && (
            <span className="text-xs font-bold text-amber-400 bg-amber-500/10 px-3 py-1 rounded-full border border-amber-500/20">
              Active: {activeProfile.name} ({scores[activeProfile.id] || 0} pts)
            </span>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-8 gap-3">
          {profiles.map((p, idx) => {
            const isSelected = p.id === selectedProfileId;
            const currentScore = scores[p.id] || 0;

            return (
              <div 
                key={p.id || idx}
                onClick={() => setSelectedProfileId(p.id)}
                className={`cursor-pointer border p-3 rounded-2xl flex flex-col items-center gap-2 text-center transition-all duration-200 relative group ${
                  isSelected 
                    ? 'bg-[#1b2238] border-indigo-500 ring-2 ring-indigo-500/50 shadow-[0_0_20px_rgba(99,102,241,0.3)] scale-[1.03]' 
                    : 'bg-[#121624] border-slate-800 hover:border-slate-700 hover:bg-[#161c2e]'
                }`}
              >
                {/* FOTO DE PERFIL */}
                <div className="w-14 h-14 rounded-xl bg-[#0f121d] border border-slate-700 overflow-hidden relative flex items-center justify-center shadow-inner">
                  {p.avatar ? (
                    <img src={p.avatar} alt={p.name} className="w-full h-full object-cover" />
                  ) : (
                    <span className="text-xl text-slate-500 font-bold flex items-center justify-center">
                      <SvgEmoji name="user" />
                    </span>
                  )}
                  {isSelected && (
                    <span className="absolute top-0 right-0 w-3.5 h-3.5 bg-indigo-500 rounded-bl-lg border-b border-l border-indigo-400 animate-pulse" />
                  )}
                </div>

                <span className="text-xs font-bold text-slate-200 truncate w-full">
                  {p.name}
                </span>

                {/* PUNTAJE SIEMPRE VISIBLE EN EL PERFIL */}
                <div className={`w-full py-1 px-2 rounded-xl text-xs font-black tracking-wide border shadow-sm ${
                  currentScore > 0 
                    ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30' 
                    : currentScore < 0 
                      ? 'bg-rose-500/20 text-rose-300 border-rose-500/30' 
                      : 'bg-slate-800/80 text-slate-300 border-slate-700/60'
                }`}>
                  <span className="inline-flex items-center gap-1"><SvgEmoji name="trophy" /> {currentScore} pts</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* PANEL DE CONTROL DE PUNTOS DEL PERFIL SELECCIONADO (4 BOTONES DE SCORE) */}
      {activeProfile && (
        <div className="mb-8 bg-[#121624] border-2 border-indigo-500/40 p-5 rounded-3xl shadow-2xl animate-fadeIn relative overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-r from-indigo-500/5 via-purple-500/5 to-transparent pointer-events-none" />

          <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-4 border-b border-slate-800/80 pb-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-[#0f121d] border border-indigo-500/40 overflow-hidden shrink-0 shadow-md">
                {activeProfile.avatar ? (
                  <img src={activeProfile.avatar} alt={activeProfile.name} className="w-full h-full object-cover" />
                ) : (
                  <span className="w-full h-full flex items-center justify-center text-xl text-slate-400 font-bold"><SvgEmoji name="user" /></span>
                )}
              </div>
              <div>
                <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">Selected Competitor</span>
                <h4 className="text-lg font-black text-slate-100 flex items-center gap-2">
                  <span>{activeProfile.name}</span>
                  <span className="text-xs font-mono font-bold text-amber-400 bg-amber-500/10 px-2.5 py-0.5 rounded-md border border-amber-500/20">
                    Current Score: {scores[activeProfile.id] || 0} pts
                  </span>
                </h4>
              </div>
            </div>

            <button
              onClick={() => handleResetScore(activeProfile.id)}
              className="text-[11px] font-bold text-slate-400 hover:text-rose-400 uppercase tracking-wider bg-slate-800/80 hover:bg-rose-500/10 px-3.5 py-2 rounded-xl border border-slate-700 transition-colors"
            >
              Reset Points (0)
            </button>
          </div>

          {/* LOS 4 BOTONES DE PUNTOS */}
          <div>
            <span className="block text-[10px] font-black text-indigo-300 uppercase tracking-widest mb-3">
              <span className="inline-flex items-center gap-1.5"><SvgEmoji name="lightning" /> Quick Score Adjuster (4 Score Action Buttons)</span>
            </span>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {fourScoreButtons.map((btn, idx) => {
                const valNum = parseInt(btn.value) || 0;
                const isPositive = valNum >= 0;
                const formattedLabel = isPositive ? `+${valNum}` : `${valNum}`;

                return (
                  <button
                    key={btn.id || idx}
                    onClick={() => handleAdjustScore(activeProfile.id, btn.value)}
                    className={`py-3.5 px-4 rounded-2xl font-black text-base md:text-lg border shadow-lg transition-all duration-150 active:scale-95 flex flex-col items-center justify-center gap-0.5 ${
                      isPositive
                        ? 'bg-gradient-to-b from-emerald-600 to-teal-700 hover:from-emerald-500 hover:to-teal-600 border-emerald-400/50 text-white shadow-emerald-900/30'
                        : 'bg-gradient-to-b from-rose-600 to-red-700 hover:from-rose-500 hover:to-red-600 border-rose-400/50 text-white shadow-rose-900/30'
                    }`}
                  >
                    <span>{formattedLabel} pts</span>
                    <span className="text-[9px] font-bold opacity-80 uppercase tracking-wider">
                      {isPositive ? 'Add Score' : 'Deduct Score'}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* VISTA STAGE DEL MINIJUEGO ACTIVO */}
      <div className="bg-[#0b0e17] border border-slate-900 rounded-3xl p-6 md:p-8 min-h-[400px] flex flex-col items-center justify-center text-center relative overflow-hidden">
        <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/5 via-transparent to-purple-500/5 pointer-events-none" />

        {/* BARRA NAVEGACIÓN ENTRE MINIJUEGOS (SIEMPRE VISIBLE) */}
        {selectedGames.length > 0 && (
          <div className="w-full flex flex-col sm:flex-row items-center justify-between gap-4 mb-6 pb-4 border-b border-slate-800/80 z-10">
            <button
              disabled={currentGameIndex === 0}
              onClick={handlePrevGame}
              className="bg-slate-800/90 hover:bg-slate-700 disabled:opacity-30 disabled:cursor-not-allowed text-white text-xs font-bold px-4 py-2 rounded-xl uppercase tracking-wider transition-all border border-slate-700 flex items-center gap-1.5 cursor-pointer"
            >
              ← Previous Game
            </button>

            {/* SELECTOR DE PÍLDORAS DIRECTAS POR JUEGO */}
            <div className="flex flex-wrap items-center justify-center gap-2">
              {selectedGames.map((g, idx) => (
                <button
                  key={idx}
                  onClick={() => setCurrentGameIndex(idx)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold uppercase tracking-wider transition-all border cursor-pointer ${
                    idx === currentGameIndex
                      ? 'bg-indigo-600 border-indigo-400 text-white shadow-md ring-2 ring-indigo-400/40'
                      : 'bg-slate-800/60 hover:bg-slate-800 border-slate-700/60 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {idx + 1}. {g.name}
                </button>
              ))}
            </div>

            <button
              disabled={currentGameIndex === selectedGames.length - 1}
              onClick={handleNextGame}
              className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-30 disabled:cursor-not-allowed text-white text-xs font-bold px-4 py-2 rounded-xl uppercase tracking-wider transition-all shadow-md flex items-center gap-1.5 cursor-pointer"
            >
              Next Game →
            </button>
          </div>
        )}

        {/* CONTENIDO DEL MINIJUEGO ACTIVO (CONECTADO VÍA DRIVER MODULAR) */}
        <div className="w-full z-10">
          <GameArenaDriver
            activeGame={activeGame}
            profiles={profiles}
            matchData={matchData}
            onSelectWinner={(winnerComp) => {
              if (winnerComp && winnerComp.id) {
                setSelectedProfileId(winnerComp.id);
              }
            }}
          />
        </div>
      </div>

      {/* CONFIRM END MATCH MODAL OVERLAY */}
      {showEndMatchModal && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn">
          <div className="bg-[#141929] border-2 border-indigo-500/50 rounded-3xl p-6 w-[350px] h-[350px] shadow-2xl flex flex-col justify-between items-center text-center">
            <div className="w-16 h-16 rounded-2xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-3xl shadow-inner mt-2">
              <SvgEmoji name="trophy" size={32} />
            </div>
            <div>
              <h3 className="text-lg font-black text-slate-100">End Match?</h3>
              <p className="text-xs text-slate-400 mt-2 leading-relaxed px-2">
                Are you sure you want to finish the match and declare the final champion?
              </p>
            </div>
            <div className="flex gap-3 justify-center w-full mb-1">
              <button
                onClick={() => setShowEndMatchModal(false)}
                className="flex-1 py-2.5 px-3 rounded-xl text-xs font-bold text-slate-300 bg-slate-800 hover:bg-slate-700 border border-slate-700 transition-colors uppercase tracking-wider cursor-pointer"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  setShowEndMatchModal(false);
                  handleEndMatch();
                }}
                className="flex-1 py-2.5 px-3 rounded-xl text-xs font-black text-white bg-indigo-600 hover:bg-indigo-500 transition-all uppercase tracking-wider border border-indigo-400 shadow-md cursor-pointer"
              >
                Yes, End Match
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL DE RULETA EN VIVO EN LA ARENA */}
      <ArenaRouletteModal
        isOpen={isRouletteModalOpen}
        onClose={() => setIsRouletteModalOpen(false)}
        profiles={profiles}
      />

    </div>
  );
}

