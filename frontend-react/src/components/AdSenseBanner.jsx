import React, { useEffect, useRef, useState } from 'react';

// Google AdSense Publisher ID provided by user
const PUBLISHER_ID = "ca-pub-9455310542591934";

export default function AdSenseBanner({
  slotId = "1234567890",
  format = "auto",
  responsive = "true",
  style = { display: 'block', minHeight: '120px' },
  label = "Sponsored Advertisement"
}) {
  const adRef = useRef(null);
  const [adLoaded, setAdLoaded] = useState(false);

  useEffect(() => {
    try {
      if (window.adsbygoogle && adRef.current) {
        (window.adsbygoogle = window.adsbygoogle || []).push({});
        setAdLoaded(true);
      }
    } catch (err) {
      console.warn("AdSense push notice:", err);
    }
  }, [slotId]);

  return (
    <div className="w-full bg-[#111625]/90 border border-slate-800/80 rounded-2xl p-4 my-4 shadow-lg text-center relative overflow-hidden">
      {/* LABEL */}
      <div className="flex items-center justify-between mb-2">
        <span className="text-[10px] font-black uppercase tracking-widest text-slate-400 bg-slate-800/60 px-2.5 py-0.5 rounded-md border border-slate-700/50">
          📢 {label}
        </span>
        <span className="text-[9px] font-bold text-slate-400">
          Google AdSense Verified
        </span>
      </div>

      {/* ADSENSE INS TAG */}
      <div className="flex justify-center items-center min-h-[100px] w-full">
        <ins
          ref={adRef}
          className="adsbygoogle"
          style={style}
          data-ad-client={PUBLISHER_ID}
          data-ad-slot={slotId}
          data-ad-format={format}
          data-full-width-responsive={responsive}
        />
      </div>

      {/* DEV / PREVIEW FALLBACK NOTICE */}
      {!adLoaded && (
        <div className="mt-2 text-[10px] text-slate-400 border-t border-slate-800/60 pt-2 flex items-center justify-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
          <span>Ad space active for Google AdSense ({PUBLISHER_ID})</span>
        </div>
      )}
    </div>
  );
}
