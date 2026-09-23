import React, { useState } from 'react';
import logoImg from './assets/logo.png';
import LoadPage from './components/LoadPage';
import Settings from './components/Settings';
import OnGameHost from './components/OnGameHost';
import ArenaStage from './components/ArenaStage';
import WinnerStage from './components/WinnerStage';
import AdSenseBanner from './components/AdSenseBanner';

import soundManager from './services/soundManager';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error("Game Show Center Render Error:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="bg-[#1b2238] border-2 border-rose-500/50 p-8 rounded-3xl text-center max-w-lg mx-auto shadow-2xl my-8">
          <span className="text-4xl mb-4 block">⚠️</span>
          <h3 className="text-xl font-black text-rose-400 mb-2 uppercase">Render Error Caught</h3>
          <p className="text-xs text-slate-300 mb-4 bg-slate-900/80 p-3 rounded-xl font-mono text-left overflow-auto max-h-40 border border-slate-800">
            {this.state.error?.toString()}
          </p>
          <button
            onClick={() => {
              this.setState({ hasError: false });
              if (this.props.onReset) this.props.onReset();
            }}
            className="bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold px-6 py-3 rounded-xl uppercase tracking-wider transition-all"
          >
            Return to Settings
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default function App() {
  // 1. Un solo estado maestro para la pantalla actual
  const [screen, setScreen] = useState('home');
  
  // 2. Estado para guardar la configuración de la dinámica que viene de Settings
  const [matchData, setMatchData] = useState(null);

  // 3. Función unificada de navegación que atrapa la pantalla y los datos
  const handleNavigate = (view, data = null) => {
    soundManager.stopAll();
    setScreen(view);
    if (data) {
      setMatchData(prev => ({ ...(prev || {}), ...data }));
    }
  };

  return (
    <div className="min-h-screen bg-[#0d111c] text-slate-100 flex flex-col justify-between font-sans selection:bg-indigo-500 selection:text-white">
      
      {/* HEADER MARCO */}
      <header className="bg-[#141929] p-4 px-8 flex justify-between items-center border-b border-slate-800 shadow-xl z-10">
        <div className="flex items-center gap-3 cursor-pointer" onClick={() => handleNavigate('home')}>
          <img 
            src={logoImg} 
            alt="Game Show Center Logo" 
            className="h-10 w-auto object-contain drop-shadow-[0_2px_8px_rgba(99,102,241,0.3)]"
          />
          <h1 className="text-3xl font-black tracking-wider text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 via-purple-400 to-pink-400 drop-shadow-[0_2px_10px_rgba(99,102,241,0.3)]">
            GAME SHOW CENTER
          </h1>
        </div>
        <div className="bg-[#1e253b] border border-indigo-500/20 rounded-xl px-4 py-1.5 shadow-inner">
          <span className="text-xs text-slate-400 tracking-wide">by </span>
          <span className="text-sm font-bold text-indigo-300">YUYI STUDIO</span>
        </div>
      </header>

      {/* WRAPPER GLOBAL MARCO */}
      <div className="flex flex-grow w-full max-w-[1920px] mx-auto items-stretch relative">
        
        {/* PUBLICIDAD IZQUIERDA MARCO (SPONSOR SPACE 1) */}
        <aside className="hidden xl:flex w-72 bg-[#111524] border-r border-slate-800/80 flex-col p-5 justify-between items-center relative shrink-0">
          <div className="absolute inset-0 bg-gradient-to-b from-indigo-500/5 to-transparent pointer-events-none" />
          <div className="w-full text-center">
            <p className="text-[11px] font-black text-indigo-400/40 tracking-[0.4em] uppercase mb-4 select-none">
              — SPONSOR SPACE —
            </p>
            
            <AdSenseBanner slotId="1000000001" label="Official Sponsor 1" />
          </div>
        </aside>

        {/* CENTRO DINÁMICO REEMPLAZABLE */}
        <main className="flex-grow flex items-center justify-center p-6 md:p-12 lg:px-16">
          <ErrorBoundary onReset={() => setScreen('settings')}>
            {/* Usamos handleNavigate en lugar de setScreen directo */}
            {screen === 'home' && <LoadPage onNavigate={handleNavigate} />}
            
            {screen === 'settings' && <Settings onNavigate={handleNavigate} />}
            
            {/* NUEVA VISTA: EL ESTUDIO DE PRODUCCIÓN */}
            {screen === 'host' && (
              <OnGameHost 
                selectedGames={matchData?.selectedGames || []}
                gameMode={matchData?.gameMode || '1vs1'}
                initialPlayers={matchData?.initialPlayers || {}}
                scorePresets={matchData?.scorePresets}
                onNavigate={handleNavigate}
              />
            )}

            {/* VISTA ARENA DE JUEGO EN VIVO */}
            {screen === 'arena' && (
              <ArenaStage
                matchData={matchData || {}}
                onNavigate={handleNavigate}
              />
            )}

            {/* VISTA GANADOR / PODIO FINAL */}
            {screen === 'winner' && (
              <WinnerStage
                matchData={matchData || {}}
                onNavigate={handleNavigate}
              />
            )}
          </ErrorBoundary>
        </main>

        {/* PUBLICIDAD DERECHA MARCO (SPONSOR SPACE 2) */}
        <aside className="hidden xl:flex w-72 bg-[#111524] border-l border-slate-800/80 flex-col p-5 justify-between items-center relative shrink-0">
          <div className="absolute inset-0 bg-gradient-to-b from-purple-500/5 to-transparent pointer-events-none" />
          <div className="w-full text-center">
            <p className="text-[11px] font-black text-purple-400/40 tracking-[0.4em] uppercase mb-4 select-none">
              — SPONSOR SPACE —
            </p>

            <AdSenseBanner slotId="2000000002" label="Official Sponsor 2" />
          </div>
        </aside>

      </div>

      {/* FOOTER MARCO */}
      <footer className="bg-[#090c14] p-4 text-center text-xs text-slate-600 border-t border-slate-900 shadow-2xl tracking-wide z-10">
        <p>
          &copy; {new Date().getFullYear()} Game Show Center. Developed by {' '}
          <strong className="text-slate-400 font-semibold hover:text-indigo-400 transition-colors">
            Yuyi Studio
          </strong>
          . All rights reserved.
        </p>
      </footer>
    </div>
  );
}