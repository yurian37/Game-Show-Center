import React, { useState } from 'react';

export default function GuessCharacterSetup({ value, onChange }) {
  const [newUrl, setNewUrl] = useState('');

  const handleFieldChange = (field, val) => {
    onChange({
      ...value,
      [field]: val
    });
  };

  const handleAddFiles = (files) => {
    if (!files || files.length === 0) return;
    const newImageUrls = Array.from(files).map((file) => URL.createObjectURL(file));

    const pool = value?.media_pool || [];
    onChange({
      ...value,
      media_pool: [...pool, ...newImageUrls]
    });
  };

  const handleAddUrl = (e) => {
    if (e) e.preventDefault();
    const url = newUrl.trim();
    if (!url) return;

    const pool = value?.media_pool || [];
    onChange({
      ...value,
      media_pool: [...pool, url]
    });
    setNewUrl('');
  };

  const handleRemoveItem = (indexToRemove) => {
    const pool = value?.media_pool || [];
    onChange({
      ...value,
      media_pool: pool.filter((_, idx) => idx !== indexToRemove)
    });
  };

  const mediaPool = value?.media_pool || [];
  const enableTimer = value?.enable_timer !== false;
  const timerSeconds = value?.timer_seconds || 30;

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      <h3 className="text-lg font-black text-amber-400 mb-2 flex items-center gap-2">
        Guess Character Setup <span className="text-[10px] bg-amber-500/20 text-amber-400 px-2 py-0.5 rounded uppercase font-bold">Premium</span>
      </h3>
      <p className="text-xs text-slate-400 mb-6">
        Configure rounds, optional countdown timer, and manage the character image pool for contestants to guess.
      </p>

      {/* PARAMETERS GRID */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
            Rounds per Player
          </label>
          <input
            type="number"
            value={value?.rounds_per_player || 3}
            onChange={(e) => handleFieldChange('rounds_per_player', Math.max(1, parseInt(e.target.value) || 1))}
            className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
            min="1"
            max="20"
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
            Set Timer
          </label>
          <button
            type="button"
            onClick={() => handleFieldChange('enable_timer', !enableTimer)}
            className={`w-full py-2.5 px-4 rounded-xl text-xs font-black uppercase tracking-wider transition-all border flex items-center justify-center gap-2 cursor-pointer ${
              enableTimer
                ? 'bg-emerald-600/30 text-emerald-300 border-emerald-500/50 shadow-sm'
                : 'bg-slate-800/50 text-slate-400 border-slate-700'
            }`}
          >
            <span>{enableTimer ? '⏱️ Timer Enabled' : '⏸️ Timer Disabled'}</span>
          </button>
        </div>

        {enableTimer && (
          <div>
            <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
              Timer Duration (Seconds)
            </label>
            <input
              type="number"
              value={timerSeconds}
              onChange={(e) => handleFieldChange('timer_seconds', Math.max(5, parseInt(e.target.value) || 30))}
              className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
              min="5"
              max="300"
            />
          </div>
        )}
      </div>

      {/* CHARACTER IMAGE POOL SECTION */}
      <div className="border-t border-slate-800/80 pt-6">
        <div className="flex justify-between items-center mb-3">
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider">
            Character Image Pool ({mediaPool.length} images)
          </label>
          <span className="text-[10px] font-bold text-slate-400 bg-[#121624] px-2.5 py-1 rounded-md border border-slate-800">
            📸 Visual Mystery Pool
          </span>
        </div>

        {/* INPUT VIA URL */}
        <form onSubmit={handleAddUrl} className="flex gap-2 mb-4">
          <input
            type="text"
            placeholder="Paste character image URL (https://...)..."
            value={newUrl}
            onChange={(e) => setNewUrl(e.target.value)}
            className="flex-grow bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-xs font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-700"
          />
          <button
            type="submit"
            className="bg-purple-600 hover:bg-purple-500 text-white font-bold px-5 rounded-xl text-xs uppercase tracking-wider transition-colors"
          >
            Add URL
          </button>
        </form>

        {/* VISUAL IMAGE TILES GRID */}
        <div className="flex flex-wrap gap-3 items-center max-h-[320px] overflow-y-auto bg-[#0f121d] p-4 rounded-xl border border-slate-900">
          {/* UPLOAD TILE (LIKE PROFILE AVATAR PICKER) */}
          <label className="w-24 h-24 shrink-0 rounded-xl bg-[#121624] border-2 border-dashed border-slate-700 hover:border-indigo-500 transition-colors flex flex-col items-center justify-center cursor-pointer text-slate-500 hover:text-indigo-400 group">
            <span className="text-2xl font-bold text-slate-400 group-hover:text-indigo-400">+</span>
            <span className="text-[9px] font-bold uppercase tracking-wider text-slate-400 group-hover:text-indigo-400">Upload Image</span>
            <input
              type="file"
              accept="image/*"
              multiple
              onChange={(e) => handleAddFiles(e.target.files)}
              className="hidden"
            />
          </label>

          {/* IMAGES THUMBNAILS LIST */}
          {mediaPool.map((img, idx) => (
            <div
              key={idx}
              className="relative w-24 h-24 rounded-xl overflow-hidden border border-slate-700 bg-[#1b2238] group shrink-0 shadow-md"
            >
              <img
                src={img}
                alt={`Character ${idx + 1}`}
                className="w-full h-full object-cover"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" stroke="%2394a3b8" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/></svg>';
                }}
              />
              <span className="absolute bottom-1 left-1 bg-black/75 backdrop-blur-xs text-white text-[9px] px-1.5 py-0.5 rounded font-mono font-bold">
                #{idx + 1}
              </span>
              <button
                type="button"
                onClick={() => handleRemoveItem(idx)}
                className="absolute top-1 right-1 bg-rose-600/90 hover:bg-rose-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold transition-all opacity-0 group-hover:opacity-100 shadow"
                title="Remove Character"
              >
                ✕
              </button>
            </div>
          ))}

          {mediaPool.length === 0 && (
            <p className="text-xs text-slate-600 py-4 w-full text-center">
              Image pool is empty. Click "+ Upload Image" or paste a URL above to add characters.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  return {
    game: 'Guess_Character',
    roundsPerPlayer: value?.rounds_per_player || 3,
    enableTimer: value?.enable_timer !== false,
    timerSeconds: value?.timer_seconds || 30,
    mediaPool: value?.media_pool || []
  };
}
