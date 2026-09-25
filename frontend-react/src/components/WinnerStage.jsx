import React from 'react';
import SvgEmoji from './SvgEmoji';

export default function WinnerStage({ matchData, onNavigate }) {
  const profiles = Array.isArray(matchData?.profiles) ? matchData.profiles : [];
  const scores = (matchData?.scores && typeof matchData.scores === 'object') ? matchData.scores : {};
  const gameMode = matchData?.gameMode || '1vs1';

  // Sort competitors by score descending
  const sortedCompetitors = [...profiles].map(p => ({
    ...p,
    score: scores[p?.id] || 0
  })).sort((a, b) => b.score - a.score);

  // Determine the highest score
  const topScore = sortedCompetitors.length > 0 ? sortedCompetitors[0].score : 0;
  
  // Find winners (can be multiple in case of a tie at top score)
  const winners = sortedCompetitors.filter(c => c.score === topScore);
  const primaryWinner = winners[0] || sortedCompetitors[0];

  return (
    <div className="w-full max-w-5xl bg-[#141929] border border-amber-500/30 rounded-[2.5rem] p-6 md:p-12 shadow-[0_0_90px_rgba(245,158,11,0.25)] flex flex-col items-center animate-fadeIn text-slate-200 relative overflow-hidden">
      
      {/* BACKGROUND DECORATIVE GLOW */}
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-96 h-96 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* CELEBRATION HEADER */}
      <div className="text-center mb-8 relative z-10">
        <span className="text-xs font-black tracking-[0.3em] text-amber-400 uppercase bg-amber-500/10 px-4 py-1.5 rounded-full border border-amber-500/20 shadow-md inline-flex items-center gap-1.5">
          <SvgEmoji name="crown" /> Grand Finale — Match Champion
        </span>
        <h2 className="text-3xl md:text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-amber-200 via-yellow-400 to-amber-500 mt-3 drop-shadow-[0_4px_12px_rgba(245,158,11,0.4)]">
          {winners.length > 1 ? 'CO-CHAMPIONS OF THE ARENA!' : 'CHAMPION OF THE ARENA!'}
        </h2>
        <p className="text-xs text-slate-400 mt-1 uppercase tracking-widest font-bold">
          Game Mode: {gameMode}
        </p>
      </div>

      {/* WINNER SPOTLIGHT */}
      <div className="mb-10 flex flex-col items-center relative z-10">
        <div className="relative group">
          {/* CROWN BADGE */}
          <div className="absolute -top-6 left-1/2 -translate-x-1/2 z-20 bg-gradient-to-r from-amber-500 to-yellow-400 text-slate-950 font-black text-xs px-4 py-1 rounded-full shadow-lg border border-yellow-200 uppercase tracking-wider flex items-center gap-1.5 animate-bounce">
            <SvgEmoji name="crown" /> <span>{winners.length > 1 ? 'Co-Winner' : 'Winner'}</span>
          </div>

          {/* MAIN SPOTLIGHT CIRCLE */}
          <div className="w-40 h-40 md:w-48 md:h-48 rounded-full p-1.5 bg-gradient-to-tr from-amber-600 via-yellow-300 to-amber-500 shadow-[0_0_60px_rgba(245,158,11,0.6)] flex items-center justify-center relative">
            <div className="w-full h-full rounded-full bg-[#0d111c] border-4 border-amber-400 overflow-hidden flex items-center justify-center relative shadow-inner">
              {primaryWinner?.avatar ? (
                <img 
                  src={primaryWinner.avatar} 
                  alt={primaryWinner.name} 
                  className="w-full h-full object-cover"
                />
              ) : (
                <span className="text-6xl text-amber-400 font-bold flex items-center justify-center">
                  <SvgEmoji name="user" />
                </span>
              )}
            </div>
          </div>
        </div>

        {/* WINNER NAME & SCORE */}
        <div className="text-center mt-5">
          <h3 className="text-2xl md:text-4xl font-black text-slate-100 tracking-tight">
            {winners.length > 1 
              ? winners.map(w => w.name).join(' & ') 
              : (primaryWinner?.name || 'Winner')}
          </h3>
          <div className="inline-flex items-center gap-2 bg-gradient-to-r from-amber-500/20 to-yellow-500/20 border border-amber-500/40 px-5 py-2 rounded-2xl mt-3 shadow-lg">
            <SvgEmoji name="trophy" className="text-xl" />
            <span className="text-lg font-black text-amber-300 font-mono tracking-wider">
              {topScore} POINTS
            </span>
          </div>
        </div>
      </div>

      {/* FINAL STANDINGS LEADERBOARD */}
      <div className="w-full mb-10 relative z-10">
        <h4 className="text-xs font-black text-indigo-300 uppercase tracking-widest mb-4 text-center flex items-center justify-center gap-1.5">
          <SvgEmoji name="chart" /> Final Match Standings & Leaderboard
        </h4>

        <div className="space-y-3 max-w-2xl mx-auto">
          {sortedCompetitors.map((comp, idx) => {
            const rank = idx + 1;
            const isWinner = comp.score === topScore;
            
            let rankBadge = `${rank}th`;
            let rankBg = 'bg-slate-800/60 border-slate-700 text-slate-400';

            if (rank === 1) {
              rankBadge = <span className="inline-flex items-center gap-1"><SvgEmoji name="medal-gold" /> 1st Place</span>;
              rankBg = 'bg-amber-500/20 border-amber-500/40 text-amber-300 font-bold';
            } else if (rank === 2) {
              rankBadge = <span className="inline-flex items-center gap-1"><SvgEmoji name="medal-silver" /> 2nd Place</span>;
              rankBg = 'bg-slate-400/20 border-slate-400/40 text-slate-200 font-bold';
            } else if (rank === 3) {
              rankBadge = <span className="inline-flex items-center gap-1"><SvgEmoji name="medal-bronze" /> 3rd Place</span>;
              rankBg = 'bg-amber-700/20 border-amber-700/40 text-amber-400 font-bold';
            }

            return (
              <div 
                key={comp.id || idx}
                className={`p-4 rounded-2xl border flex items-center justify-between gap-4 transition-all ${
                  isWinner 
                    ? 'bg-gradient-to-r from-[#1e1b30] to-[#171d33] border-amber-500/50 shadow-lg ring-1 ring-amber-500/30' 
                    : 'bg-[#121624] border-slate-800/80'
                }`}
              >
                <div className="flex items-center gap-4">
                  <span className={`text-xs px-3 py-1 rounded-xl uppercase tracking-wider border ${rankBg}`}>
                    {rankBadge}
                  </span>

                  <div className="w-11 h-11 rounded-xl bg-[#0f121d] border border-slate-700 overflow-hidden flex items-center justify-center shrink-0">
                    {comp.avatar ? (
                      <img src={comp.avatar} alt={comp.name} className="w-full h-full object-cover" />
                    ) : (
                      <span className="text-base text-slate-500 font-bold flex items-center justify-center">
                        <SvgEmoji name="user" />
                      </span>
                    )}
                  </div>

                  <span className="text-sm font-bold text-slate-200">
                    {comp.name}
                  </span>
                </div>

                <span className="text-sm font-black font-mono text-amber-400 bg-amber-500/10 px-3.5 py-1.5 rounded-xl border border-amber-500/20">
                  {comp.score} pts
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* ACTION BUTTONS */}
      <div className="flex flex-wrap justify-center gap-4 relative z-10">
        <button
          onClick={() => onNavigate('settings')}
          className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-xs uppercase tracking-wider px-6 py-3.5 rounded-2xl transition-all shadow-lg flex items-center gap-2 cursor-pointer"
        >
          <SvgEmoji name="refresh" /> Play Again / Configure Match
        </button>
        
        <button
          onClick={() => onNavigate('home')}
          className="bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs uppercase tracking-wider px-6 py-3.5 rounded-2xl transition-all border border-slate-700 flex items-center gap-2 cursor-pointer"
        >
          <SvgEmoji name="home" /> Return to Home
        </button>
      </div>

    </div>
  );
}
