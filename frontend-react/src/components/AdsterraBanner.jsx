import React, { useEffect, useRef, useState } from 'react';
import SvgEmoji from './SvgEmoji';

/**
 * AdsterraBanner - Integration component for Adsterra Display Banners (Skyscraper 160x600, 300x250, etc.)
 * Safely sandboxes `atOptions` within an isolated iframe to prevent global scope conflicts in React SPAs,
 * and renders an elegant branded fallback when no zone key is configured or when ads are blocked.
 */
// Verified production key for Adsterra 160x600 skyscraper banner
const DEFAULT_ADSTERRA_ZONE_KEY = "4e1cbb94898a46491d4e929a66cfbc53";

export default function AdsterraBanner({
  zoneKey = "",
  position = "left",
  width = 160,
  height = 600,
  label = "Sponsored Space"
}) {
  const [adBlocked, setAdBlocked] = useState(false);
  const [hasTrackingConsent, setHasTrackingConsent] = useState(() => {
    try {
      const consent = localStorage.getItem('gsc_cookie_consent');
      // In incognito or initial visits, consent is null: display ads by default unless explicitly rejected
      return consent !== 'rejected';
    } catch {
      return true;
    }
  });

  // Read zone key from prop, Vite environment variables, or verified default key
  const effectiveZoneKey =
    zoneKey ||
    (position === 'left'
      ? import.meta.env.VITE_ADSTERRA_LEFT_ZONE_KEY
      : import.meta.env.VITE_ADSTERRA_RIGHT_ZONE_KEY) ||
    DEFAULT_ADSTERRA_ZONE_KEY;

  const hasLiveKey = Boolean(
    effectiveZoneKey &&
    effectiveZoneKey.trim() !== "" &&
    effectiveZoneKey !== "YOUR_ADSTERRA_KEY_HERE" &&
    effectiveZoneKey !== "YOUR_LEFT_ZONE_KEY_HERE" &&
    effectiveZoneKey !== "YOUR_RIGHT_ZONE_KEY_HERE"
  );

  // Listen for cookie consent updates in real time
  useEffect(() => {
    const handleConsentChange = (e) => {
      try {
        const choice = e?.detail?.choice || localStorage.getItem('gsc_cookie_consent');
        setHasTrackingConsent(choice !== 'rejected');
      } catch {
        setHasTrackingConsent(true);
      }
    };
    window.addEventListener('gsc_cookie_consent_updated', handleConsentChange);
    return () => window.removeEventListener('gsc_cookie_consent_updated', handleConsentChange);
  }, []);

  // Proactive AdBlocker Detection (Canary fetch + iframe error listener)
  useEffect(() => {
    let isMounted = true;
    const canaryUrl = `https://www.topcreativeformat.com/${effectiveZoneKey}/invoke.js`;

    // Attempt lightweight canary fetch. If an adblocker extension is active, the browser rejects with ERR_BLOCKED_BY_CLIENT
    fetch(canaryUrl, { method: 'HEAD', mode: 'no-cors' })
      .catch(() => {
        if (isMounted) {
          setAdBlocked(true);
        }
      });

    const handleMessage = (event) => {
      if (event.data?.type === 'ADSTERRA_BLOCKED') {
        if (isMounted) {
          setAdBlocked(true);
        }
      }
    };

    window.addEventListener('message', handleMessage);
    return () => {
      isMounted = false;
      window.removeEventListener('message', handleMessage);
    };
  }, [effectiveZoneKey]);

  // HTML content payload for isolated iframe execution with onerror canary
  const adHtml = `<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
      body, html {
        margin: 0;
        padding: 0;
        overflow: hidden;
        display: flex;
        justify-content: center;
        align-items: center;
        background: transparent;
        width: 100%;
        height: 100%;
      }
    </style>
  </head>
  <body>
    <script type="text/javascript">
      atOptions = {
        'key' : '${effectiveZoneKey}',
        'format' : 'iframe',
        'height' : ${height},
        'width' : ${width},
        'params' : {}
      };
    </script>
    <script type="text/javascript" src="https://www.topcreativeformat.com/${effectiveZoneKey}/invoke.js" onerror="window.parent.postMessage({ type: 'ADSTERRA_BLOCKED', pos: '${position}' }, '*')"></script>
  </body>
</html>`;

  return (
    <div className="w-full bg-[#111625]/95 border border-slate-800/80 rounded-2xl p-3 my-2 shadow-xl text-center relative overflow-hidden flex flex-col items-center">
      {/* HEADER LABEL */}
      <div className="flex items-center justify-center mb-3 w-full">
        <span className="text-[9px] font-black uppercase tracking-widest text-slate-400 bg-slate-800/70 px-2.5 py-1 rounded-md border border-slate-700/50 inline-flex items-center gap-1.5 select-none">
          <SvgEmoji name={adBlocked ? "shield" : "megaphone"} size={13} /> {adBlocked ? "Template Showcase • YonIA" : label}
        </span>
      </div>

      {/* AD CONTAINER: REAL ADSTERRA IFRAME OR TEMPLATE ADVERTISING FALLBACK */}
      {hasLiveKey && hasTrackingConsent && !adBlocked ? (
        <div
          className="flex justify-center items-center overflow-hidden rounded-xl bg-slate-900/60 border border-slate-800/60"
          style={{ width: `${width}px`, minHeight: `${height}px` }}
        >
          <iframe
            title={`Adsterra ${position} Banner`}
            width={width}
            height={height}
            srcDoc={adHtml}
            frameBorder="0"
            scrolling="no"
            className="border-0 overflow-hidden"
          />
        </div>
      ) : (
        /* OFFICIAL SHOWCASE: GAME SHOW CENTER TEMPLATE & YONIA CONTACT */
        <div className="w-full flex flex-col items-center text-center p-3 py-4 bg-gradient-to-b from-[#181f33] to-[#0e1322] border border-indigo-500/30 rounded-xl shadow-inner">
          {position === 'left' ? (
            <>
              <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center mb-2 text-indigo-400">
                <SvgEmoji name="desktop" size={20} />
              </div>
              <span className="text-[10px] font-black text-amber-400 uppercase tracking-widest bg-amber-500/10 border border-amber-500/30 px-2 py-0.5 rounded-full mb-1">
                Ad Blocker Detected
              </span>
              <h4 className="text-xs font-black text-white mt-1 mb-1">
                Game Show Center Template
              </h4>
              <p className="text-[10px] text-slate-300 leading-tight mb-3">
                Complete interactive game show engine for schools, auditoriums, and live events.
              </p>
              <div className="w-full bg-[#111625] border border-slate-800 rounded-lg p-2.5 text-left mb-3 space-y-1.5">
                <div className="flex items-center gap-1.5 text-[9px] text-slate-200 font-semibold">
                  <span className="text-emerald-400">✓</span> Online & Standalone Offline
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-200 font-semibold">
                  <span className="text-emerald-400">✓</span> 10+ Integrated Minigames
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-200 font-semibold">
                  <span className="text-emerald-400">✓</span> Multi-Team Classroom Dynamic
                </div>
              </div>
              <div className="w-full bg-[#0d111c] border border-indigo-500/20 rounded-lg p-2 mb-3">
                <span className="text-[9px] text-slate-400 block mb-0.5 uppercase tracking-wider font-bold">Contact YonIA Enterprise:</span>
                <span className="text-[11px] font-mono font-bold text-emerald-400 select-all block">yoniadevs@gmail.com</span>
              </div>
              <a
                href="mailto:yoniadevs@gmail.com?subject=I%20am%20interested%20in%20game-show-center%20template"
                className="w-full py-2.5 px-3 bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 hover:from-indigo-500 hover:to-pink-500 text-white text-[10px] font-black uppercase tracking-wider rounded-xl shadow-lg transition-all cursor-pointer block active:scale-95 text-center"
              >
                ✉ Contact YonIA
              </a>
            </>
          ) : (
            <>
              <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/30 flex items-center justify-center mb-2 text-purple-400">
                <SvgEmoji name="robot" size={20} />
              </div>
              <span className="text-[10px] font-black text-amber-400 uppercase tracking-widest bg-amber-500/10 border border-amber-500/30 px-2 py-0.5 rounded-full mb-1">
                Ad Blocker Detected
              </span>
              <h4 className="text-xs font-black text-white mt-1 mb-1">
                Custom Dynamics & Software
              </h4>
              <p className="text-[10px] text-slate-300 leading-tight mb-3">
                Need customized minigames, school site licenses, or local AI question generators?
              </p>
              <div className="w-full bg-[#111625] border border-slate-800 rounded-lg p-2.5 text-left mb-3 space-y-1.5">
                <div className="flex items-center gap-1.5 text-[9px] text-slate-200 font-semibold">
                  <span className="text-purple-400">★</span> Custom Themed Minigames
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-200 font-semibold">
                  <span className="text-purple-400">★</span> Offline PDF Textbook AI RAG
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-200 font-semibold">
                  <span className="text-purple-400">★</span> Commercial Annual Licensing
                </div>
              </div>
              <div className="w-full bg-[#0d111c] border border-purple-500/20 rounded-lg p-2 mb-3">
                <span className="text-[9px] text-slate-400 block mb-0.5 uppercase tracking-wider font-bold">Contact YonIA Enterprise:</span>
                <span className="text-[11px] font-mono font-bold text-emerald-400 select-all block">yoniadevs@gmail.com</span>
              </div>
              <a
                href="mailto:yoniadevs@gmail.com?subject=I%20am%20interested%20in%20game-show-center%20template"
                className="w-full py-2.5 px-3 bg-gradient-to-r from-purple-600 via-pink-600 to-rose-600 hover:from-purple-500 hover:to-rose-500 text-white text-[10px] font-black uppercase tracking-wider rounded-xl shadow-lg transition-all cursor-pointer block active:scale-95 text-center"
              >
                ✉ Contact YonIA
              </a>
            </>
          )}
        </div>
      )}

      {/* FOOTER BADGE */}
      <div className="mt-2 text-[9px] text-slate-500 flex items-center justify-center gap-1 select-none">
        <span className={`w-1.5 h-1.5 rounded-full ${adBlocked ? 'bg-amber-400' : 'bg-emerald-400'}`}></span>
        <span>{adBlocked ? "YonIA Enterprise Verified Template" : "Adsterra Verified Partner"}</span>
      </div>
    </div>
  );
}
