import React, { useEffect, useRef, useState } from 'react';
import SvgEmoji from './SvgEmoji';

/**
 * AdsterraBanner - Integration component for Adsterra Display Banners (Skyscraper 160x600, 300x250, etc.)
 * Safely sandboxes `atOptions` within an isolated iframe to prevent global scope conflicts in React SPAs,
 * and renders an elegant branded fallback when no zone key is configured or when ads are blocked.
 */
export default function AdsterraBanner({
  zoneKey = "",
  position = "left",
  width = 160,
  height = 600,
  label = "Sponsored Space"
}) {
  const [adBlocked, setAdBlocked] = useState(false);
  const iframeRef = useRef(null);

  // Read zone key from prop or Vite environment variables
  const effectiveZoneKey =
    zoneKey ||
    (position === 'left'
      ? import.meta.env.VITE_ADSTERRA_LEFT_ZONE_KEY
      : import.meta.env.VITE_ADSTERRA_RIGHT_ZONE_KEY) ||
    "";

  const hasLiveKey = Boolean(
    effectiveZoneKey &&
    effectiveZoneKey.trim() !== "" &&
    effectiveZoneKey !== "YOUR_ADSTERRA_KEY_HERE"
  );

  useEffect(() => {
    if (!hasLiveKey || !iframeRef.current) return;

    try {
      const iframe = iframeRef.current;
      const iframeDoc = iframe.contentDocument || iframe.contentWindow?.document;
      if (!iframeDoc) return;

      const adHtml = `
        <!DOCTYPE html>
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
            <script type="text/javascript" src="//www.topcreativeformat.com/${effectiveZoneKey}/invoke.js" onerror="parent.postMessage('adsterra_blocked_${position}', '*')"></script>
          </body>
        </html>
      `;

      iframeDoc.open();
      iframeDoc.write(adHtml);
      iframeDoc.close();
    } catch (err) {
      console.warn("Adsterra banner initialization notice:", err);
      setAdBlocked(true);
    }

    const handleMessage = (event) => {
      if (event.data === `adsterra_blocked_${position}`) {
        setAdBlocked(true);
      }
    };

    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, [effectiveZoneKey, hasLiveKey, height, width, position]);

  return (
    <div className="w-full bg-[#111625]/95 border border-slate-800/80 rounded-2xl p-3 my-2 shadow-xl text-center relative overflow-hidden flex flex-col items-center">
      {/* HEADER LABEL */}
      <div className="flex items-center justify-center mb-3 w-full">
        <span className="text-[9px] font-black uppercase tracking-widest text-slate-400 bg-slate-800/70 px-2.5 py-1 rounded-md border border-slate-700/50 inline-flex items-center gap-1.5 select-none">
          <SvgEmoji name="megaphone" size={13} /> {label}
        </span>
      </div>

      {/* AD CONTAINER: REAL ADSTERRA IFRAME OR BRANDED FALLBACK */}
      {hasLiveKey && !adBlocked ? (
        <div
          className="flex justify-center items-center overflow-hidden rounded-xl bg-slate-900/60 border border-slate-800/60"
          style={{ width: `${width}px`, minHeight: `${height}px` }}
        >
          <iframe
            ref={iframeRef}
            title={`Adsterra ${position} Banner`}
            width={width}
            height={height}
            frameBorder="0"
            scrolling="no"
            className="border-0 overflow-hidden"
          />
        </div>
      ) : (
        /* ELEGANT BRANDED FALLBACK CARD (OFFLINE PRO & LOCAL AI HIGHLIGHTS) */
        <div className="w-full flex flex-col items-center text-center p-3 py-4 bg-gradient-to-b from-[#181f33] to-[#0e1322] border border-indigo-500/20 rounded-xl shadow-inner">
          {position === 'left' ? (
            <>
              <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center mb-2 text-indigo-400">
                <SvgEmoji name="desktop" size={20} />
              </div>
              <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-widest bg-indigo-500/10 px-2 py-0.5 rounded-full mb-1">
                Offline Runtime
              </span>
              <h4 className="text-xs font-black text-white mt-1 mb-1">
                Zero Internet Required
              </h4>
              <p className="text-[10px] text-slate-400 leading-tight mb-3">
                Download the standalone Windows <code className="text-indigo-300 font-mono">.exe</code> for lag-free auditorium and classroom game shows.
              </p>
              <div className="w-full bg-[#111625] border border-slate-800 rounded-lg p-2 text-left mb-3 space-y-1">
                <div className="flex items-center gap-1.5 text-[9px] text-slate-300 font-semibold">
                  <span className="text-emerald-400">✓</span> 100% Zero Network Latency
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-300 font-semibold">
                  <span className="text-emerald-400">✓</span> Battle Royale Mode
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-300 font-semibold">
                  <span className="text-emerald-400">✓</span> Custom Classroom Themes
                </div>
              </div>
              <a
                href="#download"
                onClick={(e) => {
                  e.preventDefault();
                  alert("To download the 100% Offline Edition or get enterprise licenses for your school, contact yoniadevs@gmail.com or run template-offline with master key YONIA-ENTERPRISE-PRO-2026.");
                }}
                className="w-full py-2 px-3 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white text-[10px] font-bold uppercase rounded-lg shadow-md transition-all cursor-pointer block"
              >
                Get Desktop App
              </a>
            </>
          ) : (
            <>
              <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/30 flex items-center justify-center mb-2 text-purple-400">
                <SvgEmoji name="robot" size={20} />
              </div>
              <span className="text-[10px] font-bold text-purple-400 uppercase tracking-widest bg-purple-500/10 px-2 py-0.5 rounded-full mb-1">
                Private Local AI
              </span>
              <h4 className="text-xs font-black text-white mt-1 mb-1">
                PDF Textbook RAG
              </h4>
              <p className="text-[10px] text-slate-400 leading-tight mb-3">
                Upload up to 10 school PDFs to generate tailored questions automatically with offline Qwen LLM.
              </p>
              <div className="w-full bg-[#111625] border border-slate-800 rounded-lg p-2 text-left mb-3 space-y-1">
                <div className="flex items-center gap-1.5 text-[9px] text-slate-300 font-semibold">
                  <span className="text-purple-400">★</span> Offline Quantized GGUF
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-300 font-semibold">
                  <span className="text-purple-400">★</span> Zero API Costs
                </div>
                <div className="flex items-center gap-1.5 text-[9px] text-slate-300 font-semibold">
                  <span className="text-purple-400">★</span> Strict COPPA/FERPA Privacy
                </div>
              </div>
              <a
                href="#ai-rag"
                onClick={(e) => {
                  e.preventDefault();
                  alert("Local AI with PDF RAG operates 100% offline in template-offline with no external API calls, ensuring student data never leaves your computer.");
                }}
                className="w-full py-2 px-3 bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-500 hover:to-pink-500 text-white text-[10px] font-bold uppercase rounded-lg shadow-md transition-all cursor-pointer block"
              >
                Discover Local AI
              </a>
            </>
          )}
        </div>
      )}

      {/* FOOTER ADSTERRA BADGE */}
      <div className="mt-2 text-[9px] text-slate-500 flex items-center justify-center gap-1 select-none">
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
        <span>Adsterra Verified Partner</span>
      </div>
    </div>
  );
}
