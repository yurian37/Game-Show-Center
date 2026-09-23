import React, { useState } from 'react';

// Cryptographic SHA-256 hash of the VIP promo code ("yinyang")
const ENCRYPTED_VIP_HASH = "53086b510bd55bb3f8373b5cf2e55ef92b512c1c6fbbfbe6e95c1c8a4df55811";
const PAYPAL_RECEIVER_EMAIL = "pvalencianocr@hotmail.com";

async function computeSha256(text) {
  try {
    const encoder = new TextEncoder();
    const data = encoder.encode(text.trim().toLowerCase());
    const hashBuffer = await crypto.subtle.digest('SHA-256', data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  } catch {
    return text.trim().toLowerCase();
  }
}

export default function PaymentModal({ isOpen, onClose, onDownloadSuccess }) {
  const [activeMethod, setActiveMethod] = useState('card'); // 'card', 'promo'
  const [promoCode, setPromoCode] = useState('');
  const [promoStatus, setPromoStatus] = useState(null); // { type: 'success'|'error', text: '' }
  const [paymentStatus, setPaymentStatus] = useState(null);

  if (!isOpen) return null;

  const handleValidatePromoCode = async (e) => {
    e.preventDefault();
    if (!promoCode.trim()) return;
    setPromoStatus({ type: 'error', text: '🚧 En Mantenimiento: Las descargas y validaciones se encuentran temporalmente en mantenimiento.' });
  };

  const handleSimulatedPayment = () => {
    setPaymentStatus({ type: 'error', text: '🚧 En Mantenimiento: Las descargas se encuentran temporalmente en mantenimiento.' });
  };

  const handleOpenPayPalLink = () => {
    setPaymentStatus({ type: 'error', text: '🚧 En Mantenimiento: Los pagos y descargas se encuentran temporalmente en mantenimiento.' });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/85 backdrop-blur-md p-4 animate-fadeIn">
      <div className="w-full max-w-lg bg-[#141929] border border-purple-500/30 rounded-3xl p-6 md:p-8 shadow-[0_0_50px_rgba(168,85,247,0.25)] text-slate-200 relative overflow-hidden">
        
        {/* CLOSE BUTTON */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-white bg-slate-800/60 w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm transition-all cursor-pointer"
        >
          ✕
        </button>

        {/* HEADER */}
        <div className="text-center mb-6">
          <span className="text-[10px] font-black uppercase tracking-widest text-purple-400 bg-purple-500/10 px-3 py-1 rounded-full border border-purple-500/20">
            Premium Plan Purchase • $5.00 USD
          </span>
          <h3 className="text-xl md:text-2xl font-black text-white mt-2">
            Download Standalone App
          </h3>
          <p className="text-xs text-slate-400 mt-1">
            Includes desktop executable + 5 core mini-games (Zero Margin, Hangman, TicTacToe, Roulette, Trivia Quiz) with local resources.
          </p>
        </div>

        {/* METHOD TABS */}
        <div className="grid grid-cols-2 gap-2 mb-6 bg-[#0b0e17] p-1.5 rounded-2xl border border-slate-900">
          <button
            onClick={() => setActiveMethod('card')}
            className={`py-2.5 px-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer ${
              activeMethod === 'card'
                ? 'bg-gradient-to-r from-purple-600 to-pink-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            💳 Card / PayPal
          </button>
          <button
            onClick={() => setActiveMethod('promo')}
            className={`py-2.5 px-3 text-xs font-black uppercase tracking-wider rounded-xl transition-all cursor-pointer ${
              activeMethod === 'promo'
                ? 'bg-gradient-to-r from-emerald-600 to-teal-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            🔑 VIP Promo Code
          </button>
        </div>

        {/* TAB 1: PAYPAL SMART BUTTONS & CARD */}
        {activeMethod === 'card' && (
          <div className="space-y-4 animate-fadeIn">
            <div className="bg-[#0b0e17] border border-slate-800 rounded-2xl p-4 text-center space-y-3">
              <div className="flex items-center justify-between text-xs font-bold text-slate-300">
                <span>Recipient:</span>
                <span className="text-purple-400 font-mono">{PAYPAL_RECEIVER_EMAIL}</span>
              </div>
              <div className="flex items-center justify-between text-xs font-bold text-slate-300">
                <span>Total Amount:</span>
                <span className="text-emerald-400 font-black text-sm">$5.00 USD</span>
              </div>

              {/* PAYPAL SMART BUTTONS UI */}
              <div className="space-y-2 pt-2">
                <button
                  onClick={handleOpenPayPalLink}
                  className="w-full py-3.5 px-4 rounded-xl font-black text-xs uppercase tracking-wider bg-[#ffc439] hover:bg-[#f2ba32] text-[#003087] shadow-lg active:scale-[0.99] transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  <span className="font-extrabold italic text-sm">PayPal</span>
                  <span>• Pay $5.00 USD with PayPal or Card</span>
                </button>

                <button
                  onClick={handleOpenPayPalLink}
                  className="w-full py-3 px-4 rounded-xl font-bold text-xs bg-[#2c2e2f] hover:bg-[#3b3d3e] text-white border border-slate-700 shadow-md active:scale-[0.99] transition-all cursor-pointer flex items-center justify-center gap-2"
                >
                  <span>💳 Pay with Debit or Credit Card</span>
                </button>
              </div>

              <div className="pt-2 text-[10px] text-slate-400">
                🔒 Secure global checkout powered by PayPal.
              </div>
            </div>

            {paymentStatus && (
              <div className={`p-3 rounded-xl text-xs font-bold ${
                paymentStatus.type === 'success' ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400' : 'bg-indigo-500/10 border border-indigo-500/30 text-indigo-300'
              }`}>
                {paymentStatus.text}
              </div>
            )}

            {/* TEST SIMULATION OPTION FOR DEVELOPMENT */}
            <div className="border-t border-slate-800/80 pt-3">
              <button
                onClick={handleSimulatedPayment}
                className="w-full py-2.5 px-4 rounded-xl font-bold text-[11px] bg-slate-800/80 hover:bg-slate-800 text-slate-300 border border-slate-700/60 transition-all cursor-pointer"
              >
                ⚡ Simulate Successful Checkout ($5.00 USD)
              </button>
            </div>
          </div>
        )}

        {/* TAB 2: PROMO CODE (YinYang) */}
        {activeMethod === 'promo' && (
          <form onSubmit={handleValidatePromoCode} className="space-y-4 animate-fadeIn">
            <div>
              <label className="block text-[10px] font-black uppercase text-emerald-400 tracking-wider mb-1">
                Enter VIP / Promo Access Code
              </label>
              <input
                type="text"
                placeholder="Enter promo code..."
                value={promoCode}
                onChange={(e) => setPromoCode(e.target.value)}
                className="w-full bg-[#0f1322] border border-emerald-500/40 rounded-xl px-4 py-3 text-sm font-bold text-slate-100 focus:outline-none focus:border-emerald-400 placeholder:text-slate-600"
              />
              <span className="text-[10px] text-slate-500 block mt-1">
                🔒 Cryptographically verified via secure SHA-256 hash ("YinYang").
              </span>
            </div>

            {promoStatus && (
              <div className={`p-3 rounded-xl text-xs font-bold ${
                promoStatus.type === 'success' ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400' : 'bg-rose-500/10 border border-rose-500/30 text-rose-400'
              }`}>
                {promoStatus.text}
              </div>
            )}

            <button
              type="submit"
              className="w-full py-3.5 px-6 rounded-2xl font-black text-xs uppercase tracking-wider bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white shadow-xl active:scale-95 transition-all cursor-pointer flex items-center justify-center gap-2"
            >
              🔑 Validate Code & Download Free
            </button>
          </form>
        )}

      </div>
    </div>
  );
}
