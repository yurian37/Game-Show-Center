import React, { useState, useEffect, useMemo } from 'react';
import SvgEmoji from '../../../SvgEmoji';

const DEFAULT_QUESTIONS = [
  { question: "What is the capital of France?", answer: "Paris" },
  { question: "Which planet is known as the Red Planet?", answer: "Mars" },
  { question: "What is the largest ocean on Earth?", answer: "Pacific Ocean" },
  { question: "Who painted the Mona Lisa?", answer: "Leonardo da Vinci" },
  { question: "What element has the chemical symbol 'O'?", answer: "Oxygen" },
  { question: "Which animal is the largest mammal in the world?", answer: "Blue Whale" },
  { question: "What is the hardest natural substance on Earth?", answer: "Diamond" },
  { question: "In which country can you find the Pyramids of Giza?", answer: "Egypt" },
  { question: "What is the studio that developed Game Show Center?", answer: "YuyiStudio" },
  { question: "How many continents are there on Earth?", answer: "7" },
  { question: "What is the fastest land animal in the world?", answer: "Cheetah" },
  { question: "Which gas do plants absorb during photosynthesis?", answer: "Carbon Dioxide" }
];

export default function TriviaQuizPlay({ profiles = [], setupData = {} }) {
  const roundsPerPlayer = setupData?.rounds_per_player || setupData?.roundsPerPlayer || 3;
  const numPlayers = Array.isArray(profiles) && profiles.length > 0 ? profiles.length : 1;
  const maxQuestions = numPlayers * roundsPerPlayer;

  const initialPool = useMemo(() => {
    const rawPool = setupData?.question_pool || setupData?.questionPool;
    return Array.isArray(rawPool) && rawPool.length > 0 ? rawPool : DEFAULT_QUESTIONS;
  }, [setupData]);

  const [workingPool, setWorkingPool] = useState([]);
  const [currentQuestion, setCurrentQuestion] = useState(null);
  const [showAnswer, setShowAnswer] = useState(false);
  const [questionNumber, setQuestionNumber] = useState(1);
  const [isRoundCompleted, setIsRoundCompleted] = useState(false);

  // Initialize first question once on mount or when initialPool changes
  useEffect(() => {
    let pool = [...initialPool];
    if (pool.length === 0) pool = [...DEFAULT_QUESTIONS];

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1);

    setWorkingPool(pool);
    setCurrentQuestion(chosen);
    setShowAnswer(false);
    setQuestionNumber(1);
    setIsRoundCompleted(false);
  }, [initialPool]);

  const handleNextQuestion = () => {
    if (questionNumber >= maxQuestions) {
      setIsRoundCompleted(true);
      return;
    }

    let pool = [...workingPool];
    if (pool.length === 0) {
      pool = [...initialPool];
    }
    if (pool.length === 0) {
      pool = [...DEFAULT_QUESTIONS];
    }

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1);

    setWorkingPool(pool);
    setCurrentQuestion(chosen);
    setShowAnswer(false);
    setQuestionNumber(prev => prev + 1);
  };

  const handleRestartRoundCycle = () => {
    let pool = [...initialPool];
    if (pool.length === 0) pool = [...DEFAULT_QUESTIONS];

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1);

    setWorkingPool(pool);
    setCurrentQuestion(chosen);
    setShowAnswer(false);
    setQuestionNumber(1);
    setIsRoundCompleted(false);
  };

  return (
    <div className="w-full flex flex-col items-center justify-center p-6 text-center animate-fadeIn max-w-2xl mx-auto">
      
      {/* QUESTION COUNTER & POOL BADGE */}
      <div className="mb-6 flex flex-col items-center gap-2">
        <span className="text-xs font-black text-indigo-400 uppercase tracking-widest bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
          <SvgEmoji name="question" className="mr-1.5 inline" /> Trivia Quiz • Question {Math.min(questionNumber, maxQuestions)} of {maxQuestions} ({roundsPerPlayer} round/player)
        </span>
        <span className="text-[11px] font-bold text-slate-400">
          Unseen Questions in Pool: {workingPool.length} of {initialPool.length}
        </span>
      </div>

      {/* ROUND COMPLETED BANNER OR QUESTION CARD */}
      {isRoundCompleted ? (
        <div className="w-full bg-[#121624] border-2 border-amber-500/50 p-8 rounded-3xl text-center shadow-2xl animate-fadeIn my-4 flex flex-col items-center gap-4">
          <div className="text-4xl flex items-center justify-center"><SvgEmoji name="flag" size={40} /></div>
          <h3 className="text-xl font-black text-amber-300 uppercase tracking-wider">
            TRIVIA MATCH ROUND COMPLETED!
          </h3>
          <p className="text-xs font-bold text-slate-300 leading-relaxed max-w-md">
            All {maxQuestions} questions for this round have been played ({numPlayers} player(s) × {roundsPerPlayer} rounds/player).
          </p>
          <button
            onClick={handleRestartRoundCycle}
            className="mt-2 px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-wider bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-400 hover:to-purple-500 text-white cursor-pointer shadow-xl active:scale-95 transition-all"
          >
            <SvgEmoji name="refresh" className="mr-1.5" /> Start Next Round
          </button>
        </div>
      ) : (
        currentQuestion && (
          <div className="w-full bg-[#121624] border-2 border-indigo-500/30 p-8 rounded-3xl shadow-2xl mb-6 relative overflow-hidden flex flex-col items-center gap-6">
            <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/5 via-transparent to-purple-500/5 pointer-events-none" />

            {/* QUESTION PROMPT */}
            <div className="space-y-2 z-10">
              <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">
                Question
              </span>
              <h3 className="text-xl md:text-2xl font-black text-white leading-relaxed">
                "{currentQuestion.question}"
              </h3>
            </div>

            {/* ANSWER CONTAINER */}
            <div className="w-full max-w-md bg-[#0b0e17] border border-slate-800 p-5 rounded-2xl z-10 transition-all">
              <span className="text-[10px] font-black text-amber-400 uppercase tracking-widest block mb-2">
                Correct Answer
              </span>

              {showAnswer ? (
                <div className="text-lg md:text-xl font-black text-emerald-300 animate-fadeIn bg-emerald-500/10 border border-emerald-500/30 py-3 px-4 rounded-xl">
                  <SvgEmoji name="bulb" className="mr-1.5 inline" /> {currentQuestion.answer}
                </div>
              ) : (
                <div className="py-3 px-4 rounded-xl bg-slate-900 border border-slate-800 text-slate-500 text-sm font-bold flex items-center justify-center gap-2 select-none">
                  <span className="inline-flex items-center gap-1.5"><SvgEmoji name="lock" /> Answer Hidden</span>
                </div>
              )}
            </div>

            {/* ACTION BUTTONS: SHOW ANSWER & NEXT QUESTION */}
            <div className="flex flex-wrap justify-center gap-4 z-10 pt-2 w-full">
              {!showAnswer && (
                <button
                  onClick={() => setShowAnswer(true)}
                  className="flex-1 max-w-xs bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 text-slate-950 font-black text-xs uppercase tracking-wider py-3.5 px-6 rounded-xl shadow-lg transition-all active:scale-95 cursor-pointer"
                >
                  <SvgEmoji name="eye" className="mr-1.5" /> Show Answer
                </button>
              )}

              <button
                onClick={handleNextQuestion}
                className="flex-1 max-w-xs bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-black text-xs uppercase tracking-wider py-3.5 px-6 rounded-xl shadow-lg transition-all active:scale-95 cursor-pointer"
              >
                {questionNumber >= maxQuestions ? <span className="inline-flex items-center gap-1.5">Finish Round <SvgEmoji name="flag" /></span> : <span className="inline-flex items-center gap-1.5">Next Question <SvgEmoji name="arrow-right" /></span>}
              </button>
            </div>
          </div>
        )
      )}

    </div>
  );
}