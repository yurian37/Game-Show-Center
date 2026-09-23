import React, { useState } from 'react';

export default function HangmanSetup({ value, onChange }) {
  const [newWord, setNewWord] = useState('');

  const handleNumericChange = (field, val) => {
    onChange({
      ...value,
      [field]: Math.max(1, parseInt(val) || 1)
    });
  };

  const handleAddWord = (e) => {
    e.preventDefault();
    const word = newWord.trim().toUpperCase();
    if (!word) return;
    
    const wordPool = value?.word_pool || [];
    if (wordPool.some(w => (typeof w === 'string' ? w.trim().toUpperCase() : '') === word)) {
      alert("This word already exists in the list (duplicates not allowed).");
      return;
    }

    onChange({
      ...value,
      word_pool: [...wordPool, word]
    });
    setNewWord('');
  };

  const handleRemoveWord = (wordToRemove) => {
    const wordPool = value?.word_pool || [];
    onChange({
      ...value,
      word_pool: wordPool.filter(w => w !== wordToRemove)
    });
  };

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      <h3 className="text-lg font-black text-purple-300 mb-2">Hangman Configuration</h3>
      <p className="text-xs text-slate-400 mb-6">
        Configure lives, number of rounds, and manage the hangman word list.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
            Lives per Round (Attempts)
          </label>
          <input
            type="number"
            value={value?.lives_per_round || 6}
            onChange={(e) => handleNumericChange('lives_per_round', e.target.value)}
            className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
            min="1"
            max="12"
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
            Rounds per Player
          </label>
          <input
            type="number"
            value={value?.rounds_per_player || 3}
            onChange={(e) => handleNumericChange('rounds_per_player', e.target.value)}
            className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
            min="1"
            max="10"
          />
        </div>
      </div>

      <div className="border-t border-slate-800/80 pt-6">
        <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-3">
          Word List ({value?.word_pool?.length || 0})
        </label>
        
        <form onSubmit={handleAddWord} className="flex gap-2 mb-4">
          <input
            type="text"
            placeholder="NEW WORD..."
            value={newWord}
            onChange={(e) => setNewWord(e.target.value)}
            className="flex-1 bg-[#121624] border border-slate-800 rounded-xl px-4 py-2 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-600 uppercase"
          />
          <button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold px-5 rounded-xl text-xs uppercase tracking-wider transition-colors"
          >
            Add
          </button>
        </form>

        <div className="flex flex-wrap gap-2 max-h-48 overflow-y-auto bg-[#0f121d] p-4 rounded-xl border border-slate-900">
          {value?.word_pool?.map((word, idx) => (
            <span
              key={idx}
              className="bg-[#1b2238] border border-slate-800 text-slate-300 text-xs px-3 py-1.5 rounded-lg flex items-center gap-2 font-mono font-semibold"
            >
              {word}
              <button
                type="button"
                onClick={() => handleRemoveWord(word)}
                className="text-slate-500 hover:text-rose-400 font-bold transition-colors text-[10px]"
              >
                ✕
              </button>
            </span>
          ))}
          {(!value?.word_pool || value.word_pool.length === 0) && (
            <p className="text-xs text-slate-600 py-2 w-full text-center">No words loaded. Java validation will fail!</p>
          )}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  return {
    game: 'Hangman',
    livesPerRound: value?.lives_per_round || 6,
    roundsPerPlayer: value?.rounds_per_player || 3,
    wordPool: value?.word_pool || []
  };
}
