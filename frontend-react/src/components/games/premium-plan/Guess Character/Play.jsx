import React, { useState, useEffect, useRef, useMemo } from 'react';
import colonImg from '../../../../assets/guessthecharacter/colon.png';
import digit0 from '../../../../assets/guessthecharacter/digits/0.png';
import digit1 from '../../../../assets/guessthecharacter/digits/1.png';
import digit2 from '../../../../assets/guessthecharacter/digits/2.png';
import digit3 from '../../../../assets/guessthecharacter/digits/3.png';
import digit4 from '../../../../assets/guessthecharacter/digits/4.png';
import digit5 from '../../../../assets/guessthecharacter/digits/5.png';
import digit6 from '../../../../assets/guessthecharacter/digits/6.png';
import digit7 from '../../../../assets/guessthecharacter/digits/7.png';
import digit8 from '../../../../assets/guessthecharacter/digits/8.png';
import digit9 from '../../../../assets/guessthecharacter/digits/9.png';
import SvgEmoji from '../../../SvgEmoji';

const DIGIT_IMGS = [digit0, digit1, digit2, digit3, digit4, digit5, digit6, digit7, digit8, digit9];

const DEFAULT_CHARACTERS = [
  "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1563089145-599997674d42?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1608889175123-8ee362201f81?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1569003339405-ea396a5a8a90?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1589254065878-42c9da997008?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1535223289827-42f1e9919769?auto=format&fit=crop&w=1000&q=80"
];

export default function GuessCharacterPlay({ profiles = [], setupData = {}, onSelectWinner }) {
  const roundsPerPlayer = setupData?.rounds_per_player || setupData?.roundsPerPlayer || 3;
  const numPlayers = Array.isArray(profiles) && profiles.length > 0 ? profiles.length : 1;
  const totalMatchRounds = numPlayers * roundsPerPlayer;

  const enableTimer = setupData?.enable_timer !== false && setupData?.enableTimer !== false;
  const timerInitialDuration = setupData?.timer_seconds || setupData?.timerSeconds || 30;

  // Normalized Character Pool
  const initialPool = useMemo(() => {
    const rawPool = setupData?.media_pool || setupData?.mediaPool;
    if (Array.isArray(rawPool) && rawPool.length > 0) {
      const valid = rawPool.filter(img => typeof img === 'string' && img.trim().length > 0);
      if (valid.length > 0) return valid;
    }
    return DEFAULT_CHARACTERS;
  }, [setupData]);

  // Session State
  const [workingPool, setWorkingPool] = useState([]);
  const [currentImage, setCurrentImage] = useState('');
  const [isRoundActive, setIsRoundActive] = useState(false); // Rule 1: Hidden before starting turn
  const [roundNumber, setRoundNumber] = useState(1);
  const [isMatchFinished, setIsMatchFinished] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);

  // Timer State
  const [timeRemaining, setTimeRemaining] = useState(timerInitialDuration);
  const [isTimerRunning, setIsTimerRunning] = useState(false);

  const timerIntervalRef = useRef(null);

  // Initialize pool & first character
  useEffect(() => {
    let pool = [...initialPool];
    if (pool.length === 0) pool = [...DEFAULT_CHARACTERS];

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1); // Rule: Non-repeating within game session

    setWorkingPool(pool);
    setCurrentImage(chosen);
    setIsRoundActive(false);
    setRoundNumber(1);
    setIsMatchFinished(false);
    setImageLoaded(false);
    setTimeRemaining(timerInitialDuration);
    setIsTimerRunning(false);
  }, [initialPool, timerInitialDuration]);

  // Timer interval effect
  useEffect(() => {
    if (isTimerRunning && enableTimer) {
      timerIntervalRef.current = setInterval(() => {
        setTimeRemaining((prev) => {
          if (prev <= 1) {
            clearInterval(timerIntervalRef.current);
            setIsTimerRunning(false);
            return 0; // Rule 3: Timer stops at 0, image remains on screen
          }
          return prev - 1;
        });
      }, 1000);
    } else {
      if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    }

    return () => {
      if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    };
  }, [isTimerRunning, enableTimer]);

  // Rule 2: Start Turn
  const handleStartTurn = () => {
    setIsRoundActive(true);
    setImageLoaded(false);
    setTimeRemaining(timerInitialDuration);
    if (enableTimer) {
      setIsTimerRunning(true);
    }
  };

  // Rule 4: Next Image (Manual advance)
  const handleNextImage = () => {
    if (roundNumber >= totalMatchRounds) {
      // Rule 5: Match finished
      setIsMatchFinished(true);
      setIsRoundActive(false);
      setIsTimerRunning(false);
      return;
    }

    let pool = [...workingPool];
    if (pool.length === 0) {
      pool = [...initialPool];
    }
    if (pool.length === 0) {
      pool = [...DEFAULT_CHARACTERS];
    }

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1); // Exclude from pool

    setWorkingPool(pool);
    setCurrentImage(chosen);
    setImageLoaded(false);
    setRoundNumber((prev) => prev + 1);

    // Reset timer & start counting down for next image
    setTimeRemaining(timerInitialDuration);
    if (enableTimer) {
      setIsTimerRunning(true);
    }
  };

  // Digital Timer Display using image assets (0-9 and colon)
  const renderDigitalTimer = () => {
    const minutes = Math.floor(timeRemaining / 60);
    const seconds = timeRemaining % 60;

    const minStr = minutes < 10 ? `0${minutes}` : `${minutes}`;
    const secStr = seconds < 10 ? `0${seconds}` : `${seconds}`;

    const isUrgent = timeRemaining <= 5 && timeRemaining > 0;
    const isTimesUp = timeRemaining === 0;

    return (
      <div className={`flex items-center justify-center gap-1 bg-[#0b0e17]/90 backdrop-blur-md px-5 py-2.5 rounded-2xl border transition-all duration-300 ${
        isTimesUp 
          ? 'border-rose-500 shadow-[0_0_20px_rgba(244,63,94,0.5)] animate-pulse'
          : isUrgent 
          ? 'border-amber-500 shadow-[0_0_15px_rgba(245,158,11,0.4)]'
          : 'border-slate-800 shadow-md'
      }`}>
        {/* Minutes Digits */}
        {minStr.split('').map((ch, idx) => (
          <img
            key={`m-${idx}`}
            src={DIGIT_IMGS[parseInt(ch)] || digit0}
            alt={ch}
            className="h-9 md:h-11 object-contain drop-shadow-[0_0_6px_rgba(99,102,241,0.5)]"
          />
        ))}

        {/* Colon Separator */}
        <img
          src={colonImg}
          alt=":"
          className={`h-7 md:h-9 object-contain mx-0.5 ${isTimerRunning ? 'animate-pulse' : ''}`}
        />

        {/* Seconds Digits */}
        {secStr.split('').map((ch, idx) => (
          <img
            key={`s-${idx}`}
            src={DIGIT_IMGS[parseInt(ch)] || digit0}
            alt={ch}
            className={`h-9 md:h-11 object-contain ${
              isTimesUp ? 'drop-shadow-[0_0_8px_rgba(244,63,94,0.8)]' : 'drop-shadow-[0_0_6px_rgba(99,102,241,0.5)]'
            }`}
          />
        ))}
      </div>
    );
  };

  return (
    <div className="w-full flex flex-col items-center justify-center text-center animate-fadeIn select-none">
      
      {/* HEADER STATUS / BADGES */}
      <div className="mb-4 flex flex-wrap items-center justify-center gap-2">
        <span className="text-xs font-black text-amber-400 uppercase tracking-widest bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 shadow-sm flex items-center gap-1.5">
          <span className="inline-flex items-center gap-1.5"><SvgEmoji name="masks" /> Guess The Character</span>
          <span>•</span>
          <span>Image {Math.min(roundNumber, totalMatchRounds)} of {totalMatchRounds}</span>
        </span>
        <span className="text-[11px] font-bold text-slate-400 bg-slate-800/60 px-3 py-1 rounded-full border border-slate-700/60">
          Unseen Pool: {workingPool.length}
        </span>
      </div>

      {/* ========================================================================= */}
      {/* 1. MATCH COMPLETED SCREEN (When all images/rounds are played)             */}
      {/* ========================================================================= */}
      {isMatchFinished ? (
        <div className="w-full max-w-[680px] h-[380px] bg-[#121624] border-2 border-amber-500/50 rounded-3xl p-8 flex flex-col items-center justify-center gap-4 shadow-2xl animate-fadeIn text-center">
          <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-4xl shadow-inner animate-bounce">
            <SvgEmoji name="trophy" size={40} />
          </div>
          <h3 className="text-2xl font-black text-amber-300 uppercase tracking-wider">
            ALL GUESS THE CHARACTER ROUNDS COMPLETED!
          </h3>
          <p className="text-xs font-bold text-slate-300 max-w-md leading-relaxed">
            All {totalMatchRounds} scheduled character clues have been presented ({numPlayers} competitor(s) × {roundsPerPlayer} round/player). Adjust final scores above or proceed to the next game!
          </p>
          <div className="flex items-center gap-2 text-[11px] font-bold text-slate-400 mt-2 bg-slate-900/80 px-4 py-2 rounded-xl border border-slate-800">
            <span className="inline-flex items-center gap-1.5"><SvgEmoji name="flag" /> Ready for Winner Announcement or Next Minigame</span>
          </div>
        </div>
      ) : !isRoundActive ? (
        /* ========================================================================= */
        /* 2. RULE 1: WAITING SCREEN (Hidden before starting turn)                    */
        /* ========================================================================= */
        <div className="w-full max-w-[680px] h-[380px] bg-[#121624] border-2 border-indigo-500/30 rounded-3xl p-8 flex flex-col items-center justify-center gap-5 shadow-2xl animate-fadeIn text-center relative overflow-hidden group">
          <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/10 via-transparent to-purple-500/10 pointer-events-none" />
          
          <div className="w-20 h-20 rounded-3xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-4xl shadow-inner group-hover:scale-105 transition-transform duration-300">
            <SvgEmoji name="masks" size={40} />
          </div>

          <div className="space-y-1.5 z-10">
            <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">
              Turn {roundNumber} of {totalMatchRounds}
            </span>
            <h3 className="text-xl md:text-2xl font-black text-white uppercase tracking-tight">
              Ready to Guess the Mystery Character?
            </h3>
            <p className="text-xs font-medium text-slate-400 max-w-sm mx-auto leading-relaxed">
              Decide which contestant will play this turn. {enableTimer ? `Timer is set to ${timerInitialDuration} seconds.` : 'Timer is disabled.'} Press start to reveal the character image!
            </p>
          </div>

          <button
            onClick={handleStartTurn}
            className="z-10 px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-wider bg-gradient-to-r from-amber-400 to-yellow-300 hover:from-amber-300 hover:to-yellow-200 text-slate-950 shadow-xl shadow-amber-500/20 active:scale-95 transition-all cursor-pointer flex items-center gap-2 font-mono"
          >
            <span className="inline-flex items-center gap-1.5"><SvgEmoji name="rocket" /> Start Turn</span>
          </button>
        </div>
      ) : (
        /* ========================================================================= */
        /* 3. ACTIVE ROUND SCREEN: Fixed Image Viewport + Digital Timer + Controls    */
        /* ========================================================================= */
        <div className="w-full flex flex-col items-center gap-4">
          
          {/* DIGITAL COUNTDOWN TIMER (OUTSIDE IMAGE CONTAINER) */}
          {enableTimer && (
            <div className="z-20">
              {renderDigitalTimer()}
            </div>
          )}

          {/* IMAGE CONTAINER (SPECIFIC FIXED SIZE: 680px x 380px) */}
          <div className="w-full max-w-[680px] h-[380px] rounded-3xl overflow-hidden relative border-2 border-indigo-500/40 bg-[#090c15] shadow-2xl flex items-center justify-center group">
            
            {/* SKELETON / LOADING INDICATOR */}
            {!imageLoaded && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-[#0d111e] z-10">
                <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-400 rounded-full animate-spin" />
                <span className="text-[10px] font-bold text-slate-400 tracking-wider uppercase">Loading Character...</span>
              </div>
            )}

            {/* CHARACTER IMAGE */}
            <img
              src={currentImage}
              alt={`Character ${roundNumber}`}
              onLoad={() => setImageLoaded(true)}
              onError={(e) => {
                setImageLoaded(true);
                e.target.onerror = null;
                e.target.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="680" height="380" viewBox="0 0 24 24" fill="none" stroke="%236366f1" stroke-width="1.5"><circle cx="12" cy="8" r="4"/><path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2"/></svg>';
              }}
              className={`w-full h-full object-cover transition-opacity duration-300 ${
                imageLoaded ? 'opacity-100' : 'opacity-0'
              }`}
            />

            {/* IMAGE NUMBER / TOTAL PER ROUND BADGE (BOTTOM CENTER) */}
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-2 bg-slate-950/85 backdrop-blur-md px-4 py-1.5 rounded-full border border-white/20 shadow-lg">
              <span className="text-xs font-black text-white tracking-wider">
                <SvgEmoji name="masks" className="mr-1.5 inline" /> {roundNumber} / {totalMatchRounds}
              </span>
            </div>
          </div>

          {/* ACTION BUTTON (RULE 4: NEXT IMAGE MANUAL ADVANCE) */}
          <div className="w-full max-w-[680px]">
            <button
              onClick={handleNextImage}
              className="w-full py-4 px-8 rounded-2xl font-black text-sm uppercase tracking-wider flex items-center justify-center gap-2 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white border border-indigo-400/40 shadow-xl shadow-indigo-500/20 active:scale-95 transition-all cursor-pointer"
            >
              <span className="inline-flex items-center gap-1.5">{roundNumber >= totalMatchRounds ? <><SvgEmoji name="flag" /> Finish Match</> : <>Next Image <SvgEmoji name="arrow-right" /></>}</span>
            </button>
          </div>

        </div>
      )}

    </div>
  );
}