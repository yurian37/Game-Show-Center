import React, { useState } from 'react';
import SvgEmoji from '../../../SvgEmoji';

const formatYear = (year) => {
  const y = parseInt(year, 10);
  if (isNaN(y)) return '';
  if (y < 0) return `${Math.abs(y)} BC`;
  return `${y} AD`;
};

export default function TimeLineSetup({ value, onChange, numPlayers, players }) {
  const events = value?.events || [];

  const [newTitle, setNewTitle] = useState('');
  const [newYear, setNewYear] = useState('');
  const [newDesc, setNewDesc] = useState('');
  const [search, setSearch] = useState('');

  const handleAddEvent = (e) => {
    e.preventDefault();
    if (!newTitle.trim() || newYear === '' || isNaN(parseInt(newYear, 10))) {
      alert('Please provide a valid milestone title and numerical year.');
      return;
    }

    const newEvent = {
      id: `ev_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`,
      title: newTitle.trim(),
      year: parseInt(newYear, 10),
      description: newDesc.trim()
    };

    const updated = [...events, newEvent];
    onChange({
      ...value,
      events: updated
    });

    setNewTitle('');
    setNewYear('');
    setNewDesc('');
  };

  const handleRemoveEvent = (idToRemove) => {
    if (events.length <= 3) {
      alert('At least 3 timeline events are required for a match.');
      return;
    }
    const updated = events.filter((ev) => ev.id !== idToRemove);
    onChange({
      ...value,
      events: updated
    });
  };

  const filteredEvents = events.filter((ev) => {
    const q = search.toLowerCase();
    return (
      ev.title?.toLowerCase().includes(q) ||
      ev.description?.toLowerCase().includes(q) ||
      String(ev.year).includes(q)
    );
  });

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn text-slate-200">
      <div className="flex items-center gap-3 mb-2">
        <span className="text-2xl flex items-center justify-center"><SvgEmoji name="hourglass" /></span>
        <h3 className="text-xl font-black text-amber-300">TimeLine Settings</h3>
      </div>
      <p className="text-xs text-slate-400 mb-6">
        Chronological educational game without predefined rounds: the game concludes once all historical milestones have been placed on the timeline. The host awards points manually in the Arena.
      </p>

      {/* Add New Event Card Form */}
      <div className="bg-[#121624]/90 p-5 rounded-2xl border border-slate-800/80 mb-6">
        <h4 className="text-sm font-black text-amber-400 uppercase tracking-wide mb-3 flex items-center gap-2">
          <SvgEmoji name="plus" className="mr-1" /> Add Historical Milestone
        </h4>
        <form onSubmit={handleAddEvent} className="space-y-3">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div className="md:col-span-2">
              <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1">Milestone Title</label>
              <input
                type="text"
                placeholder="e.g. Galileo's Telescope"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                className="w-full bg-[#0d101a] border border-slate-700/60 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-amber-500"
              />
            </div>
            <div>
              <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1">Year (e.g. -500 or 1609)</label>
              <input
                type="number"
                placeholder="1609"
                value={newYear}
                onChange={(e) => setNewYear(e.target.value)}
                className="w-full bg-[#0d101a] border border-slate-700/60 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-amber-500 font-mono"
              />
            </div>
          </div>

          <div>
            <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1">Description / Clue</label>
            <input
              type="text"
              placeholder="e.g. First astronomical observations with a refracting telescope..."
              value={newDesc}
              onChange={(e) => setNewDesc(e.target.value)}
              className="w-full bg-[#0d101a] border border-slate-700/60 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-amber-500"
            />
          </div>

          <div className="flex justify-end pt-1">
            <button
              type="submit"
              className="px-5 py-2 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-black text-xs rounded-xl shadow-md transition-all active:scale-95 cursor-pointer"
            >
              Save Milestone
            </button>
          </div>
        </form>
      </div>

      {/* Events Bank List */}
      <div className="border-t border-slate-800/80 pt-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-4">
          <div>
            <h4 className="text-sm font-black text-slate-200">
              Milestone Bank ({events.length} items configured)
            </h4>
            <span className="text-[11px] text-slate-400">All items will be utilized during the match.</span>
          </div>

          <input
            type="text"
            placeholder="Search milestone..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full sm:w-60 bg-[#121624] border border-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-amber-500"
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-96 overflow-y-auto pr-1">
          {filteredEvents.map((ev) => (
            <div
              key={ev.id}
              className="bg-[#121624] border border-slate-800/80 rounded-xl p-3 flex items-start justify-between gap-3 hover:border-slate-700 transition-colors"
            >
              <div className="space-y-1 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="px-2 py-0.5 rounded-md bg-amber-500/20 border border-amber-500/40 text-amber-300 font-mono text-xs font-black">
                    {formatYear(ev.year)}
                  </span>
                </div>
                <h5 className="text-sm font-bold text-white">{ev.title}</h5>
                <p className="text-xs text-slate-400 line-clamp-2">{ev.description}</p>
              </div>

              <button
                type="button"
                onClick={() => handleRemoveEvent(ev.id)}
                className="text-rose-400 hover:text-rose-300 hover:bg-rose-500/10 p-1.5 rounded-lg transition-colors text-xs cursor-pointer"
                title="Delete milestone"
              >
                <SvgEmoji name="trash" />
              </button>
            </div>
          ))}
          {filteredEvents.length === 0 && (
            <div className="col-span-full py-8 text-center text-xs text-slate-500 italic">
              No milestones match your search.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}