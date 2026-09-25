import React, { useState, useEffect, useMemo, useRef } from 'react';
import SvgEmoji from '../../../SvgEmoji';

// Format year for display (e.g. -450 -> 450 BC, 1969 -> 1969 AD)
const formatYear = (year) => {
  const y = parseInt(year, 10);
  if (isNaN(y)) return '';
  if (y < 0) return `${Math.abs(y)} BC`;
  return `${y} AD`;
};

// Subtle web audio chime for instant feedback
function playChime(isCorrect) {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain);
    gain.connect(ctx.destination);

    if (isCorrect) {
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(523.25, ctx.currentTime); // C5
      osc.frequency.setValueAtTime(659.25, ctx.currentTime + 0.1); // E5
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
      osc.start();
      osc.stop(ctx.currentTime + 0.35);
    } else {
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(220, ctx.currentTime); // A3
      osc.frequency.setValueAtTime(196, ctx.currentTime + 0.1); // G3
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
      osc.start();
      osc.stop(ctx.currentTime + 0.35);
    }
  } catch {
    // AudioContext may be restricted before user gesture
  }
}

export default function TimeLinePlay({ profiles = [], setupData = {}, onSelectWinner }) {
  const activeProfiles = useMemo(() => {
    return Array.isArray(profiles) && profiles.length > 0
      ? profiles
      : [{ id: 'p1', name: 'Player 1' }, { id: 'p2', name: 'Player 2' }];
  }, [profiles]);

  const rawEvents = setupData?.events || [];

  // Match State
  const [currentPlayerIndex, setCurrentPlayerIndex] = useState(0);
  const [timeline, setTimeline] = useState([]);
  const [deck, setDeck] = useState([]);
  const [currentEvent, setCurrentEvent] = useState(null);
  const [lastPlacedId, setLastPlacedId] = useState(null);
  const [turnFeedback, setTurnFeedback] = useState(null); // { correct: bool, message: str, chosenSlot: num }
  const [isCompleted, setIsCompleted] = useState(false);

  const timelineContainerRef = useRef(null);

  // Initialize Match
  const initMatch = React.useCallback(() => {
    let pool = Array.isArray(rawEvents) && rawEvents.length >= 3 ? [...rawEvents] : [];
    if (pool.length < 3) {
      pool = [
        { id: 'ev_1', title: 'Invention of the Wheel', year: -3500, description: 'Earliest records in Mesopotamia' },
        { id: 'ev_2', title: 'Gutenberg Printing Press', year: 1440, description: 'Revolution in printing and bookmaking' },
        { id: 'ev_3', title: 'Apollo 11 Moon Landing', year: 1969, description: 'First humans land on the Moon' },
        { id: 'ev_4', title: 'First Voyage of Columbus', year: 1492, description: 'Arrival in the Americas' },
        { id: 'ev_5', title: 'Discovery of Penicillin', year: 1928, description: 'Dawn of the modern antibiotic era' }
      ];
    }

    // Shuffle pool
    const shuffled = [...pool].sort(() => Math.random() - 0.5);

    // Initial 2 anchors placed on timeline
    const anchors = shuffled.splice(0, 2).sort((a, b) => a.year - b.year);
    const remainingDeck = shuffled;

    setTimeline(anchors);
    setDeck(remainingDeck);
    setCurrentEvent(remainingDeck.length > 0 ? remainingDeck[0] : null);
    setCurrentPlayerIndex(0);
    setLastPlacedId(null);
    setTurnFeedback(null);
    setIsCompleted(false);
  }, [rawEvents]);

  useEffect(() => {
    initMatch();
  }, [initMatch]);

  const activePlayer = activeProfiles[currentPlayerIndex] || activeProfiles[0];

  // Evaluate chosen slot
  const handleSelectSlot = (slotIndex) => {
    if (turnFeedback || !currentEvent || isCompleted) return;

    let isCorrect = false;
    if (slotIndex === 0) {
      isCorrect = currentEvent.year <= timeline[0].year;
    } else if (slotIndex === timeline.length) {
      isCorrect = currentEvent.year >= timeline[timeline.length - 1].year;
    } else {
      const prevYear = timeline[slotIndex - 1].year;
      const nextYear = timeline[slotIndex].year;
      isCorrect = currentEvent.year >= prevYear && currentEvent.year <= nextYear;
    }

    playChime(isCorrect);

    if (isCorrect) {
      setTurnFeedback({
        correct: true,
        message: `CORRECT! ${currentEvent.title} occurred in ${formatYear(currentEvent.year)}. The host can award points in the Arena.`,
        chosenSlot: slotIndex
      });
    } else {
      setTurnFeedback({
        correct: false,
        message: `INCORRECT! ${currentEvent.title} occurred in ${formatYear(currentEvent.year)}.`,
        chosenSlot: slotIndex
      });
    }

    // Insert the card into its true sorted spot on the timeline and highlight it
    const updatedTimeline = [...timeline, currentEvent].sort((a, b) => a.year - b.year);
    setTimeline(updatedTimeline);
    setLastPlacedId(currentEvent.id);

    // Remove current card from deck
    const updatedDeck = deck.slice(1);
    setDeck(updatedDeck);
  };

  // Next Turn Advance
  const handleNextTurn = () => {
    setTurnFeedback(null);

    if (deck.length === 0) {
      // Game finishes when ALL configured milestone elements have been placed!
      setIsCompleted(true);
      if (onSelectWinner) {
        onSelectWinner(activePlayer);
      }
      return;
    }

    // Continuous turn rotation
    const nextPlayerIndex = (currentPlayerIndex + 1) % activeProfiles.length;
    setCurrentPlayerIndex(nextPlayerIndex);
    setCurrentEvent(deck[0] || null);
  };

  return (
    <div className="w-full flex flex-col items-center select-none text-slate-100 py-2">
      {/* Header Banner: Game Title & Milestones Remaining */}
      <div className="w-full max-w-4xl flex items-center justify-between gap-3 bg-[#131726]/80 backdrop-blur-md px-6 py-3 rounded-2xl border border-slate-800 shadow-lg mb-4">
        <div className="flex items-center gap-2">
          <span className="text-xl flex items-center justify-center"><SvgEmoji name="hourglass" /></span>
          <span className="font-black text-amber-400 text-lg uppercase tracking-wider">TimeLine</span>
        </div>
        <span className="text-xs bg-slate-800/80 px-3 py-1.5 rounded-full font-mono text-slate-300 font-bold">
          {deck.length + (currentEvent ? 1 : 0)} milestones remaining
        </span>
      </div>

      {/* Main Game Stage */}
      {!isCompleted ? (
        <div className="w-full max-w-5xl flex flex-col items-center gap-5">
          {/* Mystery Card to Place */}
          {currentEvent && (
            <div className="w-full max-w-lg">
              <div
                className={`relative overflow-hidden rounded-2xl p-5 border-2 transition-all duration-300 shadow-2xl ${
                  turnFeedback
                    ? turnFeedback.correct
                      ? 'bg-emerald-950/80 border-emerald-500 shadow-emerald-500/20 animate-bounce'
                      : 'bg-rose-950/80 border-rose-500 shadow-rose-500/20'
                    : 'bg-gradient-to-br from-[#1c243f] to-[#121626] border-amber-500/60 shadow-amber-500/10'
                }`}
              >
                <div className="flex items-center justify-between gap-2 mb-2">
                  <span className="text-xs text-slate-400 font-bold">Milestone to place:</span>
                  <div
                    className={`font-mono text-base font-black px-3 py-1 rounded-xl border transition-all ${
                      turnFeedback
                        ? 'bg-amber-400 text-slate-950 border-amber-300 scale-110'
                        : 'bg-slate-900/80 text-amber-400 border-amber-500/40 animate-pulse'
                    }`}
                  >
                    {turnFeedback ? formatYear(currentEvent.year) : '??? Hidden Year'}
                  </div>
                </div>

                <h3 className="text-lg md:text-xl font-black text-white mb-2">{currentEvent.title}</h3>
                <p className="text-xs md:text-sm text-slate-300 leading-relaxed">{currentEvent.description}</p>

                {turnFeedback && (
                  <div
                    className={`mt-4 p-3 rounded-xl text-center text-xs md:text-sm font-black border ${
                      turnFeedback.correct
                        ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40'
                        : 'bg-rose-500/20 text-rose-300 border-rose-500/40'
                    }`}
                  >
                    {turnFeedback.message}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Action Prompt / Next Turn Button */}
          {turnFeedback ? (
            <button
              type="button"
              onClick={handleNextTurn}
              className="px-8 py-3 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-black text-sm rounded-2xl shadow-xl shadow-amber-500/20 transition-all active:scale-95 cursor-pointer flex items-center gap-2"
            >
              <span>Continue</span>
              <span className="text-base flex items-center"><SvgEmoji name="arrow-right" /></span>
            </button>
          ) : (
            <div className="text-xs font-bold text-slate-400 bg-slate-900/60 px-4 py-1.5 rounded-full border border-slate-800">
              <SvgEmoji name="arrow-down" className="mr-1 inline" /> Click a slot <span className="text-amber-300 font-black">[ + ]</span> to place the event in history:
            </div>
          )}

          {/* Horizontal Interactive Timeline Axis */}
          <div
            ref={timelineContainerRef}
            className="w-full overflow-x-auto pb-6 pt-3 px-4 flex items-center gap-2 scrollbar-thin scrollbar-thumb-slate-700"
          >
            {/* Slot 0: Before first event */}
            <button
              type="button"
              disabled={!!turnFeedback}
              onClick={() => handleSelectSlot(0)}
              className={`flex-shrink-0 group flex flex-col items-center justify-center p-3 h-36 w-24 rounded-2xl border-2 border-dashed transition-all cursor-pointer ${
                turnFeedback
                  ? 'opacity-40 cursor-not-allowed border-slate-700'
                  : 'border-amber-500/40 hover:border-amber-400 bg-amber-500/5 hover:bg-amber-500/15 hover:scale-105 active:scale-95'
              }`}
              title="Place before first event"
            >
              <div className="w-8 h-8 rounded-full bg-amber-500/20 group-hover:bg-amber-400 group-hover:text-slate-950 text-amber-400 flex items-center justify-center font-black text-lg transition-colors">
                +
              </div>
              <span className="text-[10px] font-black text-amber-300 mt-2 text-center leading-tight">
                Before everything
              </span>
            </button>

            {/* Timeline Milestones with Interleaved Slots */}
            {timeline.map((ev, idx) => {
              const isNewlyPlaced = ev.id === lastPlacedId;

              return (
                <React.Fragment key={ev.id || `${ev.title}_${idx}`}>
                  {/* Milestone Card */}
                  <div className="flex-shrink-0 flex flex-col items-center">
                    <div
                      className={`w-40 sm:w-44 rounded-2xl p-3 shadow-lg flex flex-col justify-between h-40 transition-all duration-300 ${
                        isNewlyPlaced
                          ? 'bg-gradient-to-br from-[#2a2d48] to-[#1a1e36] border-2 border-amber-400 ring-4 ring-amber-400/40 scale-105 shadow-amber-500/30'
                          : 'bg-[#1b2238] border border-slate-700/80 hover:border-amber-500/50'
                      }`}
                    >
                      <div>
                        <div className="flex items-center justify-between mb-1.5">
                          {isNewlyPlaced ? (
                            <span className="px-2 py-0.5 rounded bg-amber-400 text-slate-950 text-[10px] font-black animate-pulse">
                              <SvgEmoji name="sparkles" className="mr-1 inline" /> Just Placed
                            </span>
                          ) : (
                            <span className="text-[10px] text-slate-400 font-bold">Milestone</span>
                          )}
                          <span className="font-mono text-xs font-black text-amber-300 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/30">
                            {formatYear(ev.year)}
                          </span>
                        </div>
                        <h4 className="text-xs font-bold text-white line-clamp-2 leading-snug">{ev.title}</h4>
                      </div>
                      <p className="text-[10px] text-slate-400 line-clamp-3 leading-tight">{ev.description}</p>
                    </div>

                    {/* Dot on Axis */}
                    <div
                      className={`w-4 h-4 rounded-full border-2 border-slate-900 shadow-md -mt-2 z-10 ${
                        isNewlyPlaced ? 'bg-amber-300 ring-2 ring-amber-400 scale-125' : 'bg-amber-400'
                      }`}
                    />
                  </div>

                  {/* Slot between ev[idx] and ev[idx+1] */}
                  <button
                    type="button"
                    disabled={!!turnFeedback}
                    onClick={() => handleSelectSlot(idx + 1)}
                    className={`flex-shrink-0 group flex flex-col items-center justify-center p-3 h-36 w-24 rounded-2xl border-2 border-dashed transition-all cursor-pointer ${
                      turnFeedback
                        ? 'opacity-40 cursor-not-allowed border-slate-700'
                        : 'border-amber-500/40 hover:border-amber-400 bg-amber-500/5 hover:bg-amber-500/15 hover:scale-105 active:scale-95'
                    }`}
                    title="Place in this position"
                  >
                    <div className="w-8 h-8 rounded-full bg-amber-500/20 group-hover:bg-amber-400 group-hover:text-slate-950 text-amber-400 flex items-center justify-center font-black text-lg transition-colors">
                      +
                    </div>
                    <span className="text-[10px] font-black text-amber-300 mt-2 text-center leading-tight">
                      {idx === timeline.length - 1 ? 'After everything' : 'Here'}
                    </span>
                  </button>
                </React.Fragment>
              );
            })}
          </div>
        </div>
      ) : (
        /* Match Finished Summary */
        <div className="w-full max-w-lg bg-gradient-to-br from-[#1e2544] to-[#121626] border-2 border-amber-400/80 rounded-3xl p-8 text-center shadow-2xl animate-fadeIn">
          <span className="text-5xl block mb-3 animate-bounce flex items-center justify-center"><SvgEmoji name="scroll" size={48} /></span>
          <h3 className="text-2xl font-black text-white mb-1">Timeline Completed!</h3>
          <p className="text-xs text-slate-400 mb-6">
            All configured historical milestones have been placed in history successfully.
          </p>

          <button
            type="button"
            onClick={initMatch}
            className="w-full py-3 bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 hover:to-amber-500 text-slate-950 font-black text-sm rounded-xl shadow-lg transition-all active:scale-95 cursor-pointer"
          >
            Play Again ↺
          </button>
        </div>
      )}
    </div>
  );
}