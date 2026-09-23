import React, { useState, useEffect, useMemo } from 'react';

// Default rich landmarks pool with 3 high-quality, verified public images per location
const DEFAULT_LOCATIONS = [
  {
    location_name: "Chichén Itzá, México",
    images: [
      "https://images.unsplash.com/photo-1518638150340-f706e86654de?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1568402102990-bc541580b59f?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1599837565318-67429bde7162?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Coliseo Romano, Italia",
    images: [
      "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1515542622106-78bda8ba0e5b?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1529260830199-42c24126f198?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Machu Picchu, Perú",
    images: [
      "https://images.unsplash.com/photo-1526392060635-9d6019884377?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1587595431973-160d0d94add1?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1509299349698-dd22323b5963?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Torre Eiffel (París), Francia",
    images: [
      "https://images.unsplash.com/photo-1511739001486-6bfe10ce785f?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Taj Mahal (Agra), India",
    images: [
      "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1585506942812-e72b29cef752?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1548013146-72479768bada?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Gran Muralla China, China",
    images: [
      "https://images.unsplash.com/photo-1508804185872-d7badad00f7d?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Estatua de la Libertad (Nueva York), EE. UU.",
    images: [
      "https://images.unsplash.com/photo-1605130284535-11dd9eedc58a?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1543783207-ec64e4d95325?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1503572327579-b5c6afe5c5c5?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Cristo Redentor (Río de Janeiro), Brasil",
    images: [
      "https://images.unsplash.com/photo-1598970434795-0c54fe7c0648?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1516306580123-e6e52b1b7b5f?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Pirámides de Guiza, Egipto",
    images: [
      "https://images.unsplash.com/photo-1503177119275-0aa32b3a9368?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1539650116574-8efeb43e2750?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1568322445389-f64ac2515020?auto=format&fit=crop&w=1200&q=80"
    ]
  },
  {
    location_name: "Ópera de Sídney, Australia",
    images: [
      "https://images.unsplash.com/photo-1624138784614-87fd1b6528f8?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=1200&q=80",
      "https://images.unsplash.com/photo-1523428096881-5bd79d04330f?auto=format&fit=crop&w=1200&q=80"
    ]
  }
];

export default function GeoLocationPlay({ profiles = [], setupData = {}, onSelectWinner }) {
  // Calculation of total match rounds by competitors & rounds per player
  const roundsPerPlayer = setupData?.rounds_per_player || setupData?.roundsPerPlayer || 1;
  const numPlayers = Array.isArray(profiles) && profiles.length > 0 ? profiles.length : 1;
  const totalMatchRounds = numPlayers * roundsPerPlayer;

  // Normalized source pool
  const initialPool = useMemo(() => {
    const rawLocations = setupData?.locations;
    if (Array.isArray(rawLocations) && rawLocations.length > 0) {
      const valid = rawLocations
        .map(loc => ({
          location_name: loc.location_name || loc.locationName || 'Unknown Location',
          images: Array.isArray(loc.images) && loc.images.length > 0 ? loc.images : []
        }))
        .filter(loc => loc.images.length > 0);

      if (valid.length > 0) return valid;
    }
    return DEFAULT_LOCATIONS;
  }, [setupData]);

  // Session State
  const [workingPool, setWorkingPool] = useState([]);
  const [currentLocation, setCurrentLocation] = useState(null);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [isRoundActive, setIsRoundActive] = useState(false); // Rule 1: Hidden before starting round
  const [isAnswerRevealed, setIsAnswerRevealed] = useState(false); // Rule 2: Hidden answer until revealed
  const [roundNumber, setRoundNumber] = useState(1);
  const [isMatchFinished, setIsMatchFinished] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);

  // Initialize pool & first place on mount
  useEffect(() => {
    let pool = [...initialPool];
    if (pool.length === 0) pool = [...DEFAULT_LOCATIONS];

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1); // Rule: No duplicate place within the game turn

    setWorkingPool(pool);
    setCurrentLocation(chosen);
    setCurrentImageIndex(0);
    setIsRoundActive(false); // Hidden by default (waiting screen)
    setIsAnswerRevealed(false);
    setRoundNumber(1);
    setIsMatchFinished(false);
    setImageLoaded(false);
  }, [initialPool]);

  // Handler for Rule 2: Start Round
  const handleStartRound = () => {
    setIsRoundActive(true);
    setIsAnswerRevealed(false);
    setCurrentImageIndex(0);
    setImageLoaded(false);
  };

  // Handler for Rule 3: Next Image (No going back)
  const handleNextImage = () => {
    if (!currentLocation || !currentLocation.images) return;
    if (currentImageIndex < currentLocation.images.length - 1) {
      setImageLoaded(false);
      setCurrentImageIndex(prev => prev + 1);
    }
  };

  // Handler for Rule 2: Reveal Answer
  const handleRevealAnswer = () => {
    setIsAnswerRevealed(true);
  };

  // Handler for Rule 2: Next Round
  const handleNextRound = () => {
    if (roundNumber >= totalMatchRounds) {
      setIsMatchFinished(true);
      setIsRoundActive(false);
      return;
    }

    let pool = [...workingPool];
    if (pool.length === 0) {
      pool = [...initialPool];
    }
    if (pool.length === 0) {
      pool = [...DEFAULT_LOCATIONS];
    }

    const randomIndex = Math.floor(Math.random() * pool.length);
    const chosen = pool[randomIndex];
    pool.splice(randomIndex, 1); // Excluded from pool for next players

    setWorkingPool(pool);
    setCurrentLocation(chosen);
    setCurrentImageIndex(0);
    setIsAnswerRevealed(false);
    setIsRoundActive(false); // Returns to Rule 1 Waiting Screen for next competitor
    setRoundNumber(prev => prev + 1);
    setImageLoaded(false);
  };

  const imagesCount = currentLocation?.images?.length || 1;
  const isLastImage = currentImageIndex >= imagesCount - 1;
  const currentImageSrc = currentLocation?.images?.[currentImageIndex] || '';

  return (
    <div className="w-full flex flex-col items-center justify-center text-center animate-fadeIn select-none">
      
      {/* HEADER STATUS / BADGES */}
      <div className="mb-4 flex flex-wrap items-center justify-center gap-2">
        <span className="text-xs font-black text-amber-400 uppercase tracking-widest bg-amber-500/10 px-3.5 py-1 rounded-full border border-amber-500/20 shadow-sm flex items-center gap-1.5">
          <span>🌍 GeoLocation Premium</span>
          <span>•</span>
          <span>Round {Math.min(roundNumber, totalMatchRounds)} of {totalMatchRounds}</span>
        </span>
        <span className="text-[11px] font-bold text-slate-400 bg-slate-800/60 px-3 py-1 rounded-full border border-slate-700/60">
          Unseen Locations Pool: {workingPool.length}
        </span>
      </div>

      {/* ========================================================================= */}
      {/* 1. MATCH FINISHED SCREEN (When all rounds are completed)                  */}
      {/* ========================================================================= */}
      {isMatchFinished ? (
        <div className="w-full max-w-[680px] h-[380px] bg-[#121624] border-2 border-amber-500/50 rounded-3xl p-8 flex flex-col items-center justify-center gap-4 shadow-2xl animate-fadeIn text-center">
          <div className="w-16 h-16 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-center text-4xl shadow-inner animate-bounce">
            🏆
          </div>
          <h3 className="text-2xl font-black text-amber-300 uppercase tracking-wider">
            ALL GEOLOCATION ROUNDS COMPLETED!
          </h3>
          <p className="text-xs font-bold text-slate-300 max-w-md leading-relaxed">
            All {totalMatchRounds} scheduled rounds have concluded ({numPlayers} competitor(s) × {roundsPerPlayer} round/player). Adjust final scores above or proceed to the next game!
          </p>
          <div className="flex items-center gap-2 text-[11px] font-bold text-slate-400 mt-2 bg-slate-900/80 px-4 py-2 rounded-xl border border-slate-800">
            <span>🏁 Ready for Winner Announcement or Next Minigame</span>
          </div>
        </div>
      ) : !isRoundActive ? (
        /* ========================================================================= */
        /* 2. RULE 1: WAITING SCREEN (Hidden before starting round, exact same size)  */
        /* ========================================================================= */
        <div className="w-full max-w-[680px] h-[380px] bg-[#121624] border-2 border-indigo-500/30 rounded-3xl p-8 flex flex-col items-center justify-center gap-5 shadow-2xl animate-fadeIn text-center relative overflow-hidden group">
          <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/10 via-transparent to-purple-500/10 pointer-events-none" />
          
          <div className="w-20 h-20 rounded-3xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-4xl shadow-inner group-hover:scale-105 transition-transform duration-300">
            🧭
          </div>

          <div className="space-y-1.5 z-10">
            <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">
              Round {roundNumber} of {totalMatchRounds}
            </span>
            <h3 className="text-xl md:text-2xl font-black text-white uppercase tracking-tight">
              Ready to Explore the Mystery Location?
            </h3>
            <p className="text-xs font-medium text-slate-400 max-w-sm mx-auto leading-relaxed">
              Decide which competitor will play this round. When ready, press start to reveal the first clue image.
            </p>
          </div>

          <button
            onClick={handleStartRound}
            className="z-10 px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-wider bg-gradient-to-r from-amber-400 to-yellow-300 hover:from-amber-300 hover:to-yellow-200 text-slate-950 shadow-xl shadow-amber-500/20 active:scale-95 transition-all cursor-pointer flex items-center gap-2 font-mono"
          >
            <span>🚀 Start Round</span>
          </button>
        </div>
      ) : (
        /* ========================================================================= */
        /* 3. ACTIVE ROUND SCREEN: Image Frame + Answer Card + Controls              */
        /* ========================================================================= */
        <div className="w-full flex flex-col items-center gap-4">
          
          {/* IMAGE CONTAINER (SPECIFIC FIXED SIZE: 680px x 380px) */}
          <div className="w-full max-w-[680px] h-[380px] rounded-3xl overflow-hidden relative border-2 border-indigo-500/40 bg-[#090c15] shadow-2xl flex items-center justify-center group">
            
            {/* SKELETON / LOADING INDICATOR */}
            {!imageLoaded && (
              <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-[#0d111e] z-10">
                <div className="w-8 h-8 border-3 border-indigo-500/30 border-t-indigo-400 rounded-full animate-spin" />
                <span className="text-[10px] font-bold text-slate-400 tracking-wider uppercase">Loading Location Clue...</span>
              </div>
            )}

            {/* CLUE IMAGE */}
            <img
              src={currentImageSrc}
              alt={`Clue ${currentImageIndex + 1}`}
              onLoad={() => setImageLoaded(true)}
              onError={(e) => {
                setImageLoaded(true);
                e.target.onerror = null;
                e.target.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="680" height="380" viewBox="0 0 24 24" fill="none" stroke="%236366f1" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>';
              }}
              className={`w-full h-full object-cover transition-opacity duration-300 ${
                imageLoaded ? 'opacity-100' : 'opacity-0'
              }`}
            />

            {/* IMAGE NUMBER / TOTAL PER ROUND BADGE (BOTTOM CENTER) */}
            <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-2 bg-slate-950/85 backdrop-blur-md px-4 py-1.5 rounded-full border border-white/20 shadow-lg">
              <span className="text-xs font-black text-white tracking-wider">
                📷 {currentImageIndex + 1} / {imagesCount}
              </span>
            </div>
          </div>

          {/* ANSWER DISPLAY BOX (HIDDEN / REVEALED) */}
          <div className="w-full max-w-[680px] bg-[#121624] border border-slate-800 p-4 rounded-2xl flex flex-col items-center justify-center gap-2 shadow-lg transition-all">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">
              Location Mystery Answer
            </span>

            {isAnswerRevealed ? (
              <div className="w-full text-center py-2.5 px-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 animate-fadeIn">
                <span className="text-lg md:text-xl font-black text-emerald-300 tracking-wide flex items-center justify-center gap-2">
                  <span>🏛️</span>
                  <span>{currentLocation?.location_name || 'Secret Location'}</span>
                </span>
              </div>
            ) : (
              <div className="w-full text-center py-2.5 px-4 rounded-xl bg-slate-900 border border-slate-800 text-slate-500 text-sm font-bold flex items-center justify-center gap-2 select-none">
                <span>🔒 Answer Hidden</span>
              </div>
            )}
          </div>

          {/* ACTION BUTTONS (NEXT IMAGE & REVEAL/NEXT ROUND) */}
          <div className="w-full max-w-[680px] grid grid-cols-1 sm:grid-cols-2 gap-3 z-10">
            
            {/* BUTTON RULE 3 & 4: NEXT IMAGE / LAST IMAGE */}
            <button
              onClick={handleNextImage}
              disabled={isLastImage}
              className={`py-3.5 px-6 rounded-2xl font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 transition-all shadow-md active:scale-95 cursor-pointer ${
                isLastImage
                  ? 'bg-slate-800 text-slate-500 border border-slate-700/50 opacity-60 cursor-not-allowed'
                  : 'bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white border border-indigo-400/40 shadow-indigo-500/20'
              }`}
            >
              {isLastImage ? (
                <>
                  <span>Last Image</span>
                  <span>🚫</span>
                </>
              ) : (
                <>
                  <span>Next Image</span>
                  <span>➔</span>
                </>
              )}
            </button>

            {/* BUTTON RULE 2: REVEAL ANSWER -> NEXT ROUND */}
            {!isAnswerRevealed ? (
              <button
                onClick={handleRevealAnswer}
                className="py-3.5 px-6 rounded-2xl font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 bg-gradient-to-r from-amber-400 to-yellow-300 hover:from-amber-300 hover:to-yellow-200 text-slate-950 border border-yellow-200 shadow-md shadow-amber-500/20 active:scale-95 transition-all cursor-pointer"
              >
                <span>👁️ Reveal Answer</span>
              </button>
            ) : (
              <button
                onClick={handleNextRound}
                className="py-3.5 px-6 rounded-2xl font-black text-xs uppercase tracking-wider flex items-center justify-center gap-2 bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white border border-emerald-300/40 shadow-md shadow-emerald-500/20 active:scale-95 transition-all cursor-pointer animate-pulse"
              >
                <span>{roundNumber >= totalMatchRounds ? "🏁 Finish Match" : "➔ Next Round"}</span>
              </button>
            )}

          </div>

        </div>
      )}

    </div>
  );
}
