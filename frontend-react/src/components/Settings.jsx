import React, { useState } from 'react';
// Dynamic games list from gamesRegistry
import { gamesList as allGames } from '../components/games/gamesRegistry';
import PaymentModal from './PaymentModal';

export default function Settings({ onNavigate }) {
  const [activeTab, setActiveTab] = useState('basic');
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false);
  const [gameMode, setGameMode] = useState('');
  const [player1, setPlayer1] = useState('');
  const [player2, setPlayer2] = useState('');
  const [teamCount, setTeamCount] = useState(2);
  const [teams, setTeams] = useState(Array(4).fill(''));
  const [ffaPlayerCount, setFfaPlayerCount] = useState(3);
  const [ffaPlayers, setFfaPlayers] = useState(Array(8).fill(''));

  const [scorePresets, setScorePresets] = useState([
    { id: 1, value: '10' },
    { id: 2, value: '20' },
    { id: 3, value: '40' },
    { id: 4, value: '-10' }
  ]);

  // Modular Playlist State for selected games
  const [selectedGames, setSelectedGames] = useState([]);

  const handleTeamNameChange = (index, value) => {
    const updated = [...teams];
    updated[index] = value;
    setTeams(updated);
  };

  const handleFfaPlayerNameChange = (index, value) => {
    const updatedPlayers = [...ffaPlayers];
    updatedPlayers[index] = value;
    setFfaPlayers(updatedPlayers);
  };

  const handleScoreValueChange = (id, newValue) => {
    setScorePresets(scorePresets.map(preset => 
      preset.id === id ? { ...preset, value: newValue } : preset
    ));
  };

  const handleAddScorePreset = () => {
    if (scorePresets.length >= 8) {
      alert("Maximum limit reached. You can only have up to 8 score presets.");
      return;
    }
    const nextId = scorePresets.length > 0 ? Math.max(...scorePresets.map(p => p.id)) + 1 : 1;
    setScorePresets([...scorePresets, { id: nextId, value: '0' }]);
  };

  const handleRemoveScorePreset = (id) => {
    if (scorePresets.length <= 2) {
      alert("Minimum limit reached. You must keep at least 2 score presets.");
      return;
    }
    setScorePresets(scorePresets.filter(preset => preset.id !== id));
  };

  const isGameAvailableInMode = (game, currentMode) => {
    if (!currentMode) return true;
    if (game?.available !== undefined && Array.isArray(game.available)) {
      if (game.available.length === 0) return false;
      const normalizedAvailable = game.available.map(m =>
        m.toLowerCase().replace(/ /g, '').replace(/teams?/, 'team')
      );
      const normalizedCurrentMode = currentMode.toLowerCase().replace(/ /g, '').replace(/teams?/, 'team');
      return normalizedAvailable.includes(normalizedCurrentMode);
    }
    return true;
  };

  const handleGameModeChange = (newMode) => {
    setGameMode(newMode);
    setSelectedGames(prevSelected =>
      prevSelected.filter(game => isGameAvailableInMode(game, newMode))
    );
  };

  const handleGameSelect = (game) => {
    if (gameMode && !isGameAvailableInMode(game, gameMode)) {
      return;
    }

    const isAlreadySelected = selectedGames.some(g => g.name === game.name);

    if (isAlreadySelected) {
      setSelectedGames(selectedGames.filter(g => g.name !== game.name));
    } else {
      if (game.plan === 'premium') {
        const hasPremiumSelected = selectedGames.some(g => g.plan === 'premium');
        if (hasPremiumSelected) {
          alert("Heads up! With the Basic plan, you can include a maximum of one Premium game in your activity. Get the Premium plan to unlock them all.");
          return;
        }
      }
      setSelectedGames([...selectedGames, game]);
    }
  };

  const handleReset = () => {
    setGameMode('');
    setPlayer1('');
    setPlayer2('');
    setTeamCount(2);
    setTeams(Array(4).fill(''));
    setFfaPlayerCount(3);
    setFfaPlayers(Array(8).fill(''));
    setSelectedGames([]);
    setScorePresets([
      { id: 1, value: '10' },
      { id: 2, value: '20' },
      { id: 3, value: '40' },
      { id: 4, value: '-10' }
    ]);
  };

  const handleStartGame = () => {
    if (!gameMode) {
      alert("Please select a game mode before starting.");
      return;
    }
    if (selectedGames.length === 0) {
      alert("Please select at least one mini-game for your matchup loop.");
      return;
    }
    
    const matchConfig = {
      gameMode,
      selectedGames,
      scorePresets,
      initialPlayers: {
        player1,
        player2,
        teamCount,
        teams,
        team1: teams[0] || '',
        team2: teams[1] || '',
        ffaPlayerCount,
        ffaPlayers
      }
    };

    onNavigate('host', matchConfig);
  };

  const handleExportSetupJson = () => {
    let exportProfiles = [];
    if (gameMode === '1vs1') {
      exportProfiles = [
        { id: 'p1', name: player1 || 'Player 1' },
        { id: 'p2', name: player2 || 'Player 2' }
      ];
    } else if (gameMode === 'team') {
      exportProfiles = Array.from({ length: teamCount }).map((_, idx) => ({
        id: `t${idx + 1}`,
        name: teams[idx] || `Team ${idx + 1}`
      }));
    } else {
      exportProfiles = Array.from({ length: ffaPlayerCount }).map((_, idx) => ({
        id: `ffa_${idx}`,
        name: ffaPlayers[idx] || `Player ${idx + 1}`
      }));
    }

    const configData = {
      gameMode,
      selectedGames: selectedGames.map(g => ({
        name: g.name,
        plan: g.plan,
        setup: g.setup || g.setupData || {}
      })),
      scorePresets,
      initialPlayers: {
        player1,
        player2,
        teamCount,
        teams,
        team1: teams[0] || '',
        team2: teams[1] || '',
        ffaPlayerCount,
        ffaPlayers
      },
      profiles: exportProfiles
    };
    const jsonStr = JSON.stringify(configData, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'setup.json';
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImportSetupJson = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const importedData = JSON.parse(event.target.result);
        const importedMode = importedData.gameMode || '1vs1';
        if (importedData.gameMode) setGameMode(importedData.gameMode);

        if (Array.isArray(importedData.selectedGames)) {
          const resolvedGames = importedData.selectedGames.map(g => {
            const gName = typeof g === 'string' ? g : g.name;
            const found = allGames.find(item => item.name.toLowerCase() === (gName || '').toLowerCase());
            if (found) {
              return { ...found, ...(typeof g === 'object' ? g : {}) };
            }
            return typeof g === 'object' ? g : { name: gName };
          }).filter(Boolean);
          setSelectedGames(resolvedGames);
        }

        if (Array.isArray(importedData.scorePresets)) {
          const normalized = importedData.scorePresets.map((preset, idx) => {
            if (typeof preset === 'object' && preset !== null && preset.value !== undefined) {
              return { id: preset.id ?? (idx + 1), value: String(preset.value) };
            } else {
              return { id: idx + 1, value: String(preset) };
            }
          });
          setScorePresets(normalized);
        }

        if (importedData.initialPlayers) {
          if (importedData.initialPlayers.player1) setPlayer1(importedData.initialPlayers.player1);
          if (importedData.initialPlayers.player2) setPlayer2(importedData.initialPlayers.player2);
          if (importedData.initialPlayers.teamCount) setTeamCount(importedData.initialPlayers.teamCount);
          if (Array.isArray(importedData.initialPlayers.teams)) {
            setTeams(importedData.initialPlayers.teams);
          } else if (importedData.initialPlayers.team1 || importedData.initialPlayers.team2) {
            setTeams([importedData.initialPlayers.team1 || '', importedData.initialPlayers.team2 || '', '', '']);
          }
          if (importedData.initialPlayers.ffaPlayerCount) setFfaPlayerCount(importedData.initialPlayers.ffaPlayerCount);
          if (Array.isArray(importedData.initialPlayers.ffaPlayers)) setFfaPlayers(importedData.initialPlayers.ffaPlayers);
        } else if (Array.isArray(importedData.profiles) && importedData.profiles.length > 0) {
          const profs = importedData.profiles;
          if (importedMode === '1vs1') {
            setPlayer1(profs[0]?.name || 'Player 1');
            setPlayer2(profs[1]?.name || 'Player 2');
          } else if (importedMode === 'team') {
            const count = Math.min(4, Math.max(2, profs.length));
            setTeamCount(count);
            const teamArr = Array(4).fill('');
            profs.slice(0, count).forEach((p, i) => { teamArr[i] = p.name || ''; });
            setTeams(teamArr);
          } else {
            const count = Math.min(8, Math.max(3, profs.length));
            setFfaPlayerCount(count);
            const ffaArr = Array(8).fill('');
            profs.slice(0, count).forEach((p, i) => { ffaArr[i] = p.name || ''; });
            setFfaPlayers(ffaArr);
          }
        }

        alert("✅ Match setup loaded successfully!");
      } catch (err) {
        alert("❌ Could not read file. Make sure you select a valid match setup JSON file.");
      }
    };
    reader.readAsText(file);
    e.target.value = null;
  };

  return (
    <div className="w-full max-w-4xl bg-[#141929] border border-slate-800/60 rounded-[2.5rem] p-6 md:p-10 shadow-[0_30px_70px_-15px_rgba(0,0,0,0.8)] flex flex-col animate-fadeIn text-slate-200">
      
      <button 
        onClick={() => onNavigate('home')}
        className="text-xs font-bold text-slate-400 hover:text-indigo-400 self-start mb-6 flex items-center gap-1 uppercase tracking-wider transition-colors cursor-pointer"
      >
        ← Back to Home
      </button>

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <div>
          <h2 className="text-2xl md:text-3xl font-black tracking-tight">Match Settings Panel</h2>
          <p className="text-xs text-slate-400 mt-1">Organize participants, mini-games, and score buttons for your show.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <label className="text-xs font-bold bg-slate-800 hover:bg-purple-900/40 text-purple-300 px-3.5 py-2 rounded-xl border border-purple-700/60 transition-all uppercase tracking-wider cursor-pointer flex items-center gap-1.5 shadow-sm" title="Load a previously saved match from a file on your device">
            📥 Load Saved Match
            <input type="file" accept=".json" onChange={handleImportSetupJson} className="hidden" />
          </label>
          <button
            onClick={handleExportSetupJson}
            title="Save participants and settings to a file to reuse them later"
            className="text-xs font-bold bg-slate-800 hover:bg-indigo-900/40 text-indigo-300 px-3.5 py-2 rounded-xl border border-indigo-700/60 transition-all uppercase tracking-wider flex items-center gap-1.5 cursor-pointer shadow-sm"
          >
            💾 Save Match
          </button>
          <button
            onClick={handleReset}
            title="Reset all fields and selections to their default values"
            className="text-xs font-bold bg-slate-800 hover:bg-slate-700 text-slate-300 px-3.5 py-2 rounded-xl border border-slate-700 transition-all uppercase tracking-wider cursor-pointer"
          >
            Reset Settings
          </button>
        </div>
      </div>

      {/* TAB NAVIGATION */}
      <div className="flex border-b border-slate-800 mb-8 gap-2">
        <button
          onClick={() => setActiveTab('basic')}
          className={`py-3 px-6 text-sm font-black uppercase tracking-wider border-b-2 transition-all cursor-pointer ${
            activeTab === 'basic' 
              ? 'border-indigo-500 text-indigo-400 bg-indigo-500/5 rounded-t-xl' 
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Basic Plan (Free)
        </button>
        <button
          onClick={() => setActiveTab('premium')}
          className={`py-3 px-6 text-sm font-black uppercase tracking-wider border-b-2 transition-all cursor-pointer ${
            activeTab === 'premium' 
              ? 'border-purple-500 text-purple-400 bg-purple-500/5 rounded-t-xl' 
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Premium Plan
        </button>
      </div>

      {/* TAB 1 CONTENT: BASIC PLAN */}
      {activeTab === 'basic' && (
        <div className="space-y-8">
          
          {/* A) Game Mode Selection */}
          <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800">
            <label className="block text-xs font-black tracking-widest text-indigo-300 uppercase mb-1">A) Game Mode</label>
            <p className="text-slate-400 text-xs mb-4">Choose how participants will compete in this match.</p>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {[
                { label: '1 vs 1', value: '1vs1', hint: 'Direct duel between 2 players' },
                { label: 'Team Battle', value: 'team', hint: '2 to 4 teams or groups' },
                { label: 'Free For All', value: 'freeforall', hint: 'Open individual battle (3-8)' }
              ].map(({ label, value, hint }) => (
                <button
                  key={value}
                  onClick={() => handleGameModeChange(value)}
                  title={hint}
                  className={`py-4 px-4 rounded-xl border font-bold transition-all text-center flex flex-col items-center justify-center cursor-pointer ${
                    gameMode === value
                      ? 'bg-indigo-600 border-indigo-400 text-white shadow-lg'
                      : 'bg-[#121624] border-slate-800 text-slate-300 hover:border-slate-700'
                  }`}
                >
                  <span className="text-sm">{label}</span>
                  <span className="text-[10px] opacity-75 font-normal mt-1">{hint}</span>
                </button>
              ))}
            </div>

            {gameMode && <div className="mt-6 pt-6 border-t border-slate-800/60">
              {gameMode === '1vs1' && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-[11px] font-bold text-slate-400 uppercase mb-2">Player 1 Name</label>
                    <input type="text" value={player1} onChange={(e) => setPlayer1(e.target.value)} placeholder="e.g. Alex" className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-500 text-slate-200" />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-slate-400 uppercase mb-2">Player 2 Name</label>
                    <input type="text" value={player2} onChange={(e) => setPlayer2(e.target.value)} placeholder="e.g. Sam" className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-500 text-slate-200" />
                  </div>
                </div>
              )}

              {gameMode === 'team' && (
                <div className="space-y-4">
                  <div className="w-48">
                    <label className="block text-[11px] font-bold text-slate-400 uppercase mb-2">Number of Teams</label>
                    <select value={teamCount} onChange={(e) => setTeamCount(Number(e.target.value))} className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-500 text-slate-200 cursor-pointer">
                      {[2, 3, 4].map(num => <option key={num} value={num}>{num} Teams</option>)}
                    </select>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
                    {Array.from({ length: teamCount }).map((_, idx) => (
                      <div key={idx}>
                        <label className="block text-[11px] font-bold text-slate-400 uppercase mb-2">Team {idx + 1} Name</label>
                        <input
                          type="text"
                          value={teams[idx] || ''}
                          onChange={(e) => handleTeamNameChange(idx, e.target.value)}
                          placeholder={`e.g. ${idx === 0 ? 'Red Tigers' : idx === 1 ? 'Blue Sharks' : idx === 2 ? 'Green Dragons' : 'Golden Phoenix'}`}
                          className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-500 text-slate-200"
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {gameMode === 'freeforall' && (
                <div className="space-y-4">
                  <div className="w-48">
                    <label className="block text-[11px] font-bold text-slate-400 uppercase mb-2">Number of Players</label>
                    <select value={ffaPlayerCount} onChange={(e) => setFfaPlayerCount(Number(e.target.value))} className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-500 text-slate-200 cursor-pointer">
                      {[3, 4, 5, 6, 7, 8].map(num => <option key={num} value={num}>{num} Players</option>)}
                    </select>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 pt-2">
                    {Array.from({ length: ffaPlayerCount }).map((_, idx) => (
                      <div key={idx}>
                        <label className="block text-[11px] font-bold text-slate-400 uppercase mb-2">Player {idx + 1}</label>
                        <input type="text" value={ffaPlayers[idx] || ''} onChange={(e) => handleFfaPlayerNameChange(idx, e.target.value)} placeholder={`e.g. Participant ${idx + 1}`} className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-indigo-500 text-slate-200" />
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>}
          </div>

          {/* B) Interlocking Matchbox Puzzle Deck */}
          <div className="bg-[#1b2238] p-6 rounded-3xl border border-slate-800 relative overflow-hidden">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-4 gap-2">
              <div>
                <label className="block text-xs font-black tracking-widest text-indigo-300 uppercase">
                  🧩 B) Mini-Games Selection
                </label>
                <p className="text-slate-400 text-xs mt-1">
                  Click the games you want to include in the order they will be played during the show.
                </p>
              </div>
              <span className="text-xs font-bold text-indigo-400 bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
                {selectedGames.length} of {allGames.length} selected
              </span>
            </div>
            
            {/* 3-COLUMN PUZZLE DECK CONTAINER */}
            <div className="grid grid-cols-1 md:grid-cols-[1fr_210px_1fr] gap-4 items-stretch pt-2">
              
              {/* LEFT COLUMN: EVEN GAMES (TAB POINTING RIGHT) */}
              <div className="space-y-3 flex flex-col justify-center">
                {allGames.filter((_, idx) => idx % 2 === 0).map((game, leftIdx) => {
                  const originalIdx = leftIdx * 2;
                  const orderIndex = selectedGames.findIndex(g => g.name === game.name);
                  const isSelected = orderIndex !== -1;
                  const isPremium = game.plan === 'premium';
                  const isAvailable = isGameAvailableInMode(game, gameMode);

                  return (
                    <div key={originalIdx} className="relative group/tooltip">
                      <button
                        onClick={() => handleGameSelect(game)}
                        disabled={!isAvailable}
                        className={`w-full p-4 rounded-2xl flex items-center justify-between text-left transition-all duration-200 border relative cursor-pointer ${
                          !isAvailable
                            ? 'bg-[#0f121d] border-slate-800/40 opacity-40 cursor-not-allowed'
                            : isSelected
                              ? isPremium
                                ? 'bg-gradient-to-r from-[#2a1a3e] to-[#3b2055] border-purple-500 shadow-[0_0_20px_rgba(168,85,247,0.3)] translate-x-2 ring-1 ring-purple-400'
                                : 'bg-gradient-to-r from-[#17203a] to-[#1e2a52] border-indigo-500 shadow-[0_0_20px_rgba(99,102,241,0.3)] translate-x-2 ring-1 ring-indigo-400'
                              : 'bg-[#121624] border-slate-800 hover:border-slate-700 hover:bg-[#161c2e]'
                        }`}
                      >
                        <div className="flex flex-col pr-4">
                          <span className={`text-sm font-black ${
                            !isAvailable
                              ? 'text-slate-500 line-through'
                              : isSelected ? (isPremium ? 'text-purple-300' : 'text-indigo-300') : 'text-slate-200'
                          }`}>
                            {game.name}
                          </span>
                          <span className="text-[10px] text-slate-500 font-bold uppercase mt-0.5">
                            {isPremium ? '⭐ Premium Game' : '🎮 Basic Game'}
                          </span>
                        </div>

                        {/* INTERLOCKING PUZZLE TAB & ORDER BADGE ON RIGHT EDGE */}
                        {isSelected && (
                          <div className="absolute -right-3.5 top-1/2 -translate-y-1/2 z-20 w-7 h-7 rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 text-white font-black text-xs flex items-center justify-center border-2 border-indigo-300 shadow-[0_0_12px_rgba(99,102,241,0.8)] animate-pulse">
                            #{orderIndex + 1}
                          </div>
                        )}
                      </button>

                      {/* TOOLTIP */}
                      <div className="absolute z-30 bottom-full left-1/2 transform -translate-x-1/2 mb-2 w-64 p-3 bg-[#0f1322] border border-slate-700 rounded-xl shadow-2xl opacity-0 pointer-events-none group-hover/tooltip:opacity-100 transition-opacity duration-200 text-xs text-slate-300 leading-relaxed backdrop-blur-md">
                        <div className="font-bold text-indigo-400 mb-1">{game.name}</div>
                        {game.description}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* CENTER COLUMN: STATIC CONTROL SPINE HUB WITH LED STATUS LIGHTS */}
              <div className="bg-[#121624] border-2 border-indigo-500/40 rounded-3xl p-5 shadow-2xl flex flex-col items-center justify-between text-center min-h-[300px] relative overflow-hidden my-2 md:my-0">
                <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/5 via-purple-500/5 to-transparent pointer-events-none" />

                {/* SPINE HEADER */}
                <div className="z-10 w-full pb-3 border-b border-slate-800/80">
                  <div className="flex items-center justify-center gap-1.5 mb-1">
                    <span className="w-2 h-2 rounded-full bg-indigo-500 animate-ping" />
                    <span className="text-[10px] font-black tracking-widest text-indigo-400 uppercase">
                      SPINE CONTROL
                    </span>
                  </div>
                  <h4 className="text-xs font-black text-slate-200 uppercase tracking-tight">
                    MATCH SUMMARY
                  </h4>
                </div>

                {/* CENTRAL LED STATUS INDICATOR LIGHTS */}
                <div className="z-10 w-full space-y-4 py-3">
                  
                  {/* LED 1: POWER & READINESS LIGHT */}
                  <div className="bg-[#0b0e17] border border-slate-800 p-2.5 rounded-2xl flex items-center justify-between px-3">
                    <span className="text-[10px] font-bold text-slate-400 uppercase">STATUS</span>
                    <div className="flex items-center gap-1.5">
                      <span className={`w-3 h-3 rounded-full border shadow-md transition-all ${
                        selectedGames.length > 0
                          ? 'bg-emerald-500 border-emerald-300 shadow-emerald-500/50 animate-pulse'
                          : 'bg-amber-500 border-amber-300 shadow-amber-500/50'
                      }`} />
                      <span className={`text-[10px] font-black uppercase ${
                        selectedGames.length > 0 ? 'text-emerald-400' : 'text-amber-400'
                      }`}>
                        {selectedGames.length > 0 ? 'READY TO PLAY!' : 'SELECT GAMES'}
                      </span>
                    </div>
                  </div>

                  {/* LED 2: INTERLOCKED BLOCKS COUNTER */}
                  <div className="bg-[#0b0e17] border border-slate-800 p-2.5 rounded-2xl flex items-center justify-between px-3">
                    <span className="text-[10px] font-bold text-slate-400 uppercase">SELECTED</span>
                    <span className="text-xs font-black font-mono text-purple-400 bg-purple-500/10 px-2 py-0.5 rounded-md border border-purple-500/20">
                      {selectedGames.length} / {allGames.length}
                    </span>
                  </div>

                  {/* LED 3: ENERGY METER BAR */}
                  <div className="bg-[#0b0e17] border border-slate-800 p-2.5 rounded-2xl text-left space-y-1.5">
                    <div className="flex justify-between items-center text-[9px] font-black uppercase tracking-wider text-indigo-300">
                      <span>ACTIVE GAMES</span>
                      <span>{Math.round((selectedGames.length / Math.max(1, allGames.length)) * 100)}%</span>
                    </div>
                    <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden p-0.5 border border-slate-700">
                      <div 
                        className="bg-gradient-to-r from-indigo-500 via-purple-500 to-amber-400 h-full rounded-full transition-all duration-300"
                        style={{ width: `${Math.round((selectedGames.length / Math.max(1, allGames.length)) * 100)}%` }}
                      />
                    </div>
                  </div>

                </div>

                {/* SPINE FOOTER STATUS BADGE & ACTION CONTROLS */}
                <div className="z-10 w-full pt-3 border-t border-slate-800/80 space-y-3">
                  <span className="text-[9px] font-mono text-slate-500 uppercase tracking-widest block">
                    {selectedGames.length > 0 ? '⚡ READY TO BEGIN' : '⏳ SELECT AT LEAST ONE GAME'}
                  </span>

                  {/* MATCH CENTRAL DECK ACTION BUTTONS */}
                  <div className="space-y-2 pt-1">
                    <button
                      type="button"
                      onClick={handleStartGame}
                      className="w-full bg-gradient-to-r from-emerald-500 via-teal-500 to-emerald-600 hover:from-emerald-400 hover:via-teal-400 hover:to-emerald-500 text-slate-950 font-black text-sm py-3 px-4 rounded-xl shadow-[0_4px_18px_rgba(16,185,129,0.35)] hover:shadow-[0_6px_22px_rgba(16,185,129,0.5)] active:scale-95 transform transition-all duration-150 uppercase tracking-wider flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <span>Start Match</span>
                      <span className="text-base">🚀</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => onNavigate('home')}
                      className="w-full py-2.5 px-4 rounded-xl bg-[#1b2238] hover:bg-[#252f4c] border border-slate-700/80 text-slate-300 hover:text-white font-black text-xs uppercase tracking-wider transition-all duration-150 shadow-sm hover:shadow-md active:scale-95 flex items-center justify-center gap-2 cursor-pointer"
                    >
                      <span className="text-sm">🠔</span>
                      <span>Main Menu</span>
                    </button>
                  </div>
                </div>
              </div>

              {/* RIGHT COLUMN: ODD GAMES (TAB POINTING LEFT) */}
              <div className="space-y-3 flex flex-col justify-center">
                {allGames.filter((_, idx) => idx % 2 !== 0).map((game, rightIdx) => {
                  const originalIdx = rightIdx * 2 + 1;
                  const orderIndex = selectedGames.findIndex(g => g.name === game.name);
                  const isSelected = orderIndex !== -1;
                  const isPremium = game.plan === 'premium';
                  const isAvailable = isGameAvailableInMode(game, gameMode);

                  return (
                    <div key={originalIdx} className="relative group/tooltip">
                      <button
                        onClick={() => handleGameSelect(game)}
                        disabled={!isAvailable}
                        className={`w-full p-4 rounded-2xl flex items-center justify-between text-left transition-all duration-200 border relative cursor-pointer ${
                          !isAvailable
                            ? 'bg-[#0f121d] border-slate-800/40 opacity-40 cursor-not-allowed'
                            : isSelected
                              ? isPremium
                                ? 'bg-gradient-to-l from-[#2a1a3e] to-[#3b2055] border-purple-500 shadow-[0_0_20px_rgba(168,85,247,0.3)] -translate-x-2 ring-1 ring-purple-400'
                                : 'bg-gradient-to-l from-[#17203a] to-[#1e2a52] border-indigo-500 shadow-[0_0_20px_rgba(99,102,241,0.3)] -translate-x-2 ring-1 ring-indigo-400'
                              : 'bg-[#121624] border-slate-800 hover:border-slate-700 hover:bg-[#161c2e]'
                        }`}
                      >
                        {/* INTERLOCKING PUZZLE TAB & ORDER BADGE ON LEFT EDGE */}
                        {isSelected && (
                          <div className="absolute -left-3.5 top-1/2 -translate-y-1/2 z-20 w-7 h-7 rounded-full bg-gradient-to-r from-indigo-500 to-purple-500 text-white font-black text-xs flex items-center justify-center border-2 border-indigo-300 shadow-[0_0_12px_rgba(99,102,241,0.8)] animate-pulse">
                            #{orderIndex + 1}
                          </div>
                        )}

                        <div className="flex flex-col pl-4">
                          <span className={`text-sm font-black ${
                            !isAvailable
                              ? 'text-slate-500 line-through'
                              : isSelected ? (isPremium ? 'text-purple-300' : 'text-indigo-300') : 'text-slate-200'
                          }`}>
                            {game.name}
                          </span>
                          <span className="text-[10px] text-slate-500 font-bold uppercase mt-0.5">
                            {isPremium ? '⭐ Premium Game' : '🎮 Basic Game'}
                          </span>
                        </div>
                      </button>

                      {/* TOOLTIP */}
                      <div className="absolute z-30 bottom-full left-1/2 transform -translate-x-1/2 mb-2 w-64 p-3 bg-[#0f1322] border border-slate-700 rounded-xl shadow-2xl opacity-0 pointer-events-none group-hover/tooltip:opacity-100 transition-opacity duration-200 text-xs text-slate-300 leading-relaxed backdrop-blur-md">
                        <div className="font-bold text-indigo-400 mb-1">{game.name}</div>
                        {game.description}
                      </div>
                    </div>
                  );
                })}
              </div>

            </div>
          </div>

          {/* C) Dynamic Score Presets Toolkit */}
          <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800">
            <div className="flex justify-between items-center mb-2">
              <label className="block text-xs font-black tracking-widest text-indigo-300 uppercase">
                C) Score Presets ({scorePresets.length} / 8)
              </label>
              <button
                onClick={handleAddScorePreset}
                disabled={scorePresets.length >= 8}
                className="text-xs font-black bg-indigo-500/10 border border-indigo-500/30 hover:border-indigo-400 text-indigo-400 px-3 py-1 rounded-lg transition-all uppercase tracking-wide disabled:opacity-30 disabled:cursor-not-allowed cursor-pointer"
              >
                + Add Button
              </button>
            </div>
            <p className="text-slate-400 text-xs mb-4">Quick point values you can add (+) or subtract (-) with a single click during the match (Min 2 - Max 8 buttons).</p>
            
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {scorePresets.map((preset, index) => (
                <div key={preset.id} className="flex flex-col gap-1.5 p-3 bg-[#121624] border border-slate-800/80 rounded-xl relative group">
                  <button
                    onClick={() => handleRemoveScorePreset(preset.id)}
                    disabled={scorePresets.length <= 2}
                    className="absolute top-1 right-2 text-slate-600 hover:text-rose-400 text-xs font-bold transition-colors disabled:opacity-0 disabled:pointer-events-none cursor-pointer"
                    title="Remove button"
                  >
                    ×
                  </button>
                  <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">
                    Preset #{index + 1}
                  </span>
                  <input 
                    type="text" 
                    value={preset.value} 
                    onChange={(e) => handleScoreValueChange(preset.id, e.target.value)} 
                    placeholder="e.g. 50" 
                    className="bg-[#0f121d] border-b-4 border-slate-950 font-black text-center p-2 rounded-lg focus:outline-none focus:border-indigo-500 text-slate-200 text-base shadow-inner w-full" 
                  />
                </div>
              ))}
            </div>
          </div>

        </div>
      )}

      {/* TAB 2 CONTENT: PREMIUM PLAN */}
      {activeTab === 'premium' && (
        <div className="space-y-6">
          {/* OPTION A */}
          <div className="bg-gradient-to-br from-[#1b2238] to-[#1e1a3a] p-6 rounded-2xl border border-purple-500/20 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="flex-1">
              <h3 className="text-lg font-bold text-purple-300 mb-1">A) Download Offline Standalone App (5 Games Included)</h3>
              <p className="text-slate-400 text-xs leading-relaxed max-w-xl">
                Get the full offline standalone system packaged in a <strong>.zip</strong> archive. Includes the independent desktop application, folder structure, and the 5 core mini-games (Zero Margin, Hangman, TicTacToe, Roulette, Trivia Quiz) with local resources.
              </p>
              <button
                onClick={() => setIsPaymentModalOpen(true)}
                className="mt-4 bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-500 hover:to-indigo-500 text-white text-xs font-black py-3 px-6 rounded-xl uppercase tracking-wider shadow-lg transition-all active:scale-95 cursor-pointer flex items-center gap-2"
              >
                📥 Download Standalone App + 5 Games (.zip)
              </button>
            </div>
            <div className="bg-purple-500/10 border border-purple-500/30 rounded-xl p-4 text-center shrink-0 min-w-[130px]">
              <span className="block text-xs text-slate-400 uppercase font-bold tracking-wider mb-1">Price</span>
              <span className="text-2xl font-black text-purple-400">$5.00</span>
            </div>
          </div>

          {/* OPTION B */}
          <div className="bg-gradient-to-br from-[#1b2238] to-[#251b30] p-6 rounded-2xl border border-pink-500/20 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div className="flex-1">
              <h3 className="text-lg font-bold text-pink-300 mb-1">B) Add Additional Premium Modules</h3>
              <p className="text-slate-400 text-xs leading-relaxed max-w-xl">
                Expand your mini-game library with individual premium modules. Each module downloads into its own folder ready to drop into your catalog.
              </p>
              <button
                onClick={() => alert("⭐ Coming soon: You will be able to purchase and download individual Premium game modules ($1.00 per game).")}
                className="mt-4 bg-gradient-to-r from-pink-600 to-rose-600 hover:from-pink-500 hover:to-rose-500 text-white text-xs font-black py-3 px-6 rounded-xl uppercase tracking-wider shadow-lg transition-all active:scale-95 cursor-pointer flex items-center gap-2"
              >
                ⭐ Get Premium Modules ($1.00 / game)
              </button>
            </div>
            <div className="bg-pink-500/10 border border-pink-500/30 rounded-xl p-4 text-center shrink-0 min-w-[130px]">
              <span className="block text-xs text-slate-400 uppercase font-bold tracking-wider mb-1">Per Module</span>
              <span className="text-2xl font-black text-pink-400">$1.00</span>
            </div>
          </div>
          
          {/* PAYMENT / PROMO CHECKOUT MODAL */}
          <PaymentModal
            isOpen={isPaymentModalOpen}
            onClose={() => setIsPaymentModalOpen(false)}
            onDownloadSuccess={() => window.open('http://localhost:8080/api/download/offline-template', '_blank')}
          />
        </div>
      )}
    </div>
  );
}