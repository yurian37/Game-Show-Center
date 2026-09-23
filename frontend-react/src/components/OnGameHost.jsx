import React, { useState } from 'react';
import { gamesRegistry } from './games/gamesRegistry';

const DEFAULT_AVATAR = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%236366f1'%3E%3Cpath d='M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z'/%3E%3C/svg%3E";

export default function OnGameHost({ selectedGames = [], gameMode = '1vs1', initialPlayers = {}, scorePresets, onNavigate }) {
  const [activeStepIndex, setActiveStepIndex] = useState(0);
  const [completedSteps, setCompletedSteps] = useState(new Set());
  const [validationError, setValidationError] = useState('');

  // --- STATE 1: PARTICIPANT PROFILES ---
  const [profiles, setProfiles] = useState(() => {
    if (initialPlayers?.profiles && Array.isArray(initialPlayers.profiles) && initialPlayers.profiles.length > 0) {
      return initialPlayers.profiles.map((p, idx) => ({
        id: p.id || `p_${idx + 1}`,
        name: p.name || `Participant ${idx + 1}`,
        avatar: p.avatar || DEFAULT_AVATAR
      }));
    }

    if (gameMode === '1vs1') {
      return [
        { id: 'p1', name: initialPlayers?.player1 || 'Player 1', avatar: initialPlayers?.player1Avatar || DEFAULT_AVATAR },
        { id: 'p2', name: initialPlayers?.player2 || 'Player 2', avatar: initialPlayers?.player2Avatar || DEFAULT_AVATAR }
      ];
    } else if (gameMode === 'team') {
      const tCount = Math.min(4, Math.max(2, initialPlayers?.teamCount || (initialPlayers?.teams ? initialPlayers.teams.filter(Boolean).length : 2)));
      const defaultTeamNames = ['Team Red', 'Team Blue', 'Team Green', 'Team Gold'];
      return Array.from({ length: tCount }).map((_, idx) => ({
        id: `t${idx + 1}`,
        name: initialPlayers?.teams?.[idx] || (idx === 0 ? initialPlayers?.team1 : idx === 1 ? initialPlayers?.team2 : null) || defaultTeamNames[idx % defaultTeamNames.length],
        avatar: initialPlayers?.teamAvatars?.[idx] || DEFAULT_AVATAR
      }));
    } else {
      const count = Math.min(8, Math.max(3, initialPlayers?.ffaPlayerCount || 3));
      return Array.from({ length: count }).map((_, idx) => ({
        id: `ffa_${idx}`,
        name: initialPlayers?.ffaPlayers?.[idx] || `Player ${idx + 1}`,
        avatar: initialPlayers?.ffaAvatars?.[idx] || DEFAULT_AVATAR
      }));
    }
  });

  // --- STATE 2: DYNAMIC MINIGAME SETUPS ---
  const [gamesSetup, setGamesSetup] = useState(() => {
    const setup = {};
    selectedGames.forEach((game) => {
      const defaultData = gamesRegistry[game.name]?.defaultSetup;
      setup[game.name] = defaultData ? JSON.parse(JSON.stringify(defaultData)) : { game: game.name };
    });
    return setup;
  });

  // Steps list: 0 -> Profiles, 1..N -> Selected Games, N+1 -> Final Confirm
  const steps = [
    { type: 'profiles', title: '👤 Players & Teams' },
    ...selectedGames.map(g => ({ type: 'game', name: g.name, game: g, title: `🧩 ${g.name}` })),
    { type: 'confirm', title: '🎮 Confirm & Launch' }
  ];

  // --- PROFILE HANDLERS ---
  const handleProfileNameChange = (id, newName) => {
    setProfiles(profiles.map(p => p.id === id ? { ...p, name: newName } : p));
  };

  const handleAvatarChange = (id, e) => {
    const file = e.target.files[0];
    if (file) {
      const imageUrl = URL.createObjectURL(file);
      setProfiles(profiles.map(p => p.id === id ? { ...p, avatar: imageUrl } : p));
    }
  };

  // --- CLIENT VALIDATION RULES ---
  const validateProfiles = () => {
    for (const p of profiles) {
      if (!p.name || p.name.trim() === '') {
        return { valid: false, message: 'All participants must have a valid name.' };
      }
      // Avatars are optional; fallback to default avatar if none selected (BUG-10)
    }
    return { valid: true };
  };

  const validateGameSetup = (game, setupData, numPlayers) => {
    const name = game.name;
    const serializeFn = gamesRegistry[name]?.serialize;
    const data = serializeFn ? serializeFn(setupData) : setupData;

    if (name === 'Roulette' || name === 'Flip Coin') {
      if (!data.weights || Object.keys(data.weights).length === 0) {
        return { valid: false, message: 'Roulette: Player weights cannot be empty.' };
      }
    } else if (name === 'Hangman') {
      const required = numPlayers * (data.roundsPerPlayer || 3);
      const wordPool = data.wordPool || [];
      if (wordPool.length < required) {
        return { valid: false, message: `Hangman: At least ${required} words are required (${numPlayers} players × ${data.roundsPerPlayer || 3} rounds), but only ${wordPool.length} provided.` };
      }
    } else if (name === 'TicTacToe') {
      const team = data.startingTeam;
      if (!team || (!['red', 'blue', 'random'].includes(team))) {
        return { valid: false, message: "TicTacToe: Invalid starting team. Must be 'red', 'blue', or 'random'." };
      }
    } else if (name === 'Trivia Quiz') {
      const required = numPlayers * (data.roundsPerPlayer || 3);
      const pool = data.questionPool || [];
      if (pool.length < required) {
        return { valid: false, message: `Trivia Quiz: At least ${required} questions are required in the pool, but only ${pool.length} provided.` };
      }
    } else if (name === 'Zero Margin') {
      const pool = data.targetTimesPool || [];
      if (pool.length === 0) {
        return { valid: false, message: 'Zero Margin: Target times pool cannot be empty.' };
      }
    } else if (name === 'GeoLocation') {
      const reqLocs = numPlayers * (data.roundsPerPlayer || 2);
      const locs = data.locations || [];
      if (locs.length < reqLocs) {
        return { valid: false, message: `GeoLocation: At least ${reqLocs} locations required, but only ${locs.length} provided.` };
      }
      const minImages = data.imagesPerRound || 3;
      for (const loc of locs) {
        if (!loc.images || loc.images.length < minImages) {
          return { valid: false, message: `GeoLocation: Location "${loc.locationName}" must have at least ${minImages} images (currently has ${loc.images?.length || 0}).` };
        }
      }
    } else if (name === 'Rapid Rhythm' || name === 'Rapid_Rhythm') {
      const reqTracks = numPlayers * (data.roundsPerPlayer || 1);
      const tracks = data.tracks || [];
      const pool = data.mediaPool || [];
      const trackCount = tracks.length > 0 ? tracks.length : pool.length;
      if (trackCount < reqTracks) {
        return { valid: false, message: `Rapid Rhythm: At least ${reqTracks} song tracks required in pool (${numPlayers} players × ${data.roundsPerPlayer || 1} rounds), but only ${trackCount} provided.` };
      }
      for (let i = 0; i < tracks.length; i++) {
        const t = tracks[i];
        if (!t.answer || t.answer.trim() === '') {
          return { valid: false, message: `Rapid Rhythm: Track #${i + 1} ("${t.name || 'Untitled'}") is missing a mandatory Answer.` };
        }
      }
    } else if (name === 'Guess Character') {
      const reqMedia = numPlayers * (data.roundsPerPlayer || 4);
      const pool = data.mediaPool || [];
      if (pool.length < reqMedia) {
        return { valid: false, message: `${name}: At least ${reqMedia} media files required in pool, but only ${pool.length} provided.` };
      }
    } else if (name === 'Snap Solve' || name === 'Snap_Solve' || name === 'Snap Solve (IA)') {
      const rpp = data.roundsPerPlayer || 2;
      const reqMedia = numPlayers * rpp;
      const pool = data.mediaPool || [];
      if (pool.length < reqMedia) {
        return { valid: false, message: `Snap Solve: At least ${reqMedia} images required in pool (${numPlayers} player(s) × ${rpp} round(s)), but only ${pool.length} provided.` };
      }
      const filters = data.selectedFilters;
      if (Array.isArray(filters) && filters.length === 0) {
        return { valid: false, message: 'Snap Solve: You must select at least one visual distortion filter.' };
      }
    } else if (name === 'Topic Takedown' || name === 'Topic_Takedown') {
      const numCats = data.numCategories || 3;
      const qPerCat = data.questionsPerCategory || 3;
      if (numCats < 1 || numCats > 3) {
        return { valid: false, message: 'Topic Takedown: Online mode supports a maximum of 3 categories.' };
      }
      if (qPerCat < 1 || qPerCat > 4) {
        return { valid: false, message: 'Topic Takedown: Online mode supports a maximum of 4 questions per category.' };
      }
      const cats = data.categories || [];
      if (cats.length !== numCats) {
        return { valid: false, message: `Topic Takedown: Exactly ${numCats} categories must be configured (${cats.length} configured).` };
      }
      for (const cat of cats) {
        if (!cat.questions || cat.questions.length !== qPerCat) {
          return { valid: false, message: `Topic Takedown: Category "${cat.categoryName}" must have exactly ${qPerCat} questions.` };
        }
        for (let i = 0; i < cat.questions.length; i++) {
          const q = cat.questions[i];
          if (!q.question || !q.question.trim() || !q.answer || !q.answer.trim()) {
            return { valid: false, message: `Topic Takedown: Question #${i + 1} in "${cat.categoryName}" must contain text for both question and answer.` };
          }
        }
      }
    }

    return { valid: true };
  };

  // --- SAVE STEP 0: PROFILES ---
  const handleSaveProfilesStep = () => {
    const result = validateProfiles();
    if (!result.valid) {
      setValidationError(result.message);
      return;
    }
    setValidationError('');
    setCompletedSteps(prev => new Set([...prev, 0]));
    setActiveStepIndex(1);
  };

  // --- SAVE STEPS 1..N: GAME SETUP ---
  const handleSaveGameStep = (gameIndex) => {
    const game = selectedGames[gameIndex];
    const setupData = gamesSetup[game.name];
    const result = validateGameSetup(game, setupData, profiles.length);

    if (!result.valid) {
      setValidationError(result.message);
      return;
    }

    setValidationError('');
    const stepIdx = gameIndex + 1;
    setCompletedSteps(prev => new Set([...prev, stepIdx]));
    setActiveStepIndex(stepIdx + 1);
  };

  // --- STEP BY STEP NAVIGATION CONTROL ---
  const canAccessStep = (stepIdx) => {
    if (stepIdx === 0) return true;
    return completedSteps.has(stepIdx - 1);
  };

  const handleStepClick = (stepIdx) => {
    if (canAccessStep(stepIdx)) {
      setValidationError('');
      setActiveStepIndex(stepIdx);
    }
  };

  // --- FINAL ACTION: CONFIRM AND TRANSITION TO ARENA ---
  const handleSaveAllJson = async () => {
    const sanitizedProfiles = profiles.map(p => ({
      ...p,
      avatar: p.avatar || DEFAULT_AVATAR
    }));

    const masterPackage = {
      gameMode: gameMode,
      numPlayers: sanitizedProfiles.length,
      setups: selectedGames.map(game => {
        const setupData = gamesSetup[game.name];
        const serializeFn = gamesRegistry[game.name]?.serialize;
        return serializeFn ? serializeFn(setupData) : { game: game.name, ...setupData };
      })
    };

    console.log("Submitting Master Match Request to Java Backend:", masterPackage);

    try {
      const response = await fetch('http://localhost:8080/api/match/compile', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(masterPackage)
      });

      const responseText = await response.text();
      let responseData;
      try { responseData = JSON.parse(responseText); } catch { responseData = responseText; }

      if (!response.ok) {
        setValidationError(`❌ SERVER VALIDATION ERROR:\n${responseData.detail || responseData.detalle || 'Validation failed.'}`);
        return;
      }

      // Build updated selectedGames array containing the customized setupData
      const updatedSelectedGames = selectedGames.map(game => {
        const setupData = gamesSetup[game.name];
        const serializeFn = gamesRegistry[game.name]?.serialize;
        const serializedSetup = serializeFn ? serializeFn(setupData) : setupData;
        return {
          ...game,
          setupData: setupData,
          setup: setupData || game.setup,
          serializedSetup: serializedSetup
        };
      });

      // HTTP 200 OK: Launch central Arena Stage with profiles and game data!
      onNavigate('arena', {
        gameMode,
        selectedGames: updatedSelectedGames,
        profiles: sanitizedProfiles,
        gamesSetup,
        scorePresets: scorePresets || initialPlayers?.scorePresets,
        responseData
      });
      
    } catch (error) {
      console.error("Error submitting match package:", error);
      setValidationError(`⚠️ ERROR: ${error.message || 'Could not connect to Java server on port 8080.'}`);
    }
  };

  return (
    <div className="w-full max-w-5xl bg-[#141929] border border-slate-800/60 rounded-[2.5rem] p-6 md:p-10 shadow-[0_30px_70px_-15px_rgba(0,0,0,0.8)] flex flex-col animate-fadeIn text-slate-200">
      
      {/* HEADER */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8 gap-4 border-b border-slate-800/60 pb-6">
        <div>
          <span className="text-[10px] font-black tracking-widest text-emerald-400 uppercase bg-emerald-500/10 px-2.5 py-1 rounded-md border border-emerald-500/20">
            Setup Wizard
          </span>
          <h2 className="text-2xl md:text-3xl font-black tracking-tight mt-2">Match Preparation</h2>
        </div>
        <button 
          onClick={() => onNavigate('settings')}
          className="text-xs font-bold text-slate-400 hover:text-rose-400 flex items-center gap-1 uppercase tracking-wider transition-colors bg-slate-800/40 px-4 py-2 rounded-xl border border-slate-800 cursor-pointer"
        >
          ✕ Exit to Settings
        </button>
      </div>

      {/* STEP PROGRESS WIZARD NAVIGATION */}
      <div className="flex flex-wrap gap-2 mb-6 bg-[#0b0e17] p-2 rounded-2xl border border-slate-900">
        {steps.map((step, idx) => {
          const isCurrent = activeStepIndex === idx;
          const isCompleted = completedSteps.has(idx);
          const isAccessible = canAccessStep(idx);

          return (
            <button
              key={idx}
              onClick={() => handleStepClick(idx)}
              disabled={!isAccessible}
              className={`py-2.5 px-4 text-xs font-black uppercase tracking-wider rounded-xl transition-all flex items-center gap-2 cursor-pointer ${
                isCurrent 
                  ? 'bg-indigo-600 text-white shadow-md ring-2 ring-indigo-400/40' 
                  : isCompleted 
                    ? 'bg-slate-800/80 text-emerald-400 border border-emerald-500/30 hover:bg-slate-800' 
                    : isAccessible
                      ? 'text-slate-300 hover:bg-slate-800/40'
                      : 'text-slate-600 opacity-40 cursor-not-allowed'
              }`}
            >
              <span>{isCompleted ? '✓' : idx + 1}.</span>
              <span>{step.title}</span>
            </button>
          );
        })}
      </div>

      {/* VALIDATION ERROR ALERT */}
      {validationError && (
        <div className="mb-6 p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-bold flex justify-between items-center animate-fadeIn">
          <span>⚠️ {validationError}</span>
          <button onClick={() => setValidationError('')} className="text-rose-300 hover:text-white font-black text-sm ml-4 cursor-pointer">✕</button>
        </div>
      )}

      {/* --- STEP CONTENT --- */}
      <div className="flex-1 min-h-[400px]">
        
        {/* STEP 0: PROFILES SETUP */}
        {activeStepIndex === 0 && (
          <div className="space-y-6 animate-fadeIn">
            <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800">
              <h3 className="text-sm font-black text-indigo-300 uppercase tracking-widest mb-2">Step 1: Players & Teams</h3>
              <p className="text-slate-400 text-xs mb-6">Choose a name and an avatar for each participant before entering the arena.</p>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                {profiles.map((profile) => (
                  <div key={profile.id} className="bg-[#121624] p-4 rounded-xl border border-slate-800/80 flex items-center gap-4">
                    <label className="w-16 h-16 shrink-0 rounded-xl bg-[#0f121d] border-2 border-dashed border-slate-700 hover:border-indigo-500 transition-colors flex flex-col items-center justify-center cursor-pointer overflow-hidden relative group" title="Click to choose an image or avatar">
                      {profile.avatar ? (
                        <img src={profile.avatar} alt="Avatar" className="w-full h-full object-cover" />
                      ) : (
                        <span className="text-xl text-slate-500 group-hover:text-indigo-400">+</span>
                      )}
                      <input type="file" accept="image/*" onChange={(e) => handleAvatarChange(profile.id, e)} className="hidden" />
                    </label>

                    <div className="flex-1">
                      <label className="block text-[10px] font-bold text-slate-500 uppercase mb-1">Participant: {profile.id}</label>
                      <input 
                        type="text" 
                        value={profile.name} 
                        onChange={(e) => handleProfileNameChange(profile.id, e.target.value)}
                        className="w-full bg-[#0f121d] border border-slate-800 rounded-lg px-3 py-2 text-sm font-semibold focus:outline-none focus:border-indigo-500 text-slate-200"
                        placeholder="Enter name..."
                      />
                    </div>
                  </div>
                ))}
              </div>

              <div className="border-t border-slate-800 pt-4 flex justify-end">
                <button
                  onClick={handleSaveProfilesStep}
                  className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-3 px-6 rounded-xl text-xs uppercase tracking-wider transition-colors shadow-lg flex items-center gap-2 cursor-pointer"
                >
                  Save Participants & Continue →
                </button>
              </div>
            </div>
          </div>
        )}

        {/* STEPS 1..N: MINIGAME SETUPS */}
        {selectedGames.map((game, gameIdx) => {
          const stepIdx = gameIdx + 1;
          if (activeStepIndex !== stepIdx) return null;

          const GameSetupComponent = gamesRegistry[game.name]?.Component;

          return (
            <div key={game.name} className="space-y-6 animate-fadeIn">
              <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 space-y-6">
                <div className="flex justify-between items-center border-b border-slate-800 pb-3">
                  <h3 className="text-sm font-black text-purple-300 uppercase tracking-widest">
                    Step {stepIdx + 1}: {game.name} Settings
                  </h3>
                  <span className="text-xs text-slate-400">Game {gameIdx + 1} of {selectedGames.length}</span>
                </div>

                {GameSetupComponent ? (
                  <GameSetupComponent
                    value={gamesSetup[game.name]}
                    onChange={(newSetup) => {
                      setGamesSetup(prev => ({
                        ...prev,
                        [game.name]: newSetup
                      }));
                    }}
                    numPlayers={profiles.length}
                    players={profiles}
                  />
                ) : (
                  <div className="p-4 text-slate-400 text-xs">No settings panel found for {game.name}.</div>
                )}

                <div className="border-t border-slate-800 pt-4 flex justify-between items-center">
                  <button
                    onClick={() => setActiveStepIndex(stepIdx - 1)}
                    className="text-xs font-bold text-slate-400 hover:text-slate-200 uppercase tracking-wider cursor-pointer"
                  >
                    ← Back
                  </button>
                  <button
                    onClick={() => handleSaveGameStep(gameIdx)}
                    className="bg-purple-600 hover:bg-purple-500 text-white font-bold py-3 px-6 rounded-xl text-xs uppercase tracking-wider transition-colors shadow-lg flex items-center gap-2 cursor-pointer"
                  >
                    Save & Confirm {game.name} →
                  </button>
                </div>
              </div>
            </div>
          );
        })}

        {/* FINAL STEP: SUMMARY AND ARENA LAUNCH */}
        {activeStepIndex === steps.length - 1 && (
          <div className="space-y-6 animate-fadeIn">
            <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 text-center max-w-2xl mx-auto my-4">
              <div className="text-4xl mb-4">🚀</div>
              <h3 className="text-xl font-black text-emerald-400 uppercase tracking-wide">All Set for the Big Match!</h3>
              <p className="text-slate-400 text-xs mt-2 leading-relaxed">
                All participants and games have been configured successfully. Click below to launch the match in the main arena.
              </p>

              <div className="my-6 border-t border-b border-slate-800 py-4 text-left space-y-2 text-xs">
                <div className="flex justify-between"><span className="text-slate-500 font-bold">Game Mode:</span> <span className="font-mono uppercase">{gameMode}</span></div>
                <div className="flex justify-between"><span className="text-slate-500 font-bold">Participants Ready:</span> <span>{profiles.length}</span></div>
                <div className="flex justify-between"><span className="text-slate-500 font-bold">Selected Minigames:</span> <span className="text-purple-400 font-bold">{selectedGames.length}</span></div>
              </div>

              <div className="flex justify-between items-center pt-2">
                <button
                  onClick={() => setActiveStepIndex(steps.length - 2)}
                  className="text-xs font-bold text-slate-400 hover:text-slate-200 uppercase tracking-wider cursor-pointer"
                >
                  ← Back to Edit
                </button>
                <button
                  onClick={handleSaveAllJson}
                  className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-black text-sm py-4 px-8 rounded-xl shadow-lg transform active:scale-[0.99] transition-all uppercase tracking-wider cursor-pointer"
                >
                  Enter Arena & Play! 🎮
                </button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}