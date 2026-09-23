import React, { useState } from 'react';

export const AVAILABLE_FILTERS = [
  { id: 'displacement', label: 'Displacement', icon: '〰️', desc: 'Sine wave coordinate distortion' },
  { id: 'swirl', label: 'Swirl', icon: '🌪️', desc: 'Spiral twist and vortex rotation' },
  { id: 'pixelate', label: 'Pixelate', icon: '🧱', desc: 'Chunky mosaic pixel blocks' },
  { id: 'blur', label: 'Blur', icon: '🌫️', desc: 'Progressive visual softening' }
];

export default function SnapSolveSetup({ value, onChange }) {
  const [newUrl, setNewUrl] = useState('');

  const selectedFilters = Array.isArray(value?.selected_filters) && value.selected_filters.length > 0
    ? value.selected_filters
    : ['displacement', 'swirl', 'pixelate', 'blur'];

  const handleRoundsChange = (val) => {
    onChange({
      ...value,
      rounds_per_player: Math.max(1, parseInt(val) || 1)
    });
  };

  const handleToggleFilter = (filterId) => {
    let updated;
    if (selectedFilters.includes(filterId)) {
      if (selectedFilters.length <= 1) return; // Prevent deselecting all
      updated = selectedFilters.filter((f) => f !== filterId);
    } else {
      updated = [...selectedFilters, filterId];
    }
    onChange({
      ...value,
      selected_filters: updated
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

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      <h3 className="text-lg font-black text-amber-400 mb-2 flex items-center gap-2">
        Snap Solve Setup <span className="text-[10px] bg-amber-500/20 text-amber-400 px-2 py-0.5 rounded uppercase font-bold">Premium</span>
      </h3>
      <p className="text-xs text-slate-400 mb-6">
        Configure rounds per player, enabled visual distortion filters, and manage the mystery image clue pool.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        {/* ROUNDS PER PLAYER */}
        <div className="w-full">
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
            Rounds per Player
          </label>
          <input
            type="number"
            value={value?.rounds_per_player || 2}
            onChange={(e) => handleRoundsChange(e.target.value)}
            className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
            min="1"
            max="20"
          />
          <span className="text-[10px] text-slate-500 mt-1 block">
            Total required images = Contestants × Rounds
          </span>
        </div>

        {/* FILTERS MULTI-SELECT */}
        <div className="w-full">
          <div className="flex justify-between items-center mb-2">
            <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider">
              Available Distortion Filters ({selectedFilters.length} active)
            </label>
            <span className="text-[10px] font-bold text-amber-400">
              Minimum 1 selected
            </span>
          </div>

          <div className="grid grid-cols-2 gap-2">
            {AVAILABLE_FILTERS.map((f) => {
              const isChecked = selectedFilters.includes(f.id);
              return (
                <button
                  type="button"
                  key={f.id}
                  onClick={() => handleToggleFilter(f.id)}
                  className={`flex items-center gap-2.5 p-2.5 rounded-xl border text-left transition-all cursor-pointer ${
                    isChecked
                      ? 'bg-purple-600/20 border-purple-500 text-white shadow-sm'
                      : 'bg-[#121624] border-slate-800 text-slate-400 hover:border-slate-700'
                  }`}
                >
                  <span className="text-lg shrink-0">{f.icon}</span>
                  <div className="min-w-0">
                    <span className="text-xs font-bold block truncate">{f.label}</span>
                    <span className="text-[9px] text-slate-400 block truncate">{f.desc}</span>
                  </div>
                  <span className={`ml-auto text-xs font-bold ${isChecked ? 'text-purple-400' : 'text-slate-600'}`}>
                    {isChecked ? '✓' : '○'}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      <div className="border-t border-slate-800/80 pt-6">
        <div className="flex justify-between items-center mb-3">
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider">
            Image Pool ({mediaPool.length} images)
          </label>
          <span className="text-[10px] font-bold text-indigo-400 bg-indigo-500/10 px-2.5 py-1 rounded-md border border-indigo-500/20">
            🖼️ Visual Pool
          </span>
        </div>

        {/* INPUT VIA URL */}
        <form onSubmit={handleAddUrl} className="flex gap-2 mb-4">
          <input
            type="text"
            placeholder="Paste direct image URL (https://...)..."
            value={newUrl}
            onChange={(e) => setNewUrl(e.target.value)}
            className="flex-grow bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-xs font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-700"
          />
          <button
            type="submit"
            className="bg-purple-600 hover:bg-purple-500 text-white font-bold px-5 rounded-xl text-xs uppercase tracking-wider transition-colors cursor-pointer"
          >
            Add URL
          </button>
        </form>

        {/* VISUAL IMAGE TILES GRID */}
        <div className="flex flex-wrap gap-3 items-center max-h-[320px] overflow-y-auto bg-[#0f121d] p-4 rounded-xl border border-slate-900">
          {/* UPLOAD TILE */}
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
                alt={`Snap Solve #${idx + 1}`}
                className="w-full h-full object-cover"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 24 24" fill="none" stroke="%2394a3b8" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>';
                }}
              />
              <span className="absolute bottom-1 left-1 bg-black/75 backdrop-blur-xs text-white text-[9px] px-1.5 py-0.5 rounded font-mono font-bold">
                #{idx + 1}
              </span>
              <button
                type="button"
                onClick={() => handleRemoveItem(idx)}
                className="absolute top-1 right-1 bg-rose-600/90 hover:bg-rose-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold transition-all opacity-0 group-hover:opacity-100 shadow cursor-pointer"
                title="Remove Image"
              >
                ✕
              </button>
            </div>
          ))}

          {mediaPool.length === 0 && (
            <p className="text-xs text-slate-600 py-4 w-full text-center">
              Image pool is currently empty. Click "+ Upload Image" or paste direct URLs above to add puzzle images.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  return {
    game: 'Snap_Solve',
    roundsPerPlayer: value?.rounds_per_player || 2,
    selectedFilters: value?.selected_filters || ['displacement', 'swirl', 'pixelate', 'blur'],
    mediaPool: value?.media_pool || []
  };
}
