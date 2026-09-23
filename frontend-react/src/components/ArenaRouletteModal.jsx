import React, { useState, useMemo, useEffect } from 'react';
import flipMp3 from '../assets/roulette/flip.mp3';
import winMp3 from '../assets/roulette/win.mp3';
import soundManager from '../services/soundManager';

const SLICE_COLORS = [
  '#6366f1', // Electric Indigo
  '#ec4899', // Magenta Pink
  '#f59e0b', // Amber Gold
  '#10b981', // Emerald Green
  '#8b5cf6', // Royal Violet
  '#06b6d4', // Cyan Sky
  '#f97316', // Orange
  '#14b8a6', // Teal
  '#e11d48', // Rose
  '#84cc16'  // Lime
];

const SLICE_BORDER_COLORS = [
  '#818cf8',
  '#f472b6',
  '#fbbf24',
  '#34d399',
  '#a78bfa',
  '#22d3ee',
  '#fb923c',
  '#2dd4bf',
  '#fb7185',
  '#a3e635'
];

export default function ArenaRouletteModal({ isOpen, onClose, profiles = [] }) {
  const [isSpinning, setIsSpinning] = useState(false);
  const [winner, setWinner] = useState(null);
  const [rotationDegrees, setRotationDegrees] = useState(0);
  const [excludedIds, setExcludedIds] = useState(new Set());

  // Reset to include ALL contestants every time the modal is opened
  useEffect(() => {
    if (isOpen) {
      setExcludedIds(new Set());
      setWinner(null);
      setIsSpinning(false);
    }
  }, [isOpen]);

  // Stop any audio on unmount
  useEffect(() => {
    return () => {
      soundManager.stopAll();
    };
  }, []);

  // Filter active competitors
  const activeCompetitors = useMemo(() => {
    return (profiles || []).filter((p) => !excludedIds.has(p.id || p.name));
  }, [profiles, excludedIds]);

  const toggleExclude = (competitor) => {
    if (isSpinning) return;
    const key = competitor.id || competitor.name;
    const nextSet = new Set(excludedIds);

    if (nextSet.has(key)) {
      // Re-include
      nextSet.delete(key);
      setExcludedIds(nextSet);
      setWinner(null);
    } else {
      // Exclude only if at least 2 will remain
      if (activeCompetitors.length <= 2) {
        return;
      }
      nextSet.add(key);
      setExcludedIds(nextSet);
      setWinner(null);
    }
  };

  // Equal weights (each active competitor has weight = 1.0)
  const sliceData = useMemo(() => {
    if (!activeCompetitors || activeCompetitors.length === 0) return [];

    const totalCount = activeCompetitors.length;
    const sliceAngle = 360 / totalCount;

    return activeCompetitors.map((p, idx) => {
      const startAngle = idx * sliceAngle;
      const endAngle = (idx + 1) * sliceAngle;
      const midAngle = startAngle + sliceAngle / 2;

      return {
        competitor: p,
        weight: 1.0,
        color: SLICE_COLORS[idx % SLICE_COLORS.length],
        borderColor: SLICE_BORDER_COLORS[idx % SLICE_BORDER_COLORS.length],
        startAngle,
        endAngle,
        sliceAngle,
        midAngle,
        percentage: 100 / totalCount
      };
    });
  }, [activeCompetitors]);

  // Helper to build SVG Arc path
  const describeArc = (cx, cy, r, startAngle, endAngle) => {
    if (endAngle - startAngle >= 359.99) {
      return `M ${cx - r}, ${cy} a ${r},${r} 0 1,0 ${r * 2},0 a ${r},${r} 0 1,0 -${r * 2},0`;
    }

    const startRad = (startAngle * Math.PI) / 180;
    const endRad = (endAngle * Math.PI) / 180;

    const x1 = cx + r * Math.cos(startRad);
    const y1 = cy + r * Math.sin(startRad);
    const x2 = cx + r * Math.cos(endRad);
    const y2 = cy + r * Math.sin(endRad);

    const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0;

    return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`;
  };

  // Trigger spin with equal probability
  const spinRoulette = () => {
    if (isSpinning || !sliceData || sliceData.length < 2) return;

    setIsSpinning(true);
    setWinner(null);

    soundManager.playSfx(flipMp3);

    // Pick uniform random winner
    const chosenIndex = Math.floor(Math.random() * sliceData.length);
    const chosenSlice = sliceData[chosenIndex];
    if (!chosenSlice) {
      setIsSpinning(false);
      return;
    }

    // Align slice under the top pointer (270 deg / 12 o'clock)
    const midAngle = chosenSlice.midAngle;
    let targetLandingBase = 270 - midAngle;
    while (targetLandingBase < 0) targetLandingBase += 360;

    const halfSpan = chosenSlice.sliceAngle * 0.35;
    const randomOffset = (Math.random() * 2 - 1) * halfSpan;

    const baseRotation = Math.ceil(rotationDegrees / 360) * 360 + 2160;
    const targetAngle = baseRotation + targetLandingBase + randomOffset;

    setRotationDegrees(targetAngle);

    setTimeout(() => {
      setIsSpinning(false);
      setWinner(chosenSlice.competitor);
      soundManager.playSfx(winMp3);
    }, 3500);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 animate-fadeIn">
      <div className="relative w-full max-w-2xl bg-[#141929] border-2 border-amber-500/40 rounded-3xl p-6 md:p-8 shadow-[0_25px_60px_-15px_rgba(0,0,0,0.9)] flex flex-col items-center max-h-[90vh] overflow-y-auto">
        
        {/* CLOSE BUTTON */}
        <button
          onClick={onClose}
          disabled={isSpinning}
          className="absolute top-4 right-4 text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-700 px-3 py-1.5 rounded-xl text-xs font-black tracking-wider transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
        >
          ✖ Close
        </button>

        {/* MODAL HEADER */}
        <div className="text-center mb-4">
          <div className="inline-flex items-center gap-2 bg-amber-500/10 border border-amber-500/30 px-3 py-1 rounded-full text-amber-400 text-xs font-black uppercase tracking-widest mb-2">
            <span>🎰</span>
            <span>Arena Roulette</span>
          </div>
          <h3 className="text-xl md:text-2xl font-black text-white">
            Quick Draw • Equal Probability
          </h3>
          <p className="text-xs text-slate-400 mt-1">
            Spin the roulette at any moment. You can temporarily exclude contestants (minimum 2).
          </p>
        </div>

        {/* CONTESTANT SELECTOR / REMOVER CHIPS */}
        <div className="w-full bg-[#0d101a] border border-slate-800 rounded-2xl p-3 mb-4">
          <div className="flex items-center justify-between gap-2 mb-2">
            <span className="text-[11px] font-bold text-slate-300 uppercase tracking-wider">
              Contestants ({activeCompetitors.length} / {profiles.length} active)
            </span>
            <span className="text-[10px] text-amber-400/80 font-medium">
              * Minimum 2 to spin
            </span>
          </div>

          <div className="flex flex-wrap gap-2 max-h-28 overflow-y-auto pr-1">
            {profiles.map((p) => {
              const key = p.id || p.name;
              const isExcluded = excludedIds.has(key);
              const canRemove = activeCompetitors.length > 2 || isExcluded;

              return (
                <button
                  key={key}
                  type="button"
                  disabled={isSpinning || (!canRemove && !isExcluded)}
                  onClick={() => toggleExclude(p)}
                  className={`inline-flex items-center gap-2 px-2.5 py-1 rounded-xl text-xs font-bold transition-all cursor-pointer border ${
                    isExcluded
                      ? 'bg-slate-900/60 border-slate-800 text-slate-500 line-through opacity-60 hover:opacity-100 hover:line-through-none'
                      : 'bg-[#1b2238] border-slate-700 text-white hover:border-rose-500/60'
                  } ${!canRemove && !isExcluded ? 'cursor-not-allowed opacity-80' : ''}`}
                  title={
                    isExcluded
                      ? 'Click to include again'
                      : canRemove
                      ? 'Click to exclude from roulette'
                      : 'At least 2 contestants are required to spin'
                  }
                >
                  <div
                    className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                    style={{ backgroundColor: p.color || '#f59e0b' }}
                  />
                  <span className="truncate max-w-[110px]">{p.name}</span>
                  <span
                    className={`text-[10px] px-1 rounded font-mono ${
                      isExcluded
                        ? 'text-emerald-400 bg-emerald-500/20'
                        : canRemove
                        ? 'text-rose-400 hover:bg-rose-500/20'
                        : 'text-slate-500'
                    }`}
                  >
                    {isExcluded ? '➕' : canRemove ? '✕' : '🔒'}
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {/* ROULETTE WHEEL CONTAINER */}
        <div className="relative w-64 h-64 md:w-72 md:h-72 flex items-center justify-center my-2">
          
          {/* TOP POINTER */}
          <div className="absolute -top-3 left-1/2 -translate-x-1/2 z-20 flex flex-col items-center pointer-events-none drop-shadow-[0_4px_10px_rgba(0,0,0,0.8)]">
            <div className="w-0 h-0 border-l-[14px] border-l-transparent border-r-[14px] border-r-transparent border-t-[22px] border-t-amber-400 filter drop-shadow-[0_2px_4px_rgba(0,0,0,0.5)]" />
            <div className="w-2.5 h-2.5 rounded-full bg-white -mt-1 shadow-sm" />
          </div>

          {/* SVG WHEEL */}
          <svg
            viewBox="0 0 320 320"
            className="w-full h-full drop-shadow-[0_0_25px_rgba(99,102,241,0.25)]"
            style={{
              transform: `rotate(${rotationDegrees}deg)`,
              transition: isSpinning ? 'transform 3.5s cubic-bezier(0.15, 0.9, 0.25, 1)' : 'none'
            }}
          >
            <circle cx="160" cy="160" r="156" fill="#0f121d" stroke="#334155" strokeWidth="6" />

            {sliceData.map((slice, idx) => {
              const pathD = describeArc(160, 160, 150, slice.startAngle, slice.endAngle);
              const textRad = (slice.midAngle * Math.PI) / 180;
              const textR = 105;
              const tx = 160 + textR * Math.cos(textRad);
              const ty = 160 + textR * Math.sin(textRad);
              const rotDeg = slice.midAngle + (slice.midAngle > 90 && slice.midAngle < 270 ? 180 : 0);

              const displayName = slice.competitor.name || `P${idx + 1}`;
              const truncatedName = displayName.length > 10 ? displayName.substring(0, 9) + '…' : displayName;

              return (
                <g key={idx}>
                  <path
                    d={pathD}
                    fill={slice.color}
                    stroke={slice.borderColor}
                    strokeWidth="2.5"
                    className="transition-colors"
                  />
                  <text
                    x={tx}
                    y={ty}
                    fill="#ffffff"
                    fontSize="11"
                    fontWeight="900"
                    textAnchor="middle"
                    dominantBaseline="central"
                    transform={`rotate(${rotDeg}, ${tx}, ${ty})`}
                    style={{ textShadow: '0 2px 4px rgba(0,0,0,0.9)' }}
                  >
                    {truncatedName}
                  </text>
                </g>
              );
            })}

            {/* CENTER HUB */}
            <circle cx="160" cy="160" r="28" fill="#1e293b" stroke="#f59e0b" strokeWidth="4" />
            <circle cx="160" cy="160" r="16" fill="#0f172a" />
            <text
              x="160"
              y="160"
              fill="#f59e0b"
              fontSize="14"
              fontWeight="900"
              textAnchor="middle"
              dominantBaseline="central"
            >
              ★
            </text>
          </svg>
        </div>

        {/* WINNER DISPLAY OR ACTION BUTTON */}
        <div className="w-full mt-3 flex flex-col items-center">
          {winner ? (
            <div className="w-full bg-gradient-to-r from-amber-500/20 via-yellow-500/20 to-amber-500/20 border border-amber-400/40 rounded-2xl p-3 text-center animate-bounce mb-3">
              <span className="text-[10px] font-black uppercase tracking-widest text-amber-300">
                🎉 Contestant Selected!
              </span>
              <h4 className="text-xl md:text-2xl font-black text-white mt-0.5">
                {winner.name}
              </h4>
            </div>
          ) : (
            <div className="h-6 mb-2 text-xs text-slate-400 font-bold uppercase tracking-wider">
              {isSpinning ? 'Spinning wheel...' : 'Ready to spin'}
            </div>
          )}

          <div className="flex gap-3">
            <button
              onClick={spinRoulette}
              disabled={isSpinning || !sliceData || sliceData.length < 2}
              className="bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 text-slate-950 font-black text-sm uppercase tracking-wider px-8 py-3 rounded-2xl shadow-lg shadow-amber-500/25 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {isSpinning ? '🌀 Spinning...' : winner ? '🔄 Spin Again' : '🎯 Spin Wheel'}
            </button>
            <button
              onClick={onClose}
              disabled={isSpinning}
              className="bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-sm px-6 py-3 rounded-2xl border border-slate-700 transition-all cursor-pointer disabled:opacity-50"
            >
              Back to Arena
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}
