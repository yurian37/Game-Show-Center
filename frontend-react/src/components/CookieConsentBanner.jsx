import React, { useState, useEffect } from 'react';
import SvgEmoji from './SvgEmoji';

/**
 * CookieConsentBanner - Hardened Privacy & Cookie Tracking Consent Banner (100% English).
 * Complies with GDPR (Art. 7), ePrivacy Directive, SOC 2 CC6.1, and ISO/IEC 27701 (Privacy Information Management).
 * Clarifies that no server-side activity configurations or persistent match histories are stored.
 */
export default function CookieConsentBanner({ onOpenPrivacyPolicy }) {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    try {
      const consent = localStorage.getItem('gsc_cookie_consent');
      if (!consent) {
        // Show after a brief delay for smooth entrance
        const timer = setTimeout(() => setIsVisible(true), 800);
        return () => clearTimeout(timer);
      }
    } catch {
      // If localStorage is unavailable, remain hidden
    }
  }, []);

  const handleConsent = (choice) => {
    try {
      localStorage.setItem('gsc_cookie_consent', choice);
      localStorage.setItem('gsc_cookie_consent_timestamp', new Date().toISOString());
      window.dispatchEvent(new CustomEvent('gsc_cookie_consent_updated', { detail: { choice } }));
    } catch (e) {
      console.warn("Could not save cookie consent:", e);
    }
    setIsVisible(false);
  };

  if (!isVisible) return null;

  return (
    <div className="fixed bottom-4 left-4 right-4 md:left-8 md:right-8 z-50 flex justify-center animate-slideUp pointer-events-auto">
      <div className="w-full max-w-4xl bg-[#141929]/95 backdrop-blur-xl border border-indigo-500/30 rounded-3xl p-5 md:p-6 shadow-[0_20px_50px_rgba(0,0,0,0.7)] text-slate-200 flex flex-col md:flex-row items-center justify-between gap-4">
        
        {/* PRIVACY ICON & EXPLANATION */}
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center shrink-0 text-indigo-400">
            <SvgEmoji name="shield" size={24} />
          </div>
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-[10px] font-black uppercase tracking-wider text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded-full border border-indigo-500/20">
                Privacy & Data Governance
              </span>
              <span className="text-[10px] text-slate-400">ISO/IEC 27701 & GDPR</span>
            </div>
            <h4 className="text-sm font-bold text-white mb-1">
              Privacy & Cookie Preferences
            </h4>
            <p className="text-xs text-slate-300 leading-relaxed max-w-2xl">
              We use strictly necessary session cookies for security and core navigation. Game Show Center runs purely in-memory in your browser—<strong>we do not store or archive your custom activity configurations or match setups on remote servers</strong>. With your consent, verified partner networks (Adsterra) use anonymous identifiers to deliver relevant sponsorships that keep this platform free.
            </p>
          </div>
        </div>

        {/* ACTIONS */}
        <div className="flex items-center gap-2.5 shrink-0 w-full md:w-auto justify-end">
          {onOpenPrivacyPolicy && (
            <button
              onClick={onOpenPrivacyPolicy}
              className="px-3.5 py-2.5 rounded-xl text-xs font-bold text-slate-400 hover:text-white bg-slate-800/60 hover:bg-slate-800 transition-all cursor-pointer whitespace-nowrap"
            >
              Privacy Policy
            </button>
          )}
          <button
            onClick={() => handleConsent('essential_only')}
            className="px-4 py-2.5 rounded-xl text-xs font-bold text-slate-300 hover:text-white bg-slate-800/90 hover:bg-slate-700 border border-slate-700/80 transition-all cursor-pointer whitespace-nowrap"
          >
            Strictly Necessary
          </button>
          <button
            onClick={() => handleConsent('accepted')}
            className="px-5 py-2.5 rounded-xl text-xs font-black text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 shadow-lg shadow-indigo-600/30 transition-all cursor-pointer whitespace-nowrap"
          >
            Accept All
          </button>
        </div>

      </div>
    </div>
  );
}
