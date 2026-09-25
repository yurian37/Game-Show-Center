import React from 'react';
import SvgEmoji from '../../../SvgEmoji';

export default function TicTacToeSetup({ value, onChange }) {
  const currentTeam = value?.starting_team || 'random';

  const selectTeam = (team) => {
    onChange({
      ...value,
      starting_team: team
    });
  };

  const options = [
    { key: 'red', label: 'Red Team', icon: 'circle-dot', style: 'border-rose-500/20 hover:border-rose-500/50 text-rose-300 bg-rose-500/5' },
    { key: 'blue', label: 'Blue Team', icon: 'circle-dot', style: 'border-sky-500/20 hover:border-sky-500/50 text-sky-300 bg-sky-500/5' },
    { key: 'random', label: 'Random', icon: 'dice', style: 'border-amber-500/20 hover:border-amber-500/50 text-amber-300 bg-amber-500/5' }
  ];

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      <h3 className="text-lg font-black text-purple-300 mb-2">TicTacToe Configuration</h3>
      <p className="text-xs text-slate-400 mb-6">
        Select which team will start the TicTacToe match.
      </p>

      <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-3">
        Starting Team
      </label>
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 max-w-xl">
        {options.map((opt) => {
          const isActive = currentTeam === opt.key;
          return (
            <button
              key={opt.key}
              type="button"
              onClick={() => selectTeam(opt.key)}
              className={`p-4 rounded-xl border font-bold text-sm text-center transition-all ${opt.style} ${
                isActive 
                  ? 'ring-2 ring-indigo-500 border-indigo-500 bg-indigo-500/10 text-indigo-200' 
                  : 'opacity-70 hover:opacity-100'
              }`}
            >
              {opt.icon ? <span className="flex items-center justify-center gap-1.5"><SvgEmoji name={opt.icon} /> {opt.label}</span> : opt.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export function serialize(value) {
  return {
    game: 'TicTacToe',
    startingTeam: value?.starting_team || 'random'
  };
}