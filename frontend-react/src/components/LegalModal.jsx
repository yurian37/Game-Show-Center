import React, { useState, useEffect } from 'react';
import SvgEmoji from './SvgEmoji';

export default function LegalModal({ isOpen, onClose, initialTab = 'privacy' }) {
  const [activeTab, setActiveTab] = useState(initialTab);
  const [copiedSubject, setCopiedSubject] = useState(false);

  useEffect(() => {
    if (initialTab) {
      setActiveTab(initialTab);
    }
  }, [initialTab, isOpen]);

  if (!isOpen) return null;

  const handleCopySubject = () => {
    navigator.clipboard?.writeText("I am interested in game-show-center");
    setCopiedSubject(true);
    setTimeout(() => setCopiedSubject(false), 3000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 animate-fadeIn">
      <div className="w-full max-w-3xl bg-[#141929] border border-slate-700/60 rounded-3xl p-6 md:p-8 shadow-[0_20px_60px_rgba(0,0,0,0.8)] text-slate-200 relative max-h-[90vh] flex flex-col">
        
        {/* CLOSE BUTTON */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-700 w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm transition-all cursor-pointer"
          title="Close window"
        >
          ✕
        </button>

        {/* HEADER */}
        <div className="mb-4 pr-8">
          <h2 className="text-xl md:text-2xl font-black text-white flex items-center gap-2">
            <SvgEmoji name="star" /> Information, Contact & Policies
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Data protection, offline licensing, contact channels, and terms of service for Game Show Center.
          </p>
        </div>

        {/* TABS */}
        <div className="flex border-b border-slate-800 mb-6 gap-2 overflow-x-auto pb-1">
          <button
            onClick={() => setActiveTab('contact')}
            className={`py-2 px-4 text-xs font-black uppercase tracking-wider border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'contact'
                ? 'border-emerald-500 text-emerald-400 bg-emerald-500/10 rounded-t-lg'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Contact Us
          </button>
          <button
            onClick={() => setActiveTab('privacy')}
            className={`py-2 px-4 text-xs font-black uppercase tracking-wider border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'privacy'
                ? 'border-indigo-500 text-indigo-400 bg-indigo-500/10 rounded-t-lg'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Privacy Policy
          </button>
          <button
            onClick={() => setActiveTab('terms')}
            className={`py-2 px-4 text-xs font-black uppercase tracking-wider border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'terms'
                ? 'border-purple-500 text-purple-400 bg-purple-500/10 rounded-t-lg'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            Terms of Service
          </button>
          <button
            onClick={() => setActiveTab('about')}
            className={`py-2 px-4 text-xs font-black uppercase tracking-wider border-b-2 transition-all cursor-pointer whitespace-nowrap ${
              activeTab === 'about'
                ? 'border-pink-500 text-pink-400 bg-pink-500/10 rounded-t-lg'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            About Us
          </button>
        </div>

        {/* TAB CONTENT (SCROLLABLE) */}
        <div className="overflow-y-auto pr-2 space-y-4 text-xs text-slate-300 leading-relaxed max-h-[60vh]">
          
          {/* TAB 0: CONTACT US & COMPLETE OFFLINE EDITION */}
          {activeTab === 'contact' && (
            <div className="space-y-4">
              <div className="bg-[#1b2238] p-5 rounded-2xl border border-emerald-500/30">
                <div className="flex items-center gap-3 mb-3">
                  <span className="p-2.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
                    <SvgEmoji name="megaphone" size={20} />
                  </span>
                  <div>
                    <h3 className="text-sm font-black text-emerald-300 uppercase tracking-wide">
                      Contact YonIA Enterprise
                    </h3>
                    <p className="text-[11px] text-slate-400">
                      Official communication channels for licensing, commercial inquiries, and technical support.
                    </p>
                  </div>
                </div>

                <div className="p-3.5 bg-[#111627] rounded-xl border border-slate-700/60 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-slate-400 block tracking-wider">Official Inquiries & Support</span>
                    <span className="text-sm font-mono font-bold text-emerald-300 select-all">yoniadevs@gmail.com</span>
                  </div>
                  <a
                    href="mailto:yoniadevs@gmail.com?subject=I%20am%20interested%20in%20game-show-center"
                    className="inline-flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-black text-xs uppercase tracking-wider transition-all shadow-md cursor-pointer"
                  >
                    <span>Send Email</span>
                    <span>✉️</span>
                  </a>
                </div>
              </div>

              {/* COMPLETE OFFLINE STANDALONE EDITION HIGHLIGHT */}
              <div className="bg-gradient-to-br from-[#1d1f3b] via-[#161a2e] to-[#121626] p-5 rounded-2xl border border-indigo-500/30 shadow-xl space-y-4">
                <div className="flex items-center gap-3">
                  <span className="p-2.5 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
                    <SvgEmoji name="desktop" size={20} />
                  </span>
                  <div>
                    <h4 className="text-sm font-black text-indigo-300 uppercase tracking-wide">
                      Complete Offline Standalone Edition
                    </h4>
                    <span className="text-[10px] text-indigo-400 font-bold uppercase tracking-wider block">
                      100% Offline • Zero Latency • Native Windows Application
                    </span>
                  </div>
                </div>

                <p className="text-slate-300 text-xs leading-relaxed">
                  In addition to this web version, we offer a <strong>Complete Offline Standalone Edition</strong> for Windows. Designed specifically for live stage productions, school auditoriums, corporate events, Twitch/YouTube broadcasts, and gaming arenas, the offline edition operates without an internet connection, includes native multi-screen presenter management, low-latency audio processing, and unlimited gameplay capabilities.
                </p>

                <div className="bg-[#0e1220] p-4 rounded-xl border border-indigo-500/20 space-y-3">
                  <div className="flex items-center gap-2 text-xs font-black text-amber-300 uppercase tracking-wide">
                    <SvgEmoji name="star" size={14} /> How to Acquire the Offline Edition
                  </div>
                  <p className="text-slate-300 text-xs leading-relaxed">
                    To purchase or license the complete offline software, please contact our team via email at <strong className="text-emerald-300 font-mono">yoniadevs@gmail.com</strong> with the required subject line:
                  </p>

                  <div className="flex items-center justify-between bg-[#171d33] p-3 rounded-lg border border-slate-700/80 gap-2">
                    <span className="font-mono text-xs font-black text-amber-300 select-all">
                      I am interested in game-show-center
                    </span>
                    <button
                      type="button"
                      onClick={handleCopySubject}
                      className="text-[10px] uppercase font-bold text-indigo-300 hover:text-white bg-indigo-500/20 hover:bg-indigo-500/30 px-3 py-1.5 rounded-lg border border-indigo-500/40 transition-all shrink-0 cursor-pointer"
                    >
                      {copiedSubject ? '✓ Copied!' : 'Copy Subject'}
                    </button>
                  </div>

                  <a
                    href="mailto:yoniadevs@gmail.com?subject=I%20am%20interested%20in%20game-show-center"
                    className="w-full inline-flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-gradient-to-r from-indigo-500 via-purple-500 to-indigo-600 hover:from-indigo-400 hover:via-purple-400 hover:to-indigo-500 text-white font-black text-xs uppercase tracking-wider transition-all shadow-md cursor-pointer"
                  >
                    <span>Request Offline Edition via Email</span>
                    <SvgEmoji name="rocket" size={14} />
                  </a>
                </div>
              </div>
            </div>
          )}

          {/* TAB 1: PRIVACY POLICY (COMPLIANT WITH GOOGLE ADSENSE) */}
          {activeTab === 'privacy' && (
            <div className="space-y-4">
              <div className="bg-[#1b2238] p-4 rounded-xl border border-indigo-500/20">
                <h3 className="text-sm font-bold text-indigo-300 mb-2">1. Privacy Policy for Game Show Center</h3>
                <p>
                  At <strong>Game Show Center</strong> (accessible from https://game-show-center.vercel.app/), developed by YonIA Enterprise, the privacy of our visitors and users is of utmost importance. This policy outlines the types of information we collect and how it is used.
                </p>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">2. Google AdSense & Third-Party Cookies Disclosure</h3>
                <p className="mb-2">
                  Our website displays advertisements served by <strong>Google AdSense</strong>. In accordance with Google AdSense program policies, please note:
                </p>
                <ul className="list-disc list-inside space-y-1 text-slate-300 pl-2">
                  <li>
                    <strong>Third-Party Vendors:</strong> Third-party vendors, including Google, use cookies to serve ads based on a user's prior visits to this website or other websites across the Internet.
                  </li>
                  <li>
                    <strong>Advertising Cookies (DART):</strong> Google's use of advertising cookies enables it and its partners to serve ads to users based on their visits to our sites and/or other sites on the Internet.
                  </li>
                  <li>
                    <strong>User Opt-Out:</strong> Users may opt out of personalized advertising by visiting Google Ad Settings (<a href="https://adssettings.google.com" target="_blank" rel="noopener noreferrer" className="text-indigo-400 underline">adssettings.google.com</a>) or by visiting <a href="https://www.aboutads.info" target="_blank" rel="noopener noreferrer" className="text-indigo-400 underline">www.aboutads.info</a>.
                  </li>
                </ul>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">3. Local Storage</h3>
                <p>
                  Game Show Center does not store personally identifiable information (PII) on remote servers without explicit consent. Match setups, score presets, and contestant lists are stored entirely within your browser's local memory (localStorage) to provide a responsive, latency-free hosting experience.
                </p>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">4. Log Files and System Metrics</h3>
                <p>
                  Like most modern web services, standard non-identifying server logs may be generated for operational diagnostics, performance optimization, and cybersecurity purposes. These logs are not linked to any personally identifiable information.
                </p>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">5. Privacy Inquiries</h3>
                <p>
                  If you have additional questions or require more information about our Privacy Policy, please contact our team directly at: <span className="text-purple-300 font-mono">yoniadevs@gmail.com</span>.
                </p>
              </div>
            </div>
          )}

          {/* TAB 2: TERMS OF SERVICE */}
          {activeTab === 'terms' && (
            <div className="space-y-4">
              <div className="bg-[#1b2238] p-4 rounded-xl border border-purple-500/20">
                <h3 className="text-sm font-bold text-purple-300 mb-2">1. Acceptance of Terms</h3>
                <p>
                  By accessing and using Game Show Center, you acknowledge and agree to be bound by these Terms of Service. If you do not agree with any part of these terms, please discontinue using the service.
                </p>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">2. Permitted Use</h3>
                <p>
                  Game Show Center is designed as an interactive entertainment and gamification suite for live shows, classrooms, corporate team-building, and digital streaming broadcasts. You agree to use the software responsibly and are solely responsible for any customized questions or content entered during matches.
                </p>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">3. Intellectual Property</h3>
                <p>
                  All interface layouts, software logic, branding, and assets associated with Game Show Center remain the exclusive intellectual property of YonIA Enterprise. Unauthorized commercial redistribution or repackaging of the platform without an appropriate license is prohibited.
                </p>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">4. Service Availability</h3>
                <p>
                  The online web service is provided "as is". While YonIA Enterprise strives for maximum reliability and 100% uptime, we cannot guarantee uninterrupted access in the event of third-party network outages or browser incompatibilities.
                </p>
              </div>
            </div>
          )}

          {/* TAB 3: ABOUT */}
          {activeTab === 'about' && (
            <div className="space-y-4">
              <div className="bg-[#1b2238] p-4 rounded-xl border border-pink-500/20">
                <h3 className="text-sm font-bold text-pink-300 mb-2">About Game Show Center</h3>
                <p className="mb-2">
                  <strong>Game Show Center</strong> was envisioned to bring the thrill and production quality of televised game shows to everyday screens: classrooms, community gatherings, Twitch and YouTube live streams, and corporate game nights.
                </p>
                <p>
                  Built with state-of-the-art web performance and zero-latency sound engines, it offers a seamless web version as well as a standalone desktop edition engineered in JavaFX and Spring Boot.
                </p>
              </div>

              <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
                <h3 className="text-sm font-bold text-slate-200 mb-2">Developed by YonIA Enterprise</h3>
                <p>
                  YonIA Enterprise specializes in modern interactive entertainment software, gamification frameworks, and high-impact digital experiences.
                </p>
                <div className="mt-3 pt-3 border-t border-slate-700/60 flex flex-col sm:flex-row items-start sm:items-center justify-between text-[11px] text-slate-400 gap-2">
                  <span>Web Version: 2.5 (AdSense Compliant)</span>
                  <span className="text-indigo-400 font-mono">yoniadevs@gmail.com</span>
                </div>
              </div>
            </div>
          )}

        </div>

        {/* FOOTER ACTION */}
        <div className="mt-6 pt-4 border-t border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-black text-xs uppercase tracking-wider rounded-xl transition-all cursor-pointer shadow-md"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
}
