import React, { useState } from 'react';

export default function ZeroMarginSetup({ value, onChange }) {
  const [newTime, setNewTime] = useState('');

  const handleRoundsChange = (val) => {
    onChange({
      ...value,
      rounds_per_player: Math.max(1, parseInt(val) || 1)
    });
  };

  const handleAddTime = (e) => {
    e.preventDefault();
    const timeVal = parseFloat(newTime);
    if (isNaN(timeVal) || timeVal <= 0) {
      alert("Target time must be a number strictly greater than 0 seconds.");
      return;
    }

    const pool = value?.target_times_pool || [];
    onChange({
      ...value,
      target_times_pool: [...pool, timeVal]
    });
    setNewTime('');
  };

  const handleRemoveTime = (indexToRemove) => {
    const pool = value?.target_times_pool || [];
    onChange({
      ...value,
      target_times_pool: pool.filter((_, idx) => idx !== indexToRemove)
    });
  };

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      <h3 className="text-lg font-black text-purple-300 mb-2">Zero Margin Configuration</h3>
      <p className="text-xs text-slate-400 mb-6">
        Set how many rounds each player will play and configure target times in seconds (&gt; 0).
      </p>

      <div className="w-full max-w-xs mb-6">
        <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
          Rounds per Player
        </label>
        <input
          type="number"
          value={value?.rounds_per_player || 3}
          onChange={(e) => handleRoundsChange(e.target.value)}
          className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
          min="1"
          max="20"
        />
      </div>

      <div className="border-t border-slate-800/80 pt-6">
        <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-3">
          Target Times (Seconds, &gt; 0)
        </label>
        
        <form onSubmit={handleAddTime} className="flex gap-2 mb-4 max-w-sm">
          <input
            type="number"
            step="0.1"
            min="0.1"
            placeholder="Time in sec (e.g. 5.5)..."
            value={newTime}
            onKeyDown={(e) => {
              if (['e', 'E', '+', '-'].includes(e.key)) e.preventDefault();
            }}
            onChange={(e) => setNewTime(e.target.value)}
            className="flex-grow bg-[#121624] border border-slate-800 rounded-xl px-4 py-2 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-700"
          />
          <button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold px-5 rounded-xl text-xs uppercase tracking-wider transition-colors"
          >
            Add
          </button>
        </form>

        <div className="flex flex-wrap gap-2 max-h-48 overflow-y-auto bg-[#0f121d] p-4 rounded-xl border border-slate-900">
          {value?.target_times_pool?.map((timeVal, idx) => (
            <span
              key={idx}
              className="bg-[#1b2238] border border-slate-800 text-slate-300 text-xs px-3 py-1.5 rounded-lg flex items-center gap-2 font-mono font-semibold"
            >
              ⏱️ {timeVal.toFixed(2)}s
              <button
                type="button"
                onClick={() => handleRemoveTime(idx)}
                className="text-slate-500 hover:text-rose-400 font-bold transition-colors text-[10px]"
              >
                ✕
              </button>
            </span>
          ))}
          {(!value?.target_times_pool || value.target_times_pool.length === 0) && (
            <p className="text-xs text-slate-600 py-2 w-full text-center">No target times added.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  return {
    game: 'Zero_Margin',
    roundsPerPlayer: value?.rounds_per_player || 3,
    targetTimesPool: value?.target_times_pool || []
  };
}
