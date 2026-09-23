import React from 'react';
import logoImg from '../assets/logo.png';

export default function LoadPage({ onNavigate }) {
  return (
    <div className="w-full max-w-5xl bg-[#141929] border border-slate-800/60 rounded-[2.5rem] p-8 md:p-12 shadow-[0_30px_70px_-15px_rgba(0,0,0,0.8)] text-center animate-fadeIn">
      <div className="flex flex-col items-center justify-center mb-6">
        <img 
          src={logoImg} 
          alt="Game Show Center Splash" 
          className="max-h-48 md:max-h-64 w-auto object-contain filter drop-shadow-[0_10px_20px_rgba(0,0,0,0.6)]"
        />
      </div>
      <p className="text-slate-400 text-sm max-w-2xl mx-auto mb-12 leading-relaxed">
        Adjust your matchups, choose and select mini-games, and customize the rules for your events.
      </p>
      <div className="relative group w-full max-w-xl mx-auto">
        <button
          onClick={() => onNavigate('settings')}
          className="w-full bg-gradient-to-r from-indigo-500 via-purple-600 to-indigo-500 bg-[length:200%_auto] hover:bg-right text-white font-black text-2xl py-6 px-8 rounded-2xl shadow-[0_8px_0_0_#312e81,0_20px_30px_0_rgba(99,102,241,0.35)] active:translate-y-1 active:shadow-[0_4px_0_0_#312e81,0_8px_15px_0_rgba(99,102,241,0.3)] transform transition-all duration-300 uppercase tracking-widest text-center block"
        >
          Create your own Game Show
        </button>
      </div>
    </div>
  );
}