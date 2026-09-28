import React from 'react';
import logoImg from '../assets/logo.png';
import SvgEmoji from './SvgEmoji';

export default function LoadPage({ onNavigate, onOpenLegal }) {
  return (
    <div className="w-full max-w-5xl flex flex-col gap-10 animate-fadeIn text-slate-200">
      
      {/* HERO SECTION */}
      <section className="bg-[#141929] border border-slate-800/80 rounded-[2.5rem] p-8 md:p-12 shadow-[0_30px_70px_-15px_rgba(0,0,0,0.8)] text-center relative overflow-hidden">
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-96 h-40 bg-gradient-to-b from-indigo-500/10 via-purple-500/5 to-transparent blur-3xl pointer-events-none" />
        
        <div className="flex flex-col items-center justify-center mb-6 relative">
          <img 
            src={logoImg} 
            alt="Game Show Center Splash" 
            className="max-h-44 md:max-h-56 w-auto object-contain filter drop-shadow-[0_10px_25px_rgba(99,102,241,0.35)]"
          />
        </div>

        <h2 className="text-2xl md:text-4xl font-black text-transparent bg-clip-text bg-gradient-to-r from-indigo-300 via-purple-200 to-pink-300 tracking-tight mb-4">
          Live Interactive Game Show Platform
        </h2>

        <p className="text-slate-300 text-sm md:text-base max-w-3xl mx-auto mb-8 leading-relaxed">
          The comprehensive digital suite for educators, live streamers, content creators, and event producers. Build custom matchups, manage real-time scoreboards, and broadcast television-grade competitions with zero network latency.
        </p>

        {/* PRIMARY CALL TO ACTION */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 max-w-xl mx-auto mb-6">
          <button
            onClick={() => onNavigate('settings')}
            className="w-full sm:w-auto flex-1 bg-gradient-to-r from-indigo-500 via-purple-600 to-indigo-500 bg-[length:200%_auto] hover:bg-right text-white font-black text-lg md:text-xl py-5 px-8 rounded-2xl shadow-[0_8px_0_0_#312e81,0_20px_30px_0_rgba(99,102,241,0.35)] active:translate-y-1 active:shadow-[0_4px_0_0_#312e81,0_8px_15px_0_rgba(99,102,241,0.3)] transform transition-all duration-300 uppercase tracking-wider text-center cursor-pointer flex items-center justify-center gap-2"
          >
            <SvgEmoji name="lightning" /> Launch Match Settings Panel
          </button>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-3 text-xs text-slate-400">
          <span className="flex items-center gap-1.5 bg-slate-900/80 px-3 py-1.5 rounded-full border border-slate-800">
            <span className="w-2 h-2 rounded-full bg-emerald-400"></span> 100% Free Web Edition
          </span>
          <span className="flex items-center gap-1.5 bg-slate-900/80 px-3 py-1.5 rounded-full border border-slate-800">
            <SvgEmoji name="star" /> OBS & Live Streaming Ready
          </span>
          <span className="flex items-center gap-1.5 bg-slate-900/80 px-3 py-1.5 rounded-full border border-slate-800">
            <SvgEmoji name="lock" /> Privacy Focused
          </span>
        </div>
      </section>

      {/* SECTION: COMPETITION MODES */}
      <section className="bg-[#141929] border border-slate-800/80 rounded-3xl p-6 md:p-8 shadow-xl">
        <div className="mb-6">
          <span className="text-[10px] font-black uppercase tracking-widest text-indigo-400 bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
            Match Formats
          </span>
          <h3 className="text-xl md:text-2xl font-black text-white mt-2">
            Three Tailored Game Modes for Any Audience
          </h3>
          <p className="text-xs text-slate-400 mt-1">
            Seamlessly adapt match structures to fit your audience size and hosting dynamic.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div className="bg-[#1b2238] border border-slate-800 p-5 rounded-2xl flex flex-col justify-between hover:border-indigo-500/40 transition-colors">
            <div>
              <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center mb-3 text-indigo-400 text-lg">
                <SvgEmoji name="swords" />
              </div>
              <h4 className="text-base font-bold text-white mb-1">1 vs 1 Head-to-Head</h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Direct head-to-head showdown between two contenders. Bilateral scoreboards, turn alternations, and intense round-by-round point swings.
              </p>
            </div>
            <span className="mt-4 text-[10px] font-bold text-indigo-400 uppercase tracking-wider block">
              2 Contestants • Pure Duel
            </span>
          </div>

          <div className="bg-[#1b2238] border border-slate-800 p-5 rounded-2xl flex flex-col justify-between hover:border-purple-500/40 transition-colors">
            <div>
              <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/30 flex items-center justify-center mb-3 text-purple-400 text-lg">
                <SvgEmoji name="users" />
              </div>
              <h4 className="text-base font-bold text-white mb-1">Team Showdown</h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Supports 2 to 4 squads competing for victory. Organize team captains, tally joint scores, and rotate group representatives across mini-game cycles.
              </p>
            </div>
            <span className="mt-4 text-[10px] font-bold text-purple-400 uppercase tracking-wider block">
              2 to 4 Teams • Cooperative
            </span>
          </div>

          <div className="bg-[#1b2238] border border-slate-800 p-5 rounded-2xl flex flex-col justify-between hover:border-pink-500/40 transition-colors">
            <div>
              <div className="w-10 h-10 rounded-xl bg-pink-500/10 border border-pink-500/30 flex items-center justify-center mb-3 text-pink-400 text-lg">
                <SvgEmoji name="crown" />
              </div>
              <h4 className="text-base font-bold text-white mb-1">Free-For-All (FFA)</h4>
              <p className="text-xs text-slate-400 leading-relaxed">
                Up to 8 individual players battle in an open arena. Dynamic leaderboard standings with an automated podium finale and tie-break thrill.
              </p>
            </div>
            <span className="mt-4 text-[10px] font-bold text-pink-400 uppercase tracking-wider block">
              Up to 8 Players • Dynamic & Fun
            </span>
          </div>
        </div>
      </section>

      {/* SECTION: PRODUCER & HOST GUIDE */}
      <section className="bg-[#141929] border border-slate-800/80 rounded-3xl p-6 md:p-8 shadow-xl">
        <div className="mb-6">
          <span className="text-[10px] font-black uppercase tracking-widest text-indigo-400 bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
            Producer Guide
          </span>
          <h3 className="text-xl md:text-2xl font-black text-white mt-2">
            How to Host an Event in 4 Simple Steps
          </h3>
          <p className="text-xs text-slate-400 mt-1">
            A quick-start workflow for event coordinators, show hosts, and teachers.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-[#1b2238] p-4 rounded-2xl border border-slate-800">
            <span className="text-2xl font-black text-indigo-400 mb-2 block font-mono">01</span>
            <h4 className="text-xs font-bold text-white uppercase mb-1">Set Roster</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Select your mode (1vs1, Teams, or FFA) and enter contestant names or squad titles.
            </p>
          </div>

          <div className="bg-[#1b2238] p-4 rounded-2xl border border-slate-800">
            <span className="text-2xl font-black text-purple-400 mb-2 block font-mono">02</span>
            <h4 className="text-xs font-bold text-white uppercase mb-1">Pick Mini-Games</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Choose the games included in your loop and customize rounds, words, or timing goals.
            </p>
          </div>

          <div className="bg-[#1b2238] p-4 rounded-2xl border border-slate-800">
            <span className="text-2xl font-black text-pink-400 mb-2 block font-mono">03</span>
            <h4 className="text-xs font-bold text-white uppercase mb-1">Tune Score Buttons</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Customize quick score buttons (+10, +20, +40, penalties) to award points with one click.
            </p>
          </div>

          <div className="bg-[#1b2238] p-4 rounded-2xl border border-slate-800">
            <span className="text-2xl font-black text-emerald-400 mb-2 block font-mono">04</span>
            <h4 className="text-xs font-bold text-white uppercase mb-1">Broadcast Live!</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Mirror your screen to a projector or capture the window in OBS Studio for broadcast.
            </p>
          </div>
        </div>
      </section>

      {/* SECTION: FREQUENTLY ASKED QUESTIONS */}
      <section className="bg-[#141929] border border-slate-800/80 rounded-3xl p-6 md:p-8 shadow-xl">
        <div className="mb-6">
          <span className="text-[10px] font-black uppercase tracking-widest text-emerald-400 bg-emerald-500/10 px-3 py-1 rounded-full border border-emerald-500/20">
            Frequently Asked Questions
          </span>
          <h3 className="text-xl md:text-2xl font-black text-white mt-2">
            Everything You Need to Know
          </h3>
        </div>

        <div className="space-y-3">
          <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
            <h4 className="text-xs font-bold text-slate-200 mb-1">Is Game Show Center free to use online?</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Yes. The web application is 100% free for schools, community events, and live streams. It is supported by verified ad spaces through Google AdSense.
            </p>
          </div>

          <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
            <h4 className="text-xs font-bold text-slate-200 mb-1">Is any personal data stored or transmitted?</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              No private personal data is collected without explicit consent. Match setups and contestant names remain strictly inside your browser's local memory (localStorage).
            </p>
          </div>

          <div className="bg-[#1b2238] p-4 rounded-xl border border-slate-800">
            <h4 className="text-xs font-bold text-slate-200 mb-1">Can I stream this with OBS Studio or external monitors?</h4>
            <p className="text-[11px] text-slate-400 leading-relaxed">
              Absolutely. Game Show Center is built with responsive 1080p full-screen layouts that look crisp on projectors, secondary HDMI displays, and OBS window captures.
            </p>
          </div>
        </div>
      </section>

      {/* FOOTER CALL TO ACTION & LEGAL ACCESS */}
      <section className="bg-[#141929] border border-slate-800/80 rounded-3xl p-6 text-center shadow-xl">
        <div className="max-w-xl mx-auto space-y-4">
          <h3 className="text-lg font-bold text-white">Ready to run your interactive game show?</h3>
          <button
            onClick={() => onNavigate('settings')}
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-black text-sm py-3.5 px-8 rounded-xl uppercase tracking-wider transition-all shadow-lg cursor-pointer inline-flex items-center gap-2"
          >
            <SvgEmoji name="play" /> Open Control Panel
          </button>
          
          <div className="pt-4 border-t border-slate-800 flex flex-wrap items-center justify-center gap-4 text-xs text-slate-400">
            <button 
              type="button" 
              onClick={() => onOpenLegal && onOpenLegal('privacy')}
              className="hover:text-indigo-400 underline transition-colors cursor-pointer"
            >
              Privacy Policy
            </button>
            <span>•</span>
            <button 
              type="button" 
              onClick={() => onOpenLegal && onOpenLegal('terms')}
              className="hover:text-purple-400 underline transition-colors cursor-pointer"
            >
              Terms of Service
            </button>
            <span>•</span>
            <button 
              type="button" 
              onClick={() => onOpenLegal && onOpenLegal('about')}
              className="hover:text-pink-400 underline transition-colors cursor-pointer"
            >
              About Yuyi Studio
            </button>
          </div>
        </div>
      </section>

    </div>
  );
}