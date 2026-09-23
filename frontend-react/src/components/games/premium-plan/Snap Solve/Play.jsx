import React, { useState, useEffect, useRef, useMemo } from 'react';

const DEFAULT_POOL = [
  "https://images.unsplash.com/photo-1546182990-dffeafbe841d?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1564349683136-77e08dba1ef6?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1508974239320-0a029497e820?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1563245372-f21724e3856d?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1589254065878-42c9da997008?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1535223289827-42f1e9919769?auto=format&fit=crop&w=1000&q=80",
  "https://images.unsplash.com/photo-1574063413132-355dbfd83e23?auto=format&fit=crop&w=1000&q=80"
];

const FILTER_INFO = {
  displacement: { name: 'Displacement', icon: '〰️' },
  swirl: { name: 'Swirl', icon: '🌪️' },
  pixelate: { name: 'Pixelate', icon: '🧱' },
  blur: { name: 'Blur', icon: '🌫️' }
};

const DIFFICULTY_LABELS = [
  { text: 'Maximum Difficulty', badge: 'bg-rose-500/20 text-rose-400 border-rose-500/30' },
  { text: 'Medium Difficulty', badge: 'bg-amber-500/20 text-amber-400 border-amber-500/30' },
  { text: 'Easy Difficulty', badge: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30' },
  { text: 'Original Image', badge: 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30' }
];

export default function SnapSolvePlay({ profiles = [], setupData = {} }) {
  const roundsPerPlayer = setupData?.rounds_per_player || setupData?.roundsPerPlayer || 2;
  const numPlayers = Array.isArray(profiles) && profiles.length > 0 ? profiles.length : 1;
  const totalMatchRounds = numPlayers * roundsPerPlayer;

  const allowedFilters = useMemo(() => {
    const raw = setupData?.selected_filters || setupData?.selectedFilters;
    if (Array.isArray(raw) && raw.length > 0) return raw;
    return ['displacement', 'swirl', 'pixelate', 'blur'];
  }, [setupData]);

  const initialPool = useMemo(() => {
    const rawPool = setupData?.media_pool || setupData?.mediaPool;
    if (Array.isArray(rawPool) && rawPool.length > 0) {
      const valid = rawPool.filter((img) => typeof img === 'string' && img.trim().length > 0);
      if (valid.length > 0) return valid;
    }
    return DEFAULT_POOL;
  }, [setupData]);

  // Session State
  const [workingPool, setWorkingPool] = useState([]);
  const [currentImage, setCurrentImage] = useState('');
  const [currentFilter, setCurrentFilter] = useState('swirl');
  const [difficulty, setDifficulty] = useState(0); // 0: Max, 1: Med, 2: Easy, 3: Original
  const [isWaiting, setIsWaiting] = useState(true); // Phase 0: Waiting/Selection Pause
  const [roundNumber, setRoundNumber] = useState(1);
  const [isMatchFinished, setIsMatchFinished] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);

  const canvasRef = useRef(null);
  const rawImageRef = useRef(null);

  // Initialize pool
  useEffect(() => {
    setWorkingPool([...initialPool]);
    setRoundNumber(1);
    setIsWaiting(true);
    setIsMatchFinished(false);
  }, [initialPool]);

  // Phase 0 -> Phase 1: Start Turn
  const handleStartTurn = () => {
    let pool = [...workingPool];
    if (pool.length === 0) pool = [...initialPool];
    if (pool.length === 0) pool = [...DEFAULT_POOL];

    const randomImgIdx = Math.floor(Math.random() * pool.length);
    const chosenImg = pool[randomImgIdx];
    pool.splice(randomImgIdx, 1);

    // Pick 1 random filter from allowed filters
    const randomFilter = allowedFilters[Math.floor(Math.random() * allowedFilters.length)] || 'swirl';

    setWorkingPool(pool);
    setCurrentImage(chosenImg);
    setCurrentFilter(randomFilter);
    setDifficulty(0); // Start at Maximum difficulty
    setImageLoaded(false);
    setIsWaiting(false);
  };

  // Phase 3 & 4: Continue Guessing -> Reduce difficulty (0 -> 1 -> 2 -> 3)
  const handleContinueGuessing = () => {
    if (difficulty < 3) {
      setDifficulty((prev) => prev + 1);
    }
  };

  // Phase 6: Next Image
  const handleNextImage = () => {
    if (roundNumber >= totalMatchRounds) {
      setIsMatchFinished(true);
      setIsWaiting(false);
      return;
    }

    setRoundNumber((prev) => prev + 1);
    setIsWaiting(true); // Return to Phase 0 for next player selection
  };

  // Render filter on Canvas whenever currentImage, currentFilter, or difficulty changes
  useEffect(() => {
    if (isWaiting || isMatchFinished || !currentImage) return;

    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.src = currentImage;

    img.onload = () => {
      rawImageRef.current = img;
      setImageLoaded(true);
      applyCanvasFilter(img, currentFilter, difficulty);
    };

    img.onerror = () => {
      setImageLoaded(true);
      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        ctx.fillStyle = '#1e293b';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#94a3b8';
        ctx.font = '14px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('Unable to load remote image', canvas.width / 2, canvas.height / 2);
      }
    };
  }, [currentImage, currentFilter, difficulty, isWaiting, isMatchFinished]);

  const applyCanvasFilter = (img, filterType, diffLevel) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);

    // If Original (diffLevel === 3), draw clean image
    if (diffLevel >= 3) {
      ctx.filter = 'none';
      ctx.imageSmoothingEnabled = true;
      drawImageCover(ctx, img, width, height);
      return;
    }

    if (filterType === 'blur') {
      // Blur filter
      const blurRadius = diffLevel === 0 ? 30 : diffLevel === 1 ? 16 : 6;
      ctx.filter = `blur(${blurRadius}px)`;
      drawImageCover(ctx, img, width, height);
      ctx.filter = 'none';
    } else if (filterType === 'pixelate') {
      // Pixelate filter
      const blockSize = diffLevel === 0 ? 38 : diffLevel === 1 ? 20 : 9;
      const offW = Math.max(1, Math.floor(width / blockSize));
      const offH = Math.max(1, Math.floor(height / blockSize));

      const offCanvas = document.createElement('canvas');
      offCanvas.width = offW;
      offCanvas.height = offH;
      const offCtx = offCanvas.getContext('2d');
      drawImageCover(offCtx, img, offW, offH);

      ctx.imageSmoothingEnabled = false;
      ctx.drawImage(offCanvas, 0, 0, offW, offH, 0, 0, width, height);
      ctx.imageSmoothingEnabled = true;
    } else if (filterType === 'swirl') {
      // Swirl filter
      drawImageCover(ctx, img, width, height);
      const imgData = ctx.getImageData(0, 0, width, height);
      const srcPixels = new Uint32Array(imgData.data.buffer);
      const dstData = ctx.createImageData(width, height);
      const dstPixels = new Uint32Array(dstData.data.buffer);

      const maxAngle = diffLevel === 0 ? 4.5 * Math.PI : diffLevel === 1 ? 2.3 * Math.PI : 0.9 * Math.PI;
      const cx = width / 2;
      const cy = height / 2;
      const radius = Math.min(width, height) / 1.8;
      const radiusSq = radius * radius;

      for (let y = 0; y < height; y++) {
        const dy = y - cy;
        const dySq = dy * dy;
        for (let x = 0; x < width; x++) {
          const dx = x - cx;
          const distSq = dx * dx + dySq;

          if (distSq < radiusSq) {
            const dist = Math.sqrt(distSq);
            const factor = (1 - dist / radius);
            const angle = Math.atan2(dy, dx) + maxAngle * factor * factor;
            const srcX = Math.round(cx + dist * Math.cos(angle));
            const srcY = Math.round(cy + dist * Math.sin(angle));

            if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
              dstPixels[y * width + x] = srcPixels[srcY * width + srcX];
            } else {
              dstPixels[y * width + x] = srcPixels[y * width + x];
            }
          } else {
            dstPixels[y * width + x] = srcPixels[y * width + x];
          }
        }
      }
      ctx.putImageData(dstData, 0, 0);
    } else if (filterType === 'displacement') {
      // Displacement / Wave ripple filter
      drawImageCover(ctx, img, width, height);
      const imgData = ctx.getImageData(0, 0, width, height);
      const srcPixels = new Uint32Array(imgData.data.buffer);
      const dstData = ctx.createImageData(width, height);
      const dstPixels = new Uint32Array(dstData.data.buffer);

      const amplitude = diffLevel === 0 ? 42 : diffLevel === 1 ? 22 : 8;
      const period = diffLevel === 0 ? 18 : diffLevel === 1 ? 30 : 50;

      for (let y = 0; y < height; y++) {
        for (let x = 0; x < width; x++) {
          const shiftX = Math.round(amplitude * Math.sin((y / period) * Math.PI * 2));
          const shiftY = Math.round((amplitude * 0.7) * Math.cos((x / period) * Math.PI * 2));

          const srcX = Math.min(width - 1, Math.max(0, x + shiftX));
          const srcY = Math.min(height - 1, Math.max(0, y + shiftY));

          dstPixels[y * width + x] = srcPixels[srcY * width + srcX];
        }
      }
      ctx.putImageData(dstData, 0, 0);
    }
  };

  const drawImageCover = (targetCtx, image, canvasW, canvasH) => {
    const imgRatio = image.width / image.height;
    const canRatio = canvasW / canvasH;
    let renderW, renderH, offsetX, offsetY;

    if (imgRatio > canRatio) {
      renderH = canvasH;
      renderW = image.width * (canvasH / image.height);
      offsetX = (canvasW - renderW) / 2;
      offsetY = 0;
    } else {
      renderW = canvasW;
      renderH = image.height * (canvasW / image.width);
      offsetX = 0;
      offsetY = (canvasH - renderH) / 2;
    }
    targetCtx.drawImage(image, offsetX, offsetY, renderW, renderH);
  };

  const currentFilterObj = FILTER_INFO[currentFilter] || { name: 'Distortion', icon: '⚡' };
  const currentDiffObj = DIFFICULTY_LABELS[difficulty] || DIFFICULTY_LABELS[0];

  return (
    <div className="w-full flex flex-col items-center justify-center text-center animate-fadeIn select-none">
      
      {/* HEADER STATUS / BADGES */}
      <div className="mb-4 flex flex-wrap items-center justify-center gap-2">
        <span className="text-xs font-black text-amber-400 uppercase tracking-widest bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 shadow-sm flex items-center gap-1.5">
          <span>⚡ Snap Solve</span>
          <span>•</span>
          <span>Round {Math.min(roundNumber, totalMatchRounds)} of {totalMatchRounds}</span>
        </span>
        <span className="text-[11px] font-bold text-slate-400 bg-slate-800/60 px-3 py-1 rounded-full border border-slate-700/60">
          Remaining Pool: {workingPool.length}
        </span>
      </div>

      {/* ========================================================================= */}
      {/* 1. MATCH FINISHED SCREEN                                                  */}
      {/* ========================================================================= */}
      {isMatchFinished ? (
        <div className="w-full max-w-[680px] h-[380px] bg-[#121624] border-2 border-amber-500/50 rounded-3xl p-8 flex flex-col items-center justify-center gap-4 shadow-2xl animate-fadeIn text-center">
          <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-4xl shadow-inner animate-bounce">
            🏆
          </div>
          <h3 className="text-2xl font-black text-amber-300 uppercase tracking-wider">
            ALL SNAP SOLVE ROUNDS COMPLETED!
          </h3>
          <p className="text-xs font-bold text-slate-300 max-w-md leading-relaxed">
            All {totalMatchRounds} visual acuity clues have been presented ({numPlayers} contestant(s) × {roundsPerPlayer} round/player). Adjust final scores above or proceed to the next game!
          </p>
          <div className="flex items-center gap-2 text-[11px] font-bold text-slate-400 mt-2 bg-slate-900/80 px-4 py-2 rounded-xl border border-slate-800">
            <span>🏁 Ready for Winner Announcement</span>
          </div>
        </div>
      ) : isWaiting ? (
        /* ========================================================================= */
        /* 2. PHASE 0: WAITING SCREEN (Pause / Player Selection)                     */
        /* ========================================================================= */
        <div className="w-full max-w-[680px] h-[380px] bg-[#121624] border-2 border-indigo-500/30 rounded-3xl p-8 flex flex-col items-center justify-center gap-5 shadow-2xl animate-fadeIn text-center relative overflow-hidden group">
          <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/10 via-transparent to-purple-500/10 pointer-events-none" />
          
          <div className="w-20 h-20 rounded-3xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-4xl shadow-inner group-hover:scale-105 transition-transform duration-300">
            ⚡
          </div>

          <div className="space-y-1.5 z-10">
            <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">
              Turn {roundNumber} of {totalMatchRounds}
            </span>
            <h3 className="text-xl md:text-2xl font-black text-white uppercase tracking-tight">
              Ready to Decode the Image?
            </h3>
            <p className="text-xs font-medium text-slate-400 max-w-sm mx-auto leading-relaxed">
              Select which contestant will take this turn. The image will start at its maximum distortion. Click the button when ready!
            </p>
          </div>

          <button
            onClick={handleStartTurn}
            className="z-10 px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-wider bg-gradient-to-r from-amber-400 to-yellow-300 hover:from-amber-300 hover:to-yellow-200 text-slate-950 shadow-xl shadow-amber-500/20 active:scale-95 transition-all cursor-pointer flex items-center gap-2 font-mono"
          >
            <span>🚀 Start Turn</span>
          </button>
        </div>
      ) : (
        /* ========================================================================= */
        /* 3. ACTIVE ROUND SCREEN: Canvas Viewport + Progressive Controls             */
        /* ========================================================================= */
        <div className="w-full flex flex-col items-center gap-4">
          
          {/* FILTER & DIFFICULTY STATUS BAR */}
          <div className="flex flex-wrap items-center justify-center gap-2 z-20">
            <span className="text-xs font-bold text-slate-200 bg-[#121624] px-3 py-1 rounded-xl border border-slate-800 flex items-center gap-1.5">
              <span>{currentFilterObj.icon}</span>
              <span>Filter: {currentFilterObj.name}</span>
            </span>

            <span className={`text-xs font-black px-3 py-1 rounded-xl border ${currentDiffObj.badge}`}>
              ● {currentDiffObj.text} ({difficulty + 1}/4)
            </span>
          </div>

          {/* IMAGE CANVAS CONTAINER (EXACT 680px x 380px VIEWPORT) */}
          <div className="w-full max-w-[680px] h-[380px] rounded-3xl overflow-hidden relative border-2 border-indigo-500/40 bg-[#090c15] shadow-2xl flex items-center justify-center group">
            
            {/* SKELETON / LOADING INDICATOR */}
            {!imageLoaded && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-[#0d111e] z-10">
                <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-400 rounded-full animate-spin" />
                <span className="text-[10px] font-bold text-slate-400 tracking-wider uppercase">Loading Image...</span>
              </div>
            )}

            {/* INTERACTIVE CANVAS */}
            <canvas
              ref={canvasRef}
              width={680}
              height={380}
              className={`w-full h-full object-cover transition-opacity duration-300 ${
                imageLoaded ? 'opacity-100' : 'opacity-0'
              }`}
            />

            {/* ROUND BADGE (BOTTOM CENTER) */}
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-2 bg-slate-950/85 backdrop-blur-md px-4 py-1.5 rounded-full border border-white/20 shadow-lg">
              <span className="text-xs font-black text-white tracking-wider">
                ⚡ Turn {roundNumber} / {totalMatchRounds}
              </span>
            </div>
          </div>

          {/* ACTIONS ROW: CONTINUE GUESSING + NEXT IMAGE */}
          <div className="w-full max-w-[680px] grid grid-cols-1 sm:grid-cols-2 gap-3">
            {/* CONTINUE GUESSING BUTTON (Cycle difficulty: Max -> Med -> Easy -> Original) */}
            <button
              onClick={handleContinueGuessing}
              disabled={difficulty >= 3}
              className={`py-3.5 px-6 rounded-2xl font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 transition-all cursor-pointer ${
                difficulty >= 3
                  ? 'bg-slate-800 text-slate-500 border border-slate-700 cursor-not-allowed'
                  : 'bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-slate-950 shadow-lg shadow-orange-500/20 active:scale-95'
              }`}
            >
              <span>
                {difficulty === 0 && "🔍 Continue Guessing... (Medium Difficulty)"}
                {difficulty === 1 && "🔍 Continue Guessing... (Easy Difficulty)"}
                {difficulty === 2 && "👁️ Continue Guessing... (View Original)"}
                {difficulty >= 3 && "🖼️ Original Image Visible"}
              </span>
            </button>

            {/* NEXT IMAGE BUTTON (Always available to advance to next turn) */}
            <button
              onClick={handleNextImage}
              className="py-3.5 px-6 rounded-2xl font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white border border-indigo-400/40 shadow-xl shadow-indigo-500/20 active:scale-95 transition-all cursor-pointer"
            >
              <span>{roundNumber >= totalMatchRounds ? "🏁 Finish Match" : "Next Image ➔"}</span>
            </button>
          </div>

        </div>
      )}

    </div>
  );
}
