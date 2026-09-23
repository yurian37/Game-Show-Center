import React, { useState, useEffect, useRef } from 'react';
import colonImg from '../../../../assets/zeromargin/colon.png';
import digit0 from '../../../../assets/zeromargin/digits/0.png';
import digit1 from '../../../../assets/zeromargin/digits/1.png';
import digit2 from '../../../../assets/zeromargin/digits/2.png';
import digit3 from '../../../../assets/zeromargin/digits/3.png';
import digit4 from '../../../../assets/zeromargin/digits/4.png';
import digit5 from '../../../../assets/zeromargin/digits/5.png';
import digit6 from '../../../../assets/zeromargin/digits/6.png';
import digit7 from '../../../../assets/zeromargin/digits/7.png';
import digit8 from '../../../../assets/zeromargin/digits/8.png';
import digit9 from '../../../../assets/zeromargin/digits/9.png';

const DIGIT_IMGS = [digit0, digit1, digit2, digit3, digit4, digit5, digit6, digit7, digit8, digit9];
const DEFAULT_POOL = [5.0, 10.0, 15.0, 7.5, 20.0];

// Pick random target time with up to 3 attempts to avoid repeating the last time
function pickTargetTime(pool, lastTime) {
  const validPool = Array.isArray(pool) && pool.length > 0 ? pool : DEFAULT_POOL;
  let chosen = validPool[Math.floor(Math.random() * validPool.length)];
  for (let i = 0; i < 3; i++) {
    if (chosen !== lastTime) break;
    chosen = validPool[Math.floor(Math.random() * validPool.length)];
  }
  return chosen;
}

export default function ZeroMarginPlay({ profiles = [], setupData = {}, onSelectWinner }) {
  const activeProfiles = Array.isArray(profiles) && profiles.length > 0
    ? profiles
    : [{ id: 'p1', name: 'Player 1' }, { id: 'p2', name: 'Player 2' }];

  const roundsPerPlayer = setupData?.rounds_per_player || setupData?.roundsPerPlayer || 3;
  const rawPool = setupData?.target_times_pool || setupData?.targetTimesPool;
  const targetTimesPool = Array.isArray(rawPool) && rawPool.length > 0 ? rawPool : DEFAULT_POOL;

  const [currentRound, setCurrentRound] = useState(1);
  const [currentPlayerIndex, setCurrentPlayerIndex] = useState(0);
  const [roundTargetTime, setRoundTargetTime] = useState(5.0);
  const [timerState, setTimerState] = useState('idle'); // 'idle', 'running', 'stopped', 'match_completed'
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [stoppedSeconds, setStoppedSeconds] = useState(0);
  const [playerResults, setPlayerResults] = useState([]);
  const [isHiddenMode, setIsHiddenMode] = useState(false);

  const animFrameRef = useRef(null);
  const startTimeRef = useRef(0);

  // Initialize first round
  const startMatch = React.useCallback(() => {
    const firstTarget = pickTargetTime(targetTimesPool, null);
    setRoundTargetTime(firstTarget);
    setCurrentRound(1);
    setCurrentPlayerIndex(0);
    setTimerState('idle');
    setElapsedSeconds(0);
    setStoppedSeconds(0);
    setPlayerResults([]);
  }, [targetTimesPool]);

  const setupKey = JSON.stringify(setupData);

  useEffect(() => {
    startMatch();
  }, [setupKey, startMatch]);

  // Clean up animation frame on unmount
  useEffect(() => {
    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, []);

  // Timer loop
  const updateTimer = () => {
    const now = performance.now();
    const elapsed = (now - startTimeRef.current) / 1000;

    if (elapsed >= 900) { // Max 15 minutes = 900s
      setElapsedSeconds(900);
      handleStopTimer(900);
      return;
    }

    setElapsedSeconds(elapsed);
    animFrameRef.current = requestAnimationFrame(updateTimer);
  };

  const handleStartTimer = () => {
    if (timerState === 'running') return;
    setTimerState('running');
    startTimeRef.current = performance.now();
    animFrameRef.current = requestAnimationFrame(updateTimer);
  };

  const handleStopTimer = (forcedTime = null) => {
    if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    const finalElapsed = forcedTime !== null ? forcedTime : elapsedSeconds;
    setStoppedSeconds(finalElapsed);
    setTimerState('stopped');
  };

  // Keyboard shortcut listener for Enter key (Start Timer / Stop Timer / Next Turn)
  const timerStateRef = useRef(timerState);
  timerStateRef.current = timerState;

  const handleStartTimerRef = useRef();
  handleStartTimerRef.current = handleStartTimer;

  const handleStopTimerRef = useRef();
  handleStopTimerRef.current = handleStopTimer;

  const handleNextTurnRef = useRef();
  handleNextTurnRef.current = () => handleNextTurn();

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        const state = timerStateRef.current;
        if (state === 'idle') {
          handleStartTimerRef.current?.();
        } else if (state === 'running') {
          handleStopTimerRef.current?.();
        } else if (state === 'stopped') {
          handleNextTurnRef.current?.();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, []);

  const handleNextTurn = () => {
    const currentPlayer = activeProfiles[currentPlayerIndex];
    const diff = stoppedSeconds - roundTargetTime;

    const newResults = [
      ...playerResults,
      {
        round: currentRound,
        player: currentPlayer,
        target: roundTargetTime,
        stopped: stoppedSeconds,
        diff: diff,
        absDiff: Math.abs(diff)
      }
    ];
    setPlayerResults(newResults);

    // Check if more players in current round
    if (currentPlayerIndex < activeProfiles.length - 1) {
      setCurrentPlayerIndex(prev => prev + 1);
      setTimerState('idle');
      setElapsedSeconds(0);
    } else {
      // Round completed for all players
      if (currentRound < roundsPerPlayer) {
        const nextRoundNum = currentRound + 1;
        const nextTarget = pickTargetTime(targetTimesPool, roundTargetTime);
        setCurrentRound(nextRoundNum);
        setCurrentPlayerIndex(0);
        setRoundTargetTime(nextTarget);
        setTimerState('idle');
        setElapsedSeconds(0);
      } else {
        // MATCH COMPLETED
        setTimerState('match_completed');

        // Determine winner (lowest average absolute error diff)
        const playerStats = {};
        newResults.forEach(res => {
          const pId = res.player.id || res.player.name;
          if (!playerStats[pId]) {
            playerStats[pId] = { player: res.player, totalAbsDiff: 0, count: 0 };
          }
          playerStats[pId].totalAbsDiff += res.absDiff;
          playerStats[pId].count += 1;
        });

        let bestPlayer = activeProfiles[0];
        let lowestAvg = Infinity;

        Object.values(playerStats).forEach(stat => {
          const avg = stat.totalAbsDiff / stat.count;
          if (avg < lowestAvg) {
            lowestAvg = avg;
            bestPlayer = stat.player;
          }
        });

        if (onSelectWinner) {
          onSelectWinner(bestPlayer);
        }
      }
    }
  };

  const activePlayer = activeProfiles[currentPlayerIndex] || activeProfiles[0];

  // Helper to format time into digit assets SS:CC or Hidden mode
  const renderDigitalDisplay = (timeInSec) => {
    if (isHiddenMode && timerState === 'running') {
      return (
        <div className="flex items-center justify-center gap-2 bg-[#0b0e17] px-8 py-5 rounded-2xl border border-amber-500/40 shadow-inner my-2 font-mono">
          <span className="text-xl md:text-2xl font-black text-amber-400 animate-pulse flex items-center gap-2">
            🙈 HIDDEN TIMER (Running...)
          </span>
        </div>
      );
    }

    const sec = Math.floor(timeInSec);
    const centis = Math.floor((timeInSec - sec) * 100);

    const secStr = sec < 10 ? `0${sec}` : `${sec}`;
    const csStr = centis < 10 ? `0${centis}` : `${centis}`;

    return (
      <div className="flex items-center justify-center gap-1 bg-[#0b0e17] px-6 py-4 rounded-2xl border border-slate-800 shadow-inner my-2">
        {/* Seconds Digits */}
        {secStr.split('').map((ch, idx) => (
          <img
            key={`s-${idx}`}
            src={DIGIT_IMGS[parseInt(ch)] || digit0}
            alt={ch}
            className="h-12 md:h-14 object-contain drop-shadow-[0_0_8px_rgba(99,102,241,0.5)]"
          />
        ))}

        {/* Colon Separator */}
        <img
          src={colonImg}
          alt=":"
          className="h-10 md:h-12 object-contain mx-1 animate-pulse"
        />

        {/* Centiseconds Digits */}
        {csStr.split('').map((ch, idx) => (
          <img
            key={`cs-${idx}`}
            src={DIGIT_IMGS[parseInt(ch)] || digit0}
            alt={ch}
            className="h-12 md:h-14 object-contain drop-shadow-[0_0_8px_rgba(244,63,94,0.5)]"
          />
        ))}
      </div>
    );
  };

  const currentDiff = stoppedSeconds - roundTargetTime;
  const absDiff = Math.abs(currentDiff);
  const isTooSlow = currentDiff > 0;

  return (
    <div className="w-full flex flex-col items-center justify-center p-6 text-center animate-fadeIn max-w-2xl mx-auto">
      
      {/* ROUND & PLAYER BADGES + HIDDEN TIMER CHECKBOX */}
      <div className="mb-6 flex flex-col items-center gap-3">
        <span className="text-xs font-black text-indigo-400 uppercase tracking-widest bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
          ⏱️ Zero Margin • Round {currentRound} of {roundsPerPlayer}
        </span>
        
        <div className="flex flex-wrap items-center justify-center gap-3">
          <span className="text-xs font-black uppercase text-amber-300 bg-amber-500/10 px-4 py-1.5 rounded-xl border border-amber-500/20">
            Active Turn: <strong className="text-white font-extrabold">{activePlayer.name}</strong> ({currentPlayerIndex + 1} of {activeProfiles.length})
          </span>

          <label className="flex items-center gap-2 bg-[#121624] px-3.5 py-1.5 rounded-xl border border-slate-800 cursor-pointer text-xs font-black text-slate-300 hover:border-indigo-500/50 transition-all select-none">
            <input
              type="checkbox"
              checked={isHiddenMode}
              onChange={(e) => setIsHiddenMode(e.target.checked)}
              className="w-4 h-4 accent-indigo-500 rounded cursor-pointer"
            />
            <span>🙈 Hidden</span>
          </label>
        </div>
      </div>

      {/* MATCH COMPLETED VIEW */}
      {timerState === 'match_completed' ? (
        <div className="w-full bg-[#121624] border-2 border-amber-500/50 p-8 rounded-3xl text-center shadow-2xl animate-fadeIn my-4 flex flex-col items-center gap-4">
          <div className="text-4xl">🏁</div>
          <h3 className="text-xl font-black text-amber-300 uppercase tracking-wider">
            ZERO MARGIN MATCH COMPLETED!
          </h3>
          <p className="text-xs font-bold text-slate-300 leading-relaxed max-w-md">
            All {roundsPerPlayer} rounds for all {activeProfiles.length} competitors have been completed.
          </p>

          {/* RESULTS SUMMARY TABLE */}
          <div className="w-full max-h-48 overflow-y-auto bg-[#0b0e17] p-3 rounded-2xl border border-slate-800 my-2 space-y-2 text-xs">
            {playerResults.map((res, idx) => (
              <div key={idx} className="flex justify-between items-center bg-[#141929] px-3 py-2 rounded-xl border border-slate-800/60 font-mono">
                <span className="text-indigo-300 font-bold">R{res.round} • {res.player.name}</span>
                <span className="text-slate-400">Target: {res.target.toFixed(2)}s | Stopped: {res.stopped.toFixed(2)}s</span>
                <span className={res.absDiff < 0.3 ? 'text-emerald-400 font-black' : 'text-rose-400 font-black'}>
                  {res.diff >= 0 ? `+${res.diff.toFixed(2)}s` : `${res.diff.toFixed(2)}s`}
                </span>
              </div>
            ))}
          </div>

          <button
            onClick={startMatch}
            className="mt-2 px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-wider bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-400 hover:to-purple-500 text-white cursor-pointer shadow-xl active:scale-95 transition-all"
          >
            🔄 Start Next Round
          </button>
        </div>
      ) : (
        /* MAIN GAME CARD */
        <div className="w-full bg-[#121624] border-2 border-indigo-500/30 p-6 md:p-8 rounded-3xl shadow-2xl mb-6 relative overflow-hidden flex flex-col items-center gap-6">
          <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/5 via-transparent to-purple-500/5 pointer-events-none" />

          {/* TARGET TIME DISPLAY */}
          <div className="space-y-1 z-10">
            <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">
              TARGET TIME TO HIT
            </span>
            <div className="text-3xl md:text-4xl font-black text-amber-300 bg-amber-500/10 px-6 py-2 rounded-2xl border border-amber-500/30 inline-block font-mono">
              🎯 {roundTargetTime.toFixed(2)}s
            </div>
          </div>

          {/* DIGITAL TIMER DISPLAY USING DIGIT PNG ASSETS (OR HIDDEN) */}
          <div className="z-10 flex flex-col items-center gap-1">
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest">
              Stopwatch (Seconds : Centiseconds)
            </span>
            {renderDigitalDisplay(timerState === 'stopped' ? stoppedSeconds : elapsedSeconds)}
          </div>

          {/* STOPPED RESULT / MARGEN DE ERROR */}
          {timerState === 'stopped' && (
            <div className="w-full max-w-md bg-[#0b0e17] border border-slate-800 p-4 rounded-2xl z-10 animate-fadeIn space-y-2">
              <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block">
                Result & Precision Difference
              </span>

              <div className={`text-base md:text-lg font-black py-2.5 px-4 rounded-xl border flex items-center justify-center gap-2 ${
                absDiff < 0.2
                  ? 'bg-emerald-500/20 border-emerald-400 text-emerald-300'
                  : absDiff < 0.6
                    ? 'bg-amber-500/20 border-amber-400 text-amber-300'
                    : 'bg-rose-500/20 border-rose-400 text-rose-300'
              }`}>
                <span>{absDiff < 0.2 ? '🎯 AMAZING PRECISION!' : isTooSlow ? '⌛ TOO SLOW!' : '⚡ TOO FAST!'}</span>
                <span>•</span>
                <span>Missed by {isTooSlow ? `+${currentDiff.toFixed(2)}` : `${currentDiff.toFixed(2)}`}s</span>
              </div>

              <p className="text-[11px] text-slate-400">
                Stopped at <strong className="text-white font-mono">{stoppedSeconds.toFixed(2)}s</strong> (Target: <strong className="text-amber-300 font-mono">{roundTargetTime.toFixed(2)}s</strong>)
              </p>
            </div>
          )}

          {/* TIMER START / STOP / NEXT TURN BUTTONS */}
          <div className="flex flex-wrap justify-center gap-4 z-10 pt-2 w-full">
            {timerState === 'idle' && (
              <button
                onClick={handleStartTimer}
                className="w-full max-w-xs bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-white font-black text-sm uppercase tracking-wider py-4 px-8 rounded-2xl shadow-xl transition-all active:scale-95 cursor-pointer flex items-center justify-center gap-2"
              >
                <span>▶️ Start Timer</span>
                <span className="text-[10px] bg-black/30 px-2 py-0.5 rounded-md font-mono">↵ Enter</span>
              </button>
            )}

            {timerState === 'running' && (
              <button
                onClick={() => handleStopTimer()}
                className="w-full max-w-xs bg-gradient-to-r from-rose-600 to-red-500 hover:from-rose-500 hover:to-red-400 text-white font-black text-sm uppercase tracking-wider py-4 px-8 rounded-2xl shadow-xl transition-all active:scale-95 cursor-pointer animate-pulse flex items-center justify-center gap-2"
              >
                <span>⏹️ Stop Timer</span>
                <span className="text-[10px] bg-black/30 px-2 py-0.5 rounded-md font-mono">↵ Enter</span>
              </button>
            )}

            {timerState === 'stopped' && (
              <button
                onClick={handleNextTurn}
                className="w-full max-w-xs bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-black text-xs uppercase tracking-wider py-4 px-8 rounded-2xl shadow-xl transition-all active:scale-95 cursor-pointer flex items-center justify-center gap-2"
              >
                <span>Next Turn ➔</span>
                <span className="text-[10px] bg-black/30 px-2 py-0.5 rounded-md font-mono">↵ Enter</span>
              </button>
            )}
          </div>
        </div>
      )}

    </div>
  );
}
