import React from 'react';
import SvgEmoji from '../../../SvgEmoji';

export default function TopicTakedownSetup({ value, onChange }) {
  const numCategories = Math.min(3, Math.max(1, parseInt(value?.num_categories ?? value?.numCategories ?? 3) || 1));
  const questionsPerCategory = Math.min(4, Math.max(1, parseInt(value?.questions_per_category ?? value?.questionsPerCategory ?? 3) || 1));
  const rawCategories = Array.isArray(value?.categories) ? value.categories : [];

  // Helper to ensure rectangular shape
  const normalizeCategories = (targetNumCats, targetQPerCat, currentCats) => {
    const result = [];
    for (let c = 0; c < targetNumCats; c++) {
      const existingCat = currentCats[c] || {};
      const catName = existingCat.category_name || existingCat.categoryName || `Categoría #${c + 1}`;
      const existingQuestions = Array.isArray(existingCat.questions) ? existingCat.questions : [];

      const questions = [];
      for (let q = 0; q < targetQPerCat; q++) {
        const existingQ = existingQuestions[q] || {};
        questions.push({
          question: existingQ.question || '',
          answer: existingQ.answer || ''
        });
      }

      result.push({
        category_name: catName,
        questions
      });
    }
    return result;
  };

  const handleNumCategoriesChange = (val) => {
    const parsed = Math.min(3, Math.max(1, parseInt(val) || 1));
    const updatedCats = normalizeCategories(parsed, questionsPerCategory, rawCategories);
    onChange({
      ...value,
      num_categories: parsed,
      numCategories: parsed,
      categories: updatedCats
    });
  };

  const handleQuestionsPerCategoryChange = (val) => {
    const parsed = Math.min(4, Math.max(1, parseInt(val) || 1));
    const updatedCats = normalizeCategories(numCategories, parsed, rawCategories);
    onChange({
      ...value,
      questions_per_category: parsed,
      questionsPerCategory: parsed,
      categories: updatedCats
    });
  };

  const handleCategoryNameChange = (catIdx, name) => {
    const updatedCats = rawCategories.length === numCategories
      ? [...rawCategories]
      : normalizeCategories(numCategories, questionsPerCategory, rawCategories);

    updatedCats[catIdx] = {
      ...updatedCats[catIdx],
      category_name: name,
      categoryName: name
    };

    onChange({
      ...value,
      categories: updatedCats
    });
  };

  const handleQuestionChange = (catIdx, qIdx, field, val) => {
    const updatedCats = rawCategories.length === numCategories
      ? [...rawCategories]
      : normalizeCategories(numCategories, questionsPerCategory, rawCategories);

    const questions = [...(updatedCats[catIdx]?.questions || [])];
    while (questions.length < questionsPerCategory) {
      questions.push({ question: '', answer: '' });
    }

    questions[qIdx] = {
      ...questions[qIdx],
      [field]: val
    };

    updatedCats[catIdx] = {
      ...updatedCats[catIdx],
      questions
    };

    onChange({
      ...value,
      categories: updatedCats
    });
  };

  const categories = rawCategories.length === numCategories
    ? rawCategories
    : normalizeCategories(numCategories, questionsPerCategory, rawCategories);

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      {/* Title & Badge */}
      <div className="flex items-center justify-between flex-wrap gap-2 mb-3">
        <h3 className="text-lg font-black text-amber-400 flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5"><SvgEmoji name="target" /> Topic Takedown</span>
          <span className="text-[10px] bg-amber-500/20 text-amber-400 border border-amber-500/30 px-2 py-0.5 rounded-full uppercase font-bold tracking-wider">
            Online Mode (Texto)
          </span>
        </h3>
        <span className="text-xs font-mono text-slate-400 bg-[#121624] px-3 py-1 rounded-lg border border-slate-800">
          Tablero Rectangular: <strong className="text-purple-300">{numCategories} Col.</strong> × <strong className="text-indigo-300">{questionsPerCategory} Filas</strong> ({numCategories * questionsPerCategory} preguntas)
        </span>
      </div>

      <p className="text-xs text-slate-400 mb-6 leading-relaxed">
        Configura el tablero de categorías y preguntas. En el modo online se admiten hasta un máximo de <strong>3 categorías</strong> y <strong>4 preguntas por categoría</strong> (únicamente en formato texto).
      </p>

      {/* Grid Configuration Controls */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6 bg-[#121624] p-4 rounded-xl border border-slate-800/80">
        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2 flex items-center justify-between">
            <span># Categorías (Columnas)</span>
            <span className="text-[10px] text-amber-400 font-normal">Máximo 3</span>
          </label>
          <div className="flex items-center gap-2">
            {[1, 2, 3].map((num) => (
              <button
                key={num}
                type="button"
                onClick={() => handleNumCategoriesChange(num)}
                className={`flex-1 py-2.5 rounded-xl font-black text-xs transition-all ${
                  numCategories === num
                    ? 'bg-gradient-to-r from-purple-600 to-indigo-600 text-white shadow-lg ring-2 ring-purple-400/50'
                    : 'bg-[#1b2238] hover:bg-[#232c48] text-slate-400 border border-slate-800'
                }`}
              >
                {num} {num === 1 ? 'Categoría' : 'Categorías'}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2 flex items-center justify-between">
            <span># Preguntas por Categoría (Filas)</span>
            <span className="text-[10px] text-amber-400 font-normal">Máximo 4</span>
          </label>
          <div className="flex items-center gap-2">
            {[1, 2, 3, 4].map((num) => (
              <button
                key={num}
                type="button"
                onClick={() => handleQuestionsPerCategoryChange(num)}
                className={`flex-1 py-2.5 rounded-xl font-black text-xs transition-all ${
                  questionsPerCategory === num
                    ? 'bg-gradient-to-r from-amber-500 to-orange-500 text-slate-950 shadow-lg ring-2 ring-amber-400/50'
                    : 'bg-[#1b2238] hover:bg-[#232c48] text-slate-400 border border-slate-800'
                }`}
              >
                {num} {num === 1 ? 'Pregunta' : 'Preguntas'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Rectangular Categories and Questions Editor */}
      <div className="space-y-6">
        <h4 className="text-xs font-black text-indigo-300 uppercase tracking-wider flex items-center justify-between">
          <span>Categorías y Preguntas del Tablero</span>
          <span className="text-[11px] text-slate-500 font-normal">Todas las filas deben estar completas</span>
        </h4>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {categories.slice(0, numCategories).map((cat, catIdx) => {
            const catQuestions = Array.isArray(cat.questions) ? cat.questions : [];
            return (
              <div
                key={catIdx}
                className="bg-[#121624] border border-slate-800 rounded-xl p-4 flex flex-col gap-4 shadow-md"
              >
                {/* Category Header */}
                <div className="border-b border-slate-800 pb-3">
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-[10px] font-black uppercase text-purple-400 tracking-wider">
                      Columna #{catIdx + 1}
                    </span>
                    <span className="text-[10px] font-mono text-slate-500">
                      {questionsPerCategory} preguntas
                    </span>
                  </div>
                  <input
                    type="text"
                    value={cat.category_name || cat.categoryName || ''}
                    placeholder={`Nombre Categoría #${catIdx + 1}`}
                    onChange={(e) => handleCategoryNameChange(catIdx, e.target.value)}
                    className="w-full bg-[#1b2238] border border-slate-700/80 rounded-lg px-3 py-1.5 text-xs font-bold text-slate-100 focus:outline-none focus:border-purple-500 placeholder:text-slate-600"
                  />
                </div>

                {/* Questions for this category */}
                <div className="space-y-3 flex-1 overflow-y-auto max-h-[460px] pr-1">
                  {Array.from({ length: questionsPerCategory }).map((_, qIdx) => {
                    const qObj = catQuestions[qIdx] || { question: '', answer: '' };
                    const isFilled = qObj.question?.trim() && qObj.answer?.trim();

                    return (
                      <div
                        key={qIdx}
                        className={`p-3 rounded-lg border text-xs space-y-2 transition-colors ${
                          isFilled
                            ? 'bg-[#181f34] border-slate-800'
                            : 'bg-[#181f34]/50 border-amber-500/30'
                        }`}
                      >
                        <div className="flex items-center justify-between text-[10px] font-bold text-slate-400 uppercase">
                          <span className="text-amber-400">Pregunta #{qIdx + 1}</span>
                          {!isFilled && (
                            <span className="text-amber-400/80 text-[9px] lowercase font-normal">
                              requerida
                            </span>
                          )}
                        </div>

                        <div>
                          <label className="block text-[9px] font-bold text-slate-500 uppercase mb-0.5">
                            Pregunta (Texto)
                          </label>
                          <input
                            type="text"
                            placeholder="Texto de la pregunta..."
                            value={qObj.question || ''}
                            onChange={(e) => handleQuestionChange(catIdx, qIdx, 'question', e.target.value)}
                            className="w-full bg-[#0f121d] border border-slate-800 rounded px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 font-medium placeholder:text-slate-700"
                          />
                        </div>

                        <div>
                          <label className="block text-[9px] font-bold text-slate-500 uppercase mb-0.5">
                            Respuesta Correcta (Texto)
                          </label>
                          <input
                            type="text"
                            placeholder="Respuesta..."
                            value={qObj.answer || ''}
                            onChange={(e) => handleQuestionChange(catIdx, qIdx, 'answer', e.target.value)}
                            className="w-full bg-[#0f121d] border border-slate-800 rounded px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-emerald-500 font-medium placeholder:text-slate-700"
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  const numCategories = Math.min(3, Math.max(1, parseInt(value?.num_categories ?? value?.numCategories ?? 3) || 1));
  const questionsPerCategory = Math.min(4, Math.max(1, parseInt(value?.questions_per_category ?? value?.questionsPerCategory ?? 3) || 1));
  const rawCategories = Array.isArray(value?.categories) ? value.categories : [];

  const categories = [];
  for (let c = 0; c < numCategories; c++) {
    const rawCat = rawCategories[c] || {};
    const catQuestions = Array.isArray(rawCat.questions) ? rawCat.questions : [];

    const questions = [];
    for (let q = 0; q < questionsPerCategory; q++) {
      const rawQ = catQuestions[q] || {};
      questions.push({
        difficulty: q + 1,
        question: (rawQ.question || '').trim(),
        answer: (rawQ.answer || '').trim(),
        imageUrl: null,
        audioUrl: null
      });
    }

    categories.push({
      categoryName: (rawCat.category_name || rawCat.categoryName || `Categoría #${c + 1}`).trim(),
      questions
    });
  }

  return {
    game: 'Topic_Takedown',
    numCategories,
    questionsPerCategory,
    categories
  };
}