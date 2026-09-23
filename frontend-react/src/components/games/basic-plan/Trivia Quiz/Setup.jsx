import React, { useState } from 'react';

export default function TriviaQuizSetup({ value, onChange }) {
  const [newQuestion, setNewQuestion] = useState('');
  const [newAnswer, setNewAnswer] = useState('');

  const handleRoundsChange = (val) => {
    onChange({
      ...value,
      rounds_per_player: Math.max(1, parseInt(val) || 1)
    });
  };

  const handleAddQuestion = (e) => {
    e.preventDefault();
    const q = newQuestion.trim();
    const a = newAnswer.trim();
    if (!q || !a) return;

    const pool = value?.question_pool || [];
    onChange({
      ...value,
      question_pool: [...pool, { question: q, answer: a }]
    });

    setNewQuestion('');
    setNewAnswer('');
  };

  const handleRemoveQuestion = (indexToRemove) => {
    const pool = value?.question_pool || [];
    onChange({
      ...value,
      question_pool: pool.filter((_, idx) => idx !== indexToRemove)
    });
  };

  const handleEditQuestion = (idx, field, val) => {
    const pool = [...(value?.question_pool || [])];
    pool[idx] = {
      ...pool[idx],
      [field]: val
    };
    onChange({
      ...value,
      question_pool: pool
    });
  };

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      <h3 className="text-lg font-black text-purple-300 mb-2">Trivia Quiz Configuration</h3>
      <p className="text-xs text-slate-400 mb-6">
        Modify the number of rounds per player and manage questions and answers.
      </p>

      <div className="w-full max-w-xs mb-6">
        <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
          Rounds per Player
        </label>
        <input
          type="number"
          value={value?.rounds_per_player || 3}
          onChange={(e) => handleRoundsChange(e.target.value)}
          className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
          min="1"
          max="20"
        />
      </div>

      <div className="border-t border-slate-800/80 pt-6">
        <h4 className="text-xs font-black text-indigo-300 uppercase tracking-wider mb-3">
          Add Question to Game
        </h4>
        
        <form onSubmit={handleAddQuestion} className="bg-[#121624] p-4 rounded-xl border border-slate-800/80 space-y-4 mb-6">
          <div>
            <label className="block text-[10px] font-bold text-slate-500 uppercase mb-1">Question Prompt</label>
            <input
              type="text"
              placeholder="e.g. What is the capital of France?"
              value={newQuestion}
              onChange={(e) => setNewQuestion(e.target.value)}
              className="w-full bg-[#0f121d] border border-slate-800 rounded-lg px-3 py-2 text-sm font-semibold focus:outline-none focus:border-indigo-500 text-slate-200 placeholder:text-slate-700"
            />
          </div>
          <div>
            <label className="block text-[10px] font-bold text-slate-500 uppercase mb-1">Correct Answer</label>
            <input
              type="text"
              placeholder="Paris"
              value={newAnswer}
              onChange={(e) => setNewAnswer(e.target.value)}
              className="w-full bg-[#0f121d] border border-slate-800 rounded-lg px-3 py-2 text-sm font-semibold focus:outline-none focus:border-indigo-500 text-slate-200 placeholder:text-slate-700"
            />
          </div>
          <button
            type="submit"
            className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-bold py-2 rounded-xl text-xs uppercase tracking-wider transition-colors"
          >
            Add Question
          </button>
        </form>

        <h4 className="text-xs font-black text-indigo-300 uppercase tracking-wider mb-3">
          Question Pool ({value?.question_pool?.length || 0})
        </h4>

        <div className="space-y-4 max-h-80 overflow-y-auto bg-[#0f121d] p-3 rounded-xl border border-slate-900">
          {value?.question_pool?.map((q, idx) => (
            <div
              key={idx}
              className="bg-[#1b2238] border border-slate-800 p-4 rounded-lg flex flex-col gap-2.5"
            >
              <div className="flex gap-2">
                <span className="text-[10px] font-black text-slate-500 uppercase self-center w-6">Q:</span>
                <input
                  type="text"
                  value={q.question}
                  onChange={(e) => handleEditQuestion(idx, 'question', e.target.value)}
                  className="flex-1 bg-[#0f121d] border border-slate-800 rounded px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 font-semibold"
                />
              </div>
              <div className="flex gap-2">
                <span className="text-[10px] font-black text-indigo-400 uppercase self-center w-6">A:</span>
                <input
                  type="text"
                  value={q.answer}
                  onChange={(e) => handleEditQuestion(idx, 'answer', e.target.value)}
                  className="flex-grow bg-[#0f121d] border border-slate-800 rounded px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 font-semibold"
                />
                <button
                  type="button"
                  onClick={() => handleRemoveQuestion(idx)}
                  className="text-rose-500 hover:text-rose-400 font-bold transition-colors text-xs shrink-0 self-center px-2 uppercase tracking-wide"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
          {(!value?.question_pool || value.question_pool.length === 0) && (
            <p className="text-xs text-slate-600 py-4 text-center">No questions added yet.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  return {
    game: 'Trivia_Quiz',
    roundsPerPlayer: value?.rounds_per_player || 3,
    questionPool: value?.question_pool || []
  };
}
