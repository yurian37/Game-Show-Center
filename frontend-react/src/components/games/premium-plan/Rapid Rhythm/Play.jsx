import React, { useState, useEffect, useMemo } from 'react';
import soundManager from '../../../../services/soundManager';
import SvgEmoji from '../../../SvgEmoji';

const DEFAULT_TRACKS = [
  {
    id: "track_1",
    name: "Synthwave Groove #1",
    audioUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    answer: "SoundHelix Track 1",
    startTime: 0,
    spanTime: 30,
    endTime: 30,
    duration: 372
  },
  {
    id: "track_2",
    name: "Electropop Beat #2",
    audioUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
    answer: "SoundHelix Track 2",
    startTime: 0,
    spanTime: 30,
    endTime: 30,
    duration: 423
  },
  {
    id: "track_3",
    name: "Chill Lounge Anthem #3",
    audioUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
    answer: "SoundHelix Track 3",
    startTime: 0,
    spanTime: 30,
    endTime: 30,
    duration: 340
  },
  {
    id: "track_4",
    name: "Funky Bassline #4",
    audioUrl: "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
    answer: "SoundHelix Track 4",
    startTime: 0,
    spanTime: 30,
    endTime: 30,
    duration: 302
  }
];

export default function RapidRhythmPlay({ profiles = [], setupData = {}, onSelectWinner }) {
  const roundsPerPlayer = setupData?.rounds_per_player || setupData?.roundsPerPlayer || 1;
  const numPlayers = Array.isArray(profiles) && profiles.length > 0 ? profiles.length : 1;
  const totalMatchRounds = numPlayers * roundsPerPlayer;

  const initialPool = useMemo(() => {
    const rawTracks = setupData?.tracks;
    if (Array.isArray(rawTracks) && rawTracks.length > 0) {
      return rawTracks.map((t, idx) => ({
        id: t.id || `track_${idx}`,
        name: t.name || `Track #${idx + 1}`,
        audioUrl: t.audioUrl || t.url || t.src || '',
        answer: t.answer || t.name || `Song #${idx + 1}`,
        startTime: Math.max(0, parseInt(t.startTime) || 0),
        spanTime: Math.max(1, parseInt(t.spanTime) || 30),
        endTime: Math.max(1, parseInt(t.endTime) || ((parseInt(t.startTime) || 0) + (parseInt(t.spanTime) || 30))),
        duration: Math.max(1, parseInt(t.duration) || 30)
      })).filter(t => t.audioUrl && t.audioUrl.trim().length > 0);
    }

    const rawPool = setupData?.media_pool || setupData?.mediaPool;
    if (Array.isArray(rawPool) && rawPool.length > 0) {
      return rawPool.map((url, idx) => ({
        id: `track_${idx}`,
        name: `Track #${idx + 1}`,
        audioUrl: url,
        answer: `Song #${idx + 1}`,
        startTime: 0,
        spanTime: 30,
        endTime: 30,
        duration: 30
      }));
    }
    return [...DEFAULT_TRACKS];
  }, [setupData]);

  const [workingPool, setWorkingPool] = useState([]);
  const [currentTrack, setCurrentTrack] = useState(null);
  const [roundNumber, setRoundNumber] = useState(1);
  const [isMatchFinished, setIsMatchFinished] = useState(false);

  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [volume, setVolume] = useState(0.8);
  const [isAnswerRevealed, setIsAnswerRevealed] = useState(false);
  const [audioError, setAudioError] = useState('');

  useEffect(() => {
    return () => {
      soundManager.stopAll();
    };
  }, []);

  useEffect(() => {
    let pool = [...initialPool];
    if (pool.length === 0) pool = [...DEFAULT_TRACKS];

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1);

    soundManager.stopAll();
    setWorkingPool(pool);
    setCurrentTrack(chosen);
    setRoundNumber(1);
    setIsMatchFinished(false);
    setIsAnswerRevealed(false);
    setIsPlaying(false);
    setCurrentTime(chosen ? (chosen.startTime || 0) : 0);
    setAudioError('');
  }, [initialPool]);

  useEffect(() => {
    if (!currentTrack || !currentTrack.audioUrl) return;

    soundManager.stopAll();
    const start = currentTrack.startTime || 0;
    setCurrentTime(start);
    setIsPlaying(false);
    setIsAnswerRevealed(false);
    setAudioError('');
  }, [currentTrack]);

  const togglePlayPause = () => {
    if (!currentTrack || !currentTrack.audioUrl) return;
    const start = currentTrack.startTime || 0;
    const end = currentTrack.endTime || (start + (currentTrack.spanTime || 30));

    if (isPlaying) {
      soundManager.pause();
      setIsPlaying(false);
    } else {
      setAudioError('');
      soundManager.playTrack(currentTrack.audioUrl, {
        startTime: currentTime >= end || currentTime < start ? start : currentTime,
        endTime: end,
        volume: volume,
        onTimeUpdate: (t) => {
          setCurrentTime(t);
        },
        onEnded: () => {
          setIsPlaying(false);
          setCurrentTime(start);
        },
        onError: (err) => {
          setAudioError(`Audio error: ${err.message || 'Playback failed'}`);
          setIsPlaying(false);
        }
      }).then(() => {
        setIsPlaying(true);
      }).catch((err) => {
        setAudioError(`Audio playback error: ${err.message}`);
        setIsPlaying(false);
      });
    }
  };

  const handleReplay = () => {
    if (!currentTrack || !currentTrack.audioUrl) return;
    const start = currentTrack.startTime || 0;
    const end = currentTrack.endTime || (start + (currentTrack.spanTime || 30));

    setAudioError('');
    setCurrentTime(start);
    soundManager.playTrack(currentTrack.audioUrl, {
      startTime: start,
      endTime: end,
      volume: volume,
      onTimeUpdate: (t) => {
        setCurrentTime(t);
      },
      onEnded: () => {
        setIsPlaying(false);
        setCurrentTime(start);
      },
      onError: (err) => {
        setAudioError(`Audio error: ${err.message || 'Playback failed'}`);
        setIsPlaying(false);
      }
    }).then(() => {
      setIsPlaying(true);
    }).catch((err) => {
      setAudioError(`Replay error: ${err.message}`);
      setIsPlaying(false);
    });
  };

  const handleVolumeChange = (newVol) => {
    const v = parseFloat(newVol);
    setVolume(v);
    soundManager.setVolume(v);
  };

  const handleNextTrack = () => {
    soundManager.stopAll();

    if (roundNumber >= totalMatchRounds) {
      setIsMatchFinished(true);
      setIsPlaying(false);
      return;
    }

    let pool = [...workingPool];
    if (pool.length === 0) {
      pool = [...initialPool];
    }
    if (pool.length === 0) {
      pool = [...DEFAULT_TRACKS];
    }

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1);

    setWorkingPool(pool);
    setCurrentTrack(chosen);
    setRoundNumber(prev => prev + 1);
    setIsAnswerRevealed(false);
  };

  // Calculate playback percentages for progress bar
  const startSec = currentTrack?.startTime || 0;
  const endSec = currentTrack?.endTime || (startSec + (currentTrack?.spanTime || 30));
  const spanSec = Math.max(1, endSec - startSec);
  const elapsedInSpan = Math.max(0, Math.min(spanSec, currentTime - startSec));
  const progressPercent = Math.min(100, Math.max(0, (elapsedInSpan / spanSec) * 100));

  return (
    <div className="w-full flex flex-col items-center justify-center text-center animate-fadeIn select-none text-slate-200">
      
      {/* HEADER STATUS / BADGES */}
      <div className="mb-4 flex flex-wrap items-center justify-center gap-2">
        <span className="text-xs font-black text-amber-400 uppercase tracking-widest bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 shadow-sm flex items-center gap-1.5">
          <span className="inline-flex items-center gap-1.5"><SvgEmoji name="music" /> Rapid Rhythm</span>
          <span>•</span>
          <span>Song {Math.min(roundNumber, totalMatchRounds)} of {totalMatchRounds}</span>
        </span>
        <span className="text-[11px] font-bold text-slate-400 bg-slate-800/60 px-3 py-1 rounded-full border border-slate-700/60">
          Remaining in Pool: {workingPool.length}
        </span>
      </div>

      {/* ========================================================================= */}
      {/* 1. MATCH COMPLETED SCREEN                                                 */}
      {/* ========================================================================= */}
      {isMatchFinished ? (
        <div className="w-full max-w-[680px] min-h-[380px] bg-[#121624] border-2 border-amber-500/50 rounded-3xl p-8 flex flex-col items-center justify-center gap-4 shadow-2xl animate-fadeIn text-center">
          <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-4xl shadow-inner animate-bounce">
            <SvgEmoji name="trophy" size={40} />
          </div>
          <h3 className="text-2xl font-black text-amber-300 uppercase tracking-wider">
            ALL RAPID RHYTHM ROUNDS COMPLETED!
          </h3>
          <p className="text-xs font-bold text-slate-300 max-w-md leading-relaxed">
            All {totalMatchRounds} scheduled song clips have been played ({numPlayers} competitor(s) × {roundsPerPlayer} round/player). Award points above or proceed to the winner stage!
          </p>
          <div className="flex items-center gap-2 text-[11px] font-bold text-slate-400 mt-2 bg-slate-900/80 px-4 py-2 rounded-xl border border-slate-800">
            <span className="inline-flex items-center gap-1.5"><SvgEmoji name="flag" /> Ready for Winner Announcement or Next Minigame</span>
          </div>
        </div>
      ) : (
        /* ========================================================================= */
        /* 2. ACTIVE ROUND PLAY STAGE                                                */
        /* ========================================================================= */
        <div className="w-full max-w-[680px] flex flex-col items-center gap-5">
          
          {/* AUDIO ERROR ALERT */}
          {audioError && (
            <div className="w-full bg-rose-500/20 border border-rose-500/40 text-rose-300 text-xs p-3 rounded-2xl font-bold flex items-center justify-between">
              <span className="inline-flex items-center gap-1.5"><SvgEmoji name="warning" /> {audioError}</span>
              <button
                type="button"
                onClick={() => setAudioError('')}
                className="text-rose-400 hover:text-white font-bold ml-2 cursor-pointer"
              >
                <SvgEmoji name="close" />
              </button>
            </div>
          )}

          {/* ======================================================================= */}
          {/* RULE 1: HIDDEN / REVEALABLE ANSWER CARD                                 */}
          {/* ======================================================================= */}
          <div className="w-full bg-gradient-to-b from-[#181f34] to-[#121624] border-2 border-indigo-500/40 rounded-3xl p-6 shadow-2xl relative overflow-hidden flex flex-col items-center gap-4">
            <div className="absolute inset-0 bg-gradient-to-r from-purple-500/5 via-indigo-500/10 to-amber-500/5 pointer-events-none" />

            <div className="flex items-center justify-between w-full z-10">
              <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest bg-indigo-500/10 px-3 py-1 rounded-lg border border-indigo-500/20">
                Track #{roundNumber} Clue
              </span>
              <span className="text-[10px] font-black text-amber-400 bg-amber-500/10 px-3 py-1 rounded-lg border border-amber-500/20">
                <span className="inline-flex items-center gap-1"><SvgEmoji name="stopwatch" /> {startSec}s <SvgEmoji name="arrow-right" /> {endSec}s ({spanSec}s Clip)</span>
              </span>
            </div>

            {/* MYSTERY / REVEALED ANSWER CONTAINER */}
            <div className={`w-full min-h-[90px] rounded-2xl flex flex-col items-center justify-center p-4 transition-all duration-300 z-10 border ${
              isAnswerRevealed
                ? 'bg-gradient-to-r from-emerald-950/80 to-teal-950/80 border-emerald-400/60 shadow-[0_0_25px_rgba(16,185,129,0.3)]'
                : 'bg-[#0b0e17]/90 border-slate-700/60 shadow-inner'
            }`}>
              {isAnswerRevealed ? (
                <div className="animate-fadeIn space-y-1">
                  <span className="text-[10px] font-black text-emerald-400 uppercase tracking-widest block">
                    <SvgEmoji name="sparkles" className="mr-1.5 inline" /> Correct Answer:
                  </span>
                  <h4 className="text-xl md:text-2xl font-black text-emerald-200 tracking-wide">
                    {currentTrack?.answer || "No answer configured"}
                  </h4>
                </div>
              ) : (
                <div className="flex items-center gap-3">
                  <span className="text-2xl inline-flex items-center justify-center"><SvgEmoji name="lock" /></span>
                  <div className="text-left">
                    <span className="text-base font-black text-slate-300 tracking-widest">
                      ••••••••••••••••••••
                    </span>
                    <span className="text-[10px] text-slate-500 block font-bold uppercase">
                      Answer is currently hidden from contestants
                    </span>
                  </div>
                </div>
              )}
            </div>

            {/* RULE 1 BUTTON: REVEAL / HIDE ANSWER AT ANY MOMENT */}
            <button
              onClick={() => setIsAnswerRevealed(prev => !prev)}
              className={`z-10 px-6 py-2.5 rounded-xl font-black text-xs uppercase tracking-wider transition-all cursor-pointer flex items-center gap-2 border ${
                isAnswerRevealed
                  ? 'bg-emerald-600/30 hover:bg-emerald-600/50 text-emerald-300 border-emerald-500/40'
                  : 'bg-amber-400 hover:bg-yellow-300 text-slate-950 border-yellow-300 shadow-lg shadow-amber-500/20 active:scale-95'
              }`}
            >
              <span className="inline-flex items-center gap-1.5">{isAnswerRevealed ? <><SvgEmoji name="monkey-hide" /> Hide Answer</> : <><SvgEmoji name="eye" /> Reveal Answer</>}</span>
            </button>
          </div>

          {/* ======================================================================= */}
          {/* RULE 2 & 3: AUDIO PLAYER ENGINE & EQUALIZER VISUALIZER                 */}
          {/* ======================================================================= */}
          <div className="w-full bg-[#121624] border border-slate-800 rounded-3xl p-6 shadow-xl flex flex-col gap-5">
            
            {/* ANIMATED SOUND EQUALIZER BARS */}
            <div className="h-16 flex items-end justify-center gap-1.5 px-4 bg-[#090c14] rounded-2xl border border-slate-900 overflow-hidden py-3">
              {[40, 75, 55, 90, 60, 100, 45, 80, 65, 95, 50, 85, 70, 40, 90, 60].map((h, i) => (
                <div
                  key={i}
                  className={`w-2.5 rounded-full transition-all duration-150 ${
                    isPlaying
                      ? 'bg-gradient-to-t from-indigo-500 via-purple-500 to-amber-400 animate-pulse'
                      : 'bg-slate-800 h-2'
                  }`}
                  style={{
                    height: isPlaying ? `${Math.max(15, (h * (i % 2 === 0 ? 0.9 : 1.1)))}%` : '8px',
                    animationDelay: `${i * 70}ms`
                  }}
                />
              ))}
            </div>

            {/* TIMELINE PROGRESS BAR (START TO END TIME) */}
            <div className="space-y-1.5">
              <div className="flex justify-between text-[11px] font-mono font-bold text-slate-400 px-1">
                <span>{Math.round(elapsedInSpan)}s / {spanSec}s</span>
                <span className="text-indigo-400 font-black">Segment: [{startSec}s - {endSec}s]</span>
              </div>

              <div className="w-full h-3 bg-[#090c14] rounded-full overflow-hidden border border-slate-800 relative">
                <div
                  className="h-full bg-gradient-to-r from-indigo-500 via-purple-500 to-amber-400 transition-all duration-100 rounded-full"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>
            </div>

            {/* MAIN PLAYBACK CONTROLS */}
            <div className="flex flex-wrap items-center justify-between gap-4 pt-2 border-t border-slate-800/80">
              
              {/* REPLAY FROM START */}
              <button
                onClick={handleReplay}
                className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 transition-colors cursor-pointer border border-slate-700"
                title="Restart clip from configured start time"
              >
                <span className="inline-flex items-center gap-1.5"><SvgEmoji name="previous" /> Replay ({startSec}s)</span>
              </button>

              {/* BIG PLAY / PAUSE BUTTON */}
              <button
                onClick={togglePlayPause}
                className={`px-8 py-3.5 rounded-2xl font-black text-sm uppercase tracking-wider flex items-center gap-2 transition-all cursor-pointer shadow-xl ${
                  isPlaying
                    ? 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-600/30'
                    : 'bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white shadow-emerald-500/30 active:scale-95'
                }`}
              >
                <span className="inline-flex items-center gap-1.5">{isPlaying ? <><SvgEmoji name="pause" /> Pause Clip</> : <><SvgEmoji name="play" /> Play Clip</>}</span>
              </button>

              {/* VOLUME SLIDER */}
              <div className="flex items-center gap-2 bg-[#090c14] px-3 py-1.5 rounded-xl border border-slate-800">
                <SvgEmoji name="speaker" className="text-xs" />
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.05"
                  value={volume}
                  onChange={(e) => handleVolumeChange(e.target.value)}
                  className="w-20 h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                />
              </div>
            </div>

          </div>

          {/* NEXT TRACK BUTTON */}
          <div className="w-full">
            <button
              onClick={handleNextTrack}
              className="w-full py-4 px-8 rounded-2xl font-black text-sm uppercase tracking-wider flex items-center justify-center gap-2 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white border border-indigo-400/40 shadow-xl shadow-indigo-500/20 active:scale-95 transition-all cursor-pointer"
            >
              <span className="inline-flex items-center gap-1.5">{roundNumber >= totalMatchRounds ? <><SvgEmoji name="flag" /> Finish Match</> : <>Next Song <SvgEmoji name="arrow-right" /></>}</span>
            </button>
          </div>

        </div>
      )}

    </div>
  );
}