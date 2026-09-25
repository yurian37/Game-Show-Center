import React, { useState, useEffect, useMemo } from 'react';
import heartFilledImg from '../../../../assets/hangman/heart_filled.png';
import heartEmptyImg from '../../../../assets/hangman/heart_empty.png';
import SvgEmoji from '../../../SvgEmoji';

const DEFAULT_WORD_POOL = ['CHAMPION', 'VICTORY', 'STUDIO', 'ARENA', 'SHOWCASE', 'OFFLINE'];

export default function HangmanPlay({ profiles = [], setupData = {}, _onSelectWinner }) {
  const maxLives = setupData?.lives_per_round || setupData?.livesPerRound || 6;
  const roundsPerPlayer = setupData?.rounds_per_player || setupData?.roundsPerPlayer || 1;
  const numPlayers = Array.isArray(profiles) && profiles.length > 0 ? profiles.length : 1;

  // Total Rounds limit = number of players * roundsPerPlayer
  const totalRounds = numPlayers * roundsPerPlayer;

  const initialPool = useMemo(() => {
    if (Array.isArray(setupData?.wordPool) && setupData.wordPool.length > 0) {
      return setupData.wordPool;
    }
    if (Array.isArray(setupData?.word_pool) && setupData.word_pool.length > 0) {
      return setupData.word_pool;
    }
    return DEFAULT_WORD_POOL;
  }, [setupData]);

  // Active working word pool (cloned)
  const [workingPool, setWorkingPool] = useState([]);
  const [currentRoundNumber, setCurrentRoundNumber] = useState(1);
  const [currentWord, setCurrentWord] = useState('');
  const [guessedLetters, setGuessedLetters] = useState([]);
  const [usedLetters, setUsedLetters] = useState([]);
  const [livesLeft, setLivesLeft] = useState(maxLives);

  const [inputLetter, setInputLetter] = useState('');
  const [inputFullWord, setInputFullWord] = useState('');

  const [gameState, setGameState] = useState('playing'); // 'playing', 'won', 'lost'
  const [endMessage, setEndMessage] = useState('');

  // Initialize round 1 on mount or when setupData changes
  useEffect(() => {
    let pool = [...initialPool];
    if (pool.length === 0) pool = [...DEFAULT_WORD_POOL];

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosenWord = pool[randomIndex].toUpperCase().trim();
    pool.splice(randomIndex, 1);

    setWorkingPool(pool);
    setCurrentRoundNumber(1);
    setCurrentWord(chosenWord);
    setGuessedLetters([]);
    setUsedLetters([]);
    setLivesLeft(maxLives);
    setInputLetter('');
    setInputFullWord('');
    setGameState('playing');
    setEndMessage('');
  }, [initialPool, maxLives]);

  // Advance to next round on button click
  const handleNextRound = () => {
    let pool = [...workingPool];
    if (pool.length === 0) {
      pool = [...initialPool];
    }
    if (pool.length === 0) {
      pool = [...DEFAULT_WORD_POOL];
    }

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosenWord = pool[randomIndex].toUpperCase().trim();
    pool.splice(randomIndex, 1);

    setWorkingPool(pool);
    setCurrentRoundNumber(prev => prev + 1);
    setCurrentWord(chosenWord);
    setGuessedLetters([]);
    setUsedLetters([]);
    setLivesLeft(maxLives);
    setInputLetter('');
    setInputFullWord('');
    setGameState('playing');
    setEndMessage('');
  };

  // Handle Letter Submission
  const handleSubmitLetter = (e) => {
    e.preventDefault();
    if (gameState !== 'playing' || !inputLetter.trim()) return;

    const letter = inputLetter.trim().toUpperCase()[0];
    setInputLetter('');

    // Check if letter was already used
    if (usedLetters.includes(letter)) {
      // Deduct 1 life for repeated letter
      const newLives = livesLeft - 1;
      setLivesLeft(newLives);

      if (newLives <= 0) {
        setGameState('lost');
        setEndMessage(`Out of hearts! The word was "${currentWord}".`);
      }
      return;
    }

    // Register as used letter
    const updatedUsed = [...usedLetters, letter];
    setUsedLetters(updatedUsed);

    // Check if letter is not in word
    if (!currentWord.includes(letter)) {
      // Deduct 1 life for wrong letter
      const newLives = livesLeft - 1;
      setLivesLeft(newLives);

      if (newLives <= 0) {
        setGameState('lost');
        setEndMessage(`Out of hearts! The word was "${currentWord}".`);
      }
      return;
    }

    // Correct new letter
    const updatedGuessed = [...guessedLetters, letter];
    setGuessedLetters(updatedGuessed);

    // Check if entire word is guessed
    const isWordComplete = currentWord.split('').every(ch => updatedGuessed.includes(ch));
    if (isWordComplete) {
      setGameState('won');
      setEndMessage(`CONGRATULATIONS! You solved the word with ${livesLeft} heart(s) remaining!`);
    }
  };

  // Handle Full Word Submission
  const handleSubmitFullWord = (e) => {
    e.preventDefault();
    if (gameState !== 'playing' || !inputFullWord.trim()) return;

    const attemptedWord = inputFullWord.trim().toUpperCase();
    setInputFullWord('');

    if (attemptedWord === currentWord) {
      // WIN: Reveal all letters and mark them as used
      const allLetters = Array.from(new Set(currentWord.split('')));
      setGuessedLetters(allLetters);
      setUsedLetters(prev => Array.from(new Set([...prev, ...allLetters])));
      setGameState('won');
      setEndMessage(`BRILLIANT! You guessed the entire word correctly with ${livesLeft} heart(s) remaining!`);
    } else {
      // LOSS: Lose ALL hearts immediately
      setLivesLeft(0);
      setGameState('lost');
      setEndMessage(`Incorrect full word guess! You lost all hearts. The word was "${currentWord}".`);
    }
  };

  // Render Image Hearts Bar
  const renderHearts = () => {
    const hearts = [];
    for (let i = 0; i < maxLives; i++) {
      const isFilled = i < livesLeft;
      hearts.push(
        <img
          key={i}
          src={isFilled ? heartFilledImg : heartEmptyImg}
          alt={isFilled ? "Filled Heart" : "Empty Heart"}
          className={`w-9 h-9 object-contain transition-all transform hover:scale-110 ${
            isFilled ? 'drop-shadow-[0_0_8px_rgba(244,63,94,0.6)]' : 'opacity-40'
          }`}
        />
      );
    }
    return hearts;
  };

  const isLastRound = currentRoundNumber >= totalRounds;

  return (
    <div className="w-full flex flex-col items-center justify-center p-6 text-center animate-fadeIn max-w-2xl mx-auto">
      
      {/* ROUND COUNTER & HEARTS LIVES BAR */}
      <div className="mb-6 flex flex-col items-center gap-2">
        <span className="text-xs font-black text-indigo-400 uppercase tracking-widest bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
          Round {currentRoundNumber} of {totalRounds} ({roundsPerPlayer} round/player)
        </span>

        <span className="text-[11px] font-black text-rose-400 uppercase tracking-widest mt-1">
          LIVES REMAINING ({livesLeft} / {maxLives})
        </span>

        <div className="flex gap-2 bg-[#121624] px-5 py-3 rounded-2xl border border-slate-800 shadow-inner items-center">
          {renderHearts()}
        </div>
      </div>

      {/* WORD TILES DISPLAY */}
      <div className="flex flex-wrap justify-center gap-3 my-6">
        {currentWord.split('').map((char, idx) => {
          const isRevealed = guessedLetters.includes(char) || gameState !== 'playing';
          return (
            <div 
              key={idx}
              className={`w-12 h-14 rounded-2xl border-2 flex items-center justify-center text-2xl font-black shadow-lg transition-all ${
                isRevealed
                  ? (gameState === 'lost' && !guessedLetters.includes(char)
                      ? 'bg-rose-500/20 border-rose-500 text-rose-300'
                      : 'bg-indigo-600/30 border-indigo-500 text-white shadow-indigo-500/20')
                  : 'bg-[#121624] border-slate-700 text-transparent'
              }`}
            >
              {isRevealed ? char : '_'}
            </div>
          );
        })}
      </div>

      {/* WORD LENGTH BADGE */}
      <span className="text-xs font-bold text-slate-400 mb-5 bg-slate-800/80 px-3 py-1 rounded-full border border-slate-700">
        Word Length: {currentWord.length} letters • Words Remaining in Pool: {workingPool.length}
      </span>

      {/* USED LETTERS LIST DISPLAY */}
      <div className="w-full bg-[#121624] border border-slate-800/90 rounded-2xl p-4 mb-6 shadow-lg flex flex-col items-center gap-2.5 transition-all">
        <div className="flex items-center justify-between w-full px-1">
          <span className="text-xs font-black text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
            <SvgEmoji name="letters-case" />
            <span>Used Letters</span>
          </span>
          <span className="text-[11px] font-black px-2.5 py-0.5 rounded-full bg-slate-800/90 border border-slate-700 text-indigo-300">
            {usedLetters.length}
          </span>
        </div>

        {usedLetters.length === 0 ? (
          <p className="text-xs text-slate-500 font-medium py-2 italic tracking-wide">
            No letters used yet
          </p>
        ) : (
          <div className="flex flex-wrap justify-center gap-2 pt-1 w-full">
            {[...usedLetters].sort().map((letter) => {
              const isCorrect = currentWord.includes(letter);
              return (
                <span
                  key={letter}
                  className={`inline-flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-xl font-black text-xs transition-all shadow-md transform hover:scale-105 ${
                    isCorrect
                      ? 'bg-emerald-500/20 border border-emerald-500/50 text-emerald-300 shadow-emerald-950/40'
                      : 'bg-rose-500/20 border border-rose-500/40 text-rose-300/80 line-through shadow-rose-950/40'
                  }`}
                >
                  <span className="text-sm font-black">{letter}</span>
                  <span className="text-[10px] font-bold no-underline opacity-90">{isCorrect ? <SvgEmoji name="check" /> : <SvgEmoji name="close" />}</span>
                </span>
              );
            })}
          </div>
        )}
      </div>

      {/* INPUT FORM CONTROLS (VISIBLE ONLY WHILE PLAYING) */}
      {gameState === 'playing' && (
        <div className="w-full grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          
          {/* 1. GUESS SINGLE LETTER */}
          <form onSubmit={handleSubmitLetter} className="bg-[#121624] p-4 rounded-2xl border border-slate-800/80 flex flex-col gap-3">
            <label className="text-xs font-black text-indigo-300 uppercase tracking-wider text-left">
              <SvgEmoji name="letters-abc" className="mr-1.5" /> Guess a Single Letter
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={inputLetter}
                onChange={(e) => {
                  const val = e.target.value;
                  setInputLetter(val ? val[val.length - 1].toUpperCase() : '');
                }}
                placeholder="A-Z"
                className="w-16 bg-[#0f121d] border border-slate-700 rounded-xl px-3 py-2 text-center text-lg font-black text-white focus:outline-none focus:border-indigo-500 uppercase"
              />
              <button
                type="submit"
                className="flex-1 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded-xl text-xs uppercase tracking-wider transition-all shadow-md cursor-pointer"
              >
                Submit Letter
              </button>
            </div>
          </form>

          {/* 2. GUESS FULL WORD */}
          <form onSubmit={handleSubmitFullWord} className="bg-[#121624] p-4 rounded-2xl border border-slate-800/80 flex flex-col gap-3">
            <label className="text-xs font-black text-amber-300 uppercase tracking-wider text-left">
              <SvgEmoji name="bulb" className="mr-1.5" /> Solve Entire Word
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={inputFullWord}
                onChange={(e) => setInputFullWord(e.target.value.toUpperCase())}
                placeholder="FULL WORD..."
                className="flex-1 bg-[#0f121d] border border-slate-700 rounded-xl px-4 py-2 text-sm font-bold text-white focus:outline-none focus:border-amber-500 uppercase"
              />
              <button
                type="submit"
                className="bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 text-slate-950 font-black rounded-xl text-xs uppercase tracking-wider transition-all shadow-md px-4 cursor-pointer"
              >
                Solve Word
              </button>
            </div>
          </form>
        </div>
      )}

      {/* GAME OVER / VICTORY BANNER & NEXT ROUND OR GAME COMPLETED BUTTON */}
      {gameState !== 'playing' && (
        <div className={`w-full p-6 rounded-3xl border-2 mb-6 shadow-2xl animate-bounce-short ${
          gameState === 'won'
            ? 'bg-gradient-to-r from-emerald-500/20 via-teal-500/30 to-emerald-500/20 border-emerald-400 text-emerald-200'
            : 'bg-gradient-to-r from-rose-500/20 via-red-500/30 to-rose-500/20 border-rose-400 text-rose-200'
        }`}>
          <div className="text-4xl mb-2 flex items-center justify-center">{gameState === 'won' ? <SvgEmoji name="crown" size={40} /> : <SvgEmoji name="skull" size={40} />}</div>
          <h3 className="text-xl font-black mb-2 uppercase">{gameState === 'won' ? 'ROUND VICTORY!' : 'ROUND OVER'}</h3>
          <p className="text-sm font-bold leading-relaxed mb-6">{endMessage}</p>

          {!isLastRound ? (
            <button
              onClick={handleNextRound}
              className="px-8 py-3.5 rounded-2xl font-black text-sm uppercase tracking-wider transition-all duration-300 shadow-xl bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-400 hover:to-purple-500 text-white cursor-pointer active:scale-95"
            >
              NEXT ROUND
            </button>
          ) : (
            <div className="inline-block bg-slate-900/90 border border-amber-500/40 text-amber-300 px-6 py-3 rounded-2xl font-black text-sm uppercase tracking-widest shadow-lg">
              <SvgEmoji name="flag" className="mr-1.5 inline" /> HANGMAN MATCH COMPLETED! ({totalRounds} of {totalRounds} Rounds Played)
            </div>
          )}
        </div>
      )}

    </div>
  );
}