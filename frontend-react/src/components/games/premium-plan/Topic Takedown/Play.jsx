import React, { useState, useMemo } from 'react';

const FALLBACK_CATEGORIES = [
  {
    category_name: "Ciencia y Naturaleza",
    questions: [
      { question: "¿Cuál es el elemento químico más abundante en el universo?", answer: "Hidrógeno" },
      { question: "¿Qué planeta de nuestro sistema solar tiene el mayor número de lunas?", answer: "Saturno" },
      { question: "¿Cómo se llama el proceso por el cual las plantas convierten la luz solar en energía?", answer: "Fotosíntesis" }
    ]
  },
  {
    category_name: "Historia Universal",
    questions: [
      { question: "¿En qué año llegó el ser humano a la Luna por primera vez?", answer: "1969" },
      { question: "¿Quién fue el primer presidente de los Estados Unidos?", answer: "George Washington" },
      { question: "¿Qué civilización antigua construyó la mítica ciudad de Machu Picchu?", answer: "Los Incas" }
    ]
  },
  {
    category_name: "Cine y Cultura Pop",
    questions: [
      { question: "¿Cómo se llama la inteligencia artificial del traje de Iron Man?", answer: "J.A.R.V.I.S." },
      { question: "¿Qué película ganó el premio Óscar a Mejor Película en 1997 narrando un famoso naufragio?", answer: "Titanic" },
      { question: "¿En qué ficticio país africano transcurre la historia de Black Panther?", answer: "Wakanda" }
    ]
  }
];

export default function TopicTakedownPlay({ profiles = [], setupData = {} }) {
  // Parse categories and rectangular dimensions
  const categories = useMemo(() => {
    const rawCats = setupData?.categories || setupData?.categoryList;
    if (Array.isArray(rawCats) && rawCats.length > 0) {
      return rawCats.map((cat, idx) => ({
        categoryName: cat.categoryName || cat.category_name || `Categoría #${idx + 1}`,
        questions: Array.isArray(cat.questions) ? cat.questions : []
      }));
    }
    return FALLBACK_CATEGORIES.map(c => ({
      categoryName: c.category_name,
      questions: c.questions
    }));
  }, [setupData]);

  const numCategories = categories.length;
  const questionsPerCategory = useMemo(() => {
    if (setupData?.questionsPerCategory || setupData?.questions_per_category) {
      return parseInt(setupData.questionsPerCategory || setupData.questions_per_category) || 3;
    }
    if (categories.length > 0 && categories[0].questions) {
      return categories[0].questions.length;
    }
    return 3;
  }, [categories, setupData]);

  const totalQuestions = numCategories * questionsPerCategory;

  // Track revealed questions: set of "cIdx_qIdx"
  const [revealedSet, setRevealedSet] = useState(new Set());
  // Active modal question or null
  const [activeQuestion, setActiveQuestion] = useState(null);
  // Is answer revealed in current active question view
  const [showAnswerInView, setShowAnswerInView] = useState(false);

  const isQuestionRevealed = (catIdx, qIdx) => {
    return revealedSet.has(`${catIdx}_${qIdx}`);
  };

  const handleOpenQuestion = (catIdx, qIdx) => {
    if (isQuestionRevealed(catIdx, qIdx)) return; // Bloqueada: no puede volver a abrirse

    const cat = categories[catIdx];
    const qData = cat?.questions?.[qIdx] || {
      question: `Pregunta ${qIdx + 1}`,
      answer: "Respuesta no configurada"
    };

    setActiveQuestion({
      catIdx,
      qIdx,
      categoryName: cat?.categoryName || `Categoría #${catIdx + 1}`,
      question: qData.question || 'Sin enunciado',
      answer: qData.answer || 'Sin respuesta',
      imageUrl: qData.imageUrl || qData.image_url || null,
      audioUrl: qData.audioUrl || qData.audio_url || null
    });
    setShowAnswerInView(false);
  };

  const handleRevealAnswer = () => {
    if (!activeQuestion) return;
    setShowAnswerInView(true);

    // Bloquear de forma permanente para esta ronda
    setRevealedSet(prev => {
      const next = new Set(prev);
      next.add(`${activeQuestion.catIdx}_${activeQuestion.qIdx}`);
      return next;
    });
  };

  const handleCloseQuestion = () => {
    setActiveQuestion(null);
    setShowAnswerInView(false);
  };

  const handleRestartRound = () => {
    setRevealedSet(new Set());
    setActiveQuestion(null);
    setShowAnswerInView(false);
  };

  const isAllBoardCleared = revealedSet.size >= totalQuestions && totalQuestions > 0;

  return (
    <div className="w-full flex flex-col items-center justify-center p-4 sm:p-6 text-center animate-fadeIn max-w-5xl mx-auto">
      {/* Top Header & Status */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4 w-full border-b border-slate-800 pb-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center text-xl shadow-lg shadow-orange-500/20">
            🎯
          </div>
          <div className="text-left">
            <h2 className="text-lg font-black text-white tracking-wide">Topic Takedown</h2>
            <p className="text-[11px] font-bold text-slate-400">
              {numCategories} Categorías × {questionsPerCategory} Preguntas
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="px-4 py-1.5 rounded-xl bg-[#121624] border border-slate-800 text-xs font-mono font-bold text-slate-300">
            Preguntas Reveladas: <strong className="text-amber-400">{revealedSet.size}</strong> / {totalQuestions}
          </div>

          <button
            onClick={handleRestartRound}
            title="Reset Board for a new round"
            className="px-3.5 py-1.5 rounded-xl text-xs font-bold bg-[#1b2238] hover:bg-[#252f4c] text-slate-300 hover:text-white border border-slate-700/60 transition-all flex items-center gap-1.5 cursor-pointer"
          >
            <span>🔄</span> Reset Round
          </button>
        </div>
      </div>

      {/* Round Finished Banner */}
      {isAllBoardCleared && (
        <div className="w-full bg-gradient-to-r from-amber-950/40 via-amber-900/20 to-amber-950/40 border-2 border-amber-500/60 p-6 rounded-3xl text-center shadow-2xl animate-fadeIn mb-8 flex flex-col items-center gap-3">
          <div className="text-4xl animate-bounce">🏆</div>
          <h3 className="text-2xl font-black text-amber-300 uppercase tracking-wider">
            BOARD COMPLETED!
          </h3>
          <p className="text-xs font-bold text-slate-300 max-w-md leading-relaxed">
            All {totalQuestions} questions on the board have been revealed this round.
          </p>
          <button
            onClick={handleRestartRound}
            className="mt-2 px-8 py-3 rounded-2xl font-black text-xs uppercase tracking-wider bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 text-slate-950 cursor-pointer shadow-xl active:scale-95 transition-all"
          >
            🔄 Play New Round
          </button>
        </div>
      )}

      {/* BOARD: Columns (Categories) x Rows (Questions) */}
      <div
        className="w-full grid gap-4 sm:gap-6"
        style={{
          gridTemplateColumns: `repeat(${Math.max(1, numCategories)}, minmax(0, 1fr))`
        }}
      >
        {categories.map((cat, catIdx) => (
          <div
            key={catIdx}
            className="flex flex-col gap-3 bg-[#101422]/70 p-3 sm:p-4 rounded-2xl border border-slate-800/80 shadow-xl"
          >
            {/* Category Header Card */}
            <div className="bg-gradient-to-br from-indigo-900/60 to-purple-900/60 border border-indigo-500/30 rounded-xl p-3 sm:p-4 min-h-[72px] flex items-center justify-center text-center shadow-md">
              <h4 className="text-xs sm:text-sm font-black text-indigo-100 uppercase tracking-wide break-words">
                {cat.categoryName}
              </h4>
            </div>

            {/* Questions for this category */}
            <div className="flex flex-col gap-2.5">
              {Array.from({ length: questionsPerCategory }).map((_, qIdx) => {
                const isRevealed = isQuestionRevealed(catIdx, qIdx);
                const questionNumber = qIdx + 1;

                return (
                  <button
                    key={qIdx}
                    disabled={isRevealed}
                    onClick={() => handleOpenQuestion(catIdx, qIdx)}
                    className={`relative w-full h-20 sm:h-24 rounded-xl font-black text-sm sm:text-base transition-all duration-200 flex flex-col items-center justify-center gap-1 border-2 select-none ${
                      isRevealed
                        ? 'bg-[#121624]/70 border-slate-800/80 text-slate-500 cursor-not-allowed'
                        : 'bg-gradient-to-b from-[#1c243a] to-[#141b2e] hover:from-[#25304e] hover:to-[#1a233b] border-indigo-500/40 hover:border-amber-400 text-amber-400 hover:text-amber-300 shadow-lg hover:shadow-indigo-500/20 active:scale-95 cursor-pointer'
                    }`}
                  >
                    {isRevealed ? (
                      <>
                        <span className="text-[10px] font-extrabold uppercase text-slate-500 tracking-wider">
                          ✓ Revelada
                        </span>
                        <span className="text-lg sm:text-xl font-black text-slate-600 line-through">
                          {questionNumber}
                        </span>
                      </>
                    ) : (
                      <>
                        <span className="text-[10px] font-extrabold uppercase text-slate-400 tracking-wider">
                          Pregunta
                        </span>
                        <span className="text-lg sm:text-xl font-black text-amber-300">
                          {questionNumber}
                        </span>
                      </>
                    )}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </div>

      {/* QUESTION MODAL (Item B of prompt) */}
      {activeQuestion && (
        <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 animate-fadeIn">
          <div className="w-full max-w-xl bg-[#121624] border-2 border-amber-500/40 rounded-3xl p-6 sm:p-8 shadow-2xl flex flex-col items-center gap-6 relative overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-b from-amber-500/5 via-transparent to-purple-500/5 pointer-events-none" />

            {/* Category & Question Number Badge */}
            <div className="flex flex-col items-center gap-1.5 z-10 text-center">
              <span className="text-[10px] font-black uppercase text-amber-400 tracking-widest bg-amber-500/10 border border-amber-500/30 px-3 py-1 rounded-full">
                {activeQuestion.categoryName} • Pregunta #{activeQuestion.qIdx + 1}
              </span>
            </div>

            {/* Attached Image (if present) */}
            {activeQuestion.imageUrl && (
              <div className="w-full max-h-56 overflow-hidden rounded-2xl border border-slate-800 bg-[#0b0e17] flex items-center justify-center z-10">
                <img
                  src={activeQuestion.imageUrl}
                  alt="Pregunta Clue"
                  className="max-h-56 w-auto object-contain rounded-xl"
                  onError={(e) => { e.currentTarget.style.display = 'none'; }}
                />
              </div>
            )}

            {/* Attached Audio (if present) */}
            {activeQuestion.audioUrl && (
              <div className="w-full bg-[#0b0e17] p-3 rounded-2xl border border-slate-800 z-10">
                <audio controls className="w-full">
                  <source src={activeQuestion.audioUrl} type="audio/mpeg" />
                  Tu navegador no soporta el reproductor de audio.
                </audio>
              </div>
            )}

            {/* Question Text */}
            <div className="space-y-2 z-10 text-center px-2">
              <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">
                Enunciado
              </span>
              <h3 className="text-lg sm:text-2xl font-black text-white leading-relaxed">
                "{activeQuestion.question}"
              </h3>
            </div>

            {/* Hidden / Revealed Answer Section */}
            <div className="w-full max-w-md bg-[#0b0e17] border border-slate-800 p-4 sm:p-5 rounded-2xl z-10 transition-all">
              <span className="text-[10px] font-black text-amber-400 uppercase tracking-widest block mb-2 text-center">
                Respuesta
              </span>

              {showAnswerInView ? (
                <div className="text-base sm:text-lg font-black text-emerald-300 animate-fadeIn bg-emerald-500/10 border border-emerald-500/30 py-3 px-4 rounded-xl text-center">
                  💡 {activeQuestion.answer}
                </div>
              ) : (
                <div className="py-3 px-4 rounded-xl bg-slate-900 border border-slate-800 text-slate-500 text-xs sm:text-sm font-bold flex items-center justify-center gap-2 select-none">
                  <span>🔒 Hidden Answer</span>
                </div>
              )}
            </div>

            {/* Action Buttons: Reveal Answer and Return */}
            <div className="flex flex-wrap justify-center gap-3 sm:gap-4 z-10 pt-2 w-full">
              {!showAnswerInView ? (
                <button
                  type="button"
                  onClick={handleRevealAnswer}
                  className="flex-1 min-w-[140px] max-w-xs bg-gradient-to-r from-amber-500 to-yellow-400 hover:from-amber-400 hover:to-yellow-300 text-slate-950 font-black text-xs uppercase tracking-wider py-3.5 px-6 rounded-xl shadow-lg transition-all active:scale-95 cursor-pointer"
                >
                  👁️ Reveal Answer
                </button>
              ) : (
                <div className="text-[11px] font-bold text-emerald-400/90 py-2">
                  ✓ Answer revealed (this question is now completed)
                </div>
              )}

              <button
                type="button"
                onClick={handleCloseQuestion}
                className="flex-1 min-w-[140px] max-w-xs bg-gradient-to-r from-slate-700 to-slate-800 hover:from-slate-600 hover:to-slate-700 text-white font-black text-xs uppercase tracking-wider py-3.5 px-6 rounded-xl shadow-lg transition-all active:scale-95 cursor-pointer border border-slate-600/50"
              >
                ↩ Return
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
