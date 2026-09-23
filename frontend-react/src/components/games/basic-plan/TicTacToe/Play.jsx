import React, { useState, useEffect } from 'react';
import blueTeamImg from '../../../../assets/tictactoe/blueteam.png';
import redTeamImg from '../../../../assets/tictactoe/redteam.png';

const WINNING_COMBOS = [
  [0, 1, 2], [3, 4, 5], [6, 7, 8], // Rows
  [0, 3, 6], [1, 4, 7], [2, 5, 8], // Columns
  [0, 4, 8], [2, 4, 6]             // Diagonals
];

export default function TicTacToePlay({ profiles = [], setupData = {}, onSelectWinner }) {
  const bluePlayer = Array.isArray(profiles) && profiles[0] ? profiles[0] : { id: 'p1', name: 'Blue Team (P1)' };
  const redPlayer = Array.isArray(profiles) && profiles[1] ? profiles[1] : { id: 'p2', name: 'Red Team (P2)' };

  const [board, setBoard] = useState(Array(9).fill(null));
  const [currentTurn, setCurrentTurn] = useState('blue'); // 'blue' or 'red'
  const [startingTeam, setStartingTeam] = useState('blue');
  const [winner, setWinner] = useState(null); // null, 'blue', 'red', or 'draw'
  const [winningLine, setWinningLine] = useState(null);
  const [roundNumber, setRoundNumber] = useState(1);

  // Helper to resolve starting turn
  const determineStartingTeam = React.useCallback(() => {
    const preference = setupData?.starting_team || setupData?.startingTeam || 'random';
    if (preference === 'blue') return 'blue';
    if (preference === 'red') return 'red';
    return Math.random() < 0.5 ? 'blue' : 'red';
  }, [setupData]);

  // Start new round
  const startNewRound = React.useCallback(() => {
    const nextStart = determineStartingTeam();
    setStartingTeam(nextStart);
    setCurrentTurn(nextStart);
    setBoard(Array(9).fill(null));
    setWinner(null);
    setWinningLine(null);
  }, [determineStartingTeam]);

  const setupKey = JSON.stringify(setupData);

  useEffect(() => {
    startNewRound();
  }, [setupKey, startNewRound]);

  const handleCellClick = (index) => {
    if (board[index] !== null || winner !== null) return;

    const newBoard = [...board];
    newBoard[index] = currentTurn;
    setBoard(newBoard);

    // Check Win Condition
    let foundWinner = null;
    let foundLine = null;

    for (const combo of WINNING_COMBOS) {
      const [a, b, c] = combo;
      if (newBoard[a] && newBoard[a] === newBoard[b] && newBoard[a] === newBoard[c]) {
        foundWinner = newBoard[a];
        foundLine = combo;
        break;
      }
    }

    if (foundWinner) {
      setWinner(foundWinner);
      setWinningLine(foundLine);

      const winnerComp = foundWinner === 'blue' ? bluePlayer : redPlayer;
      if (onSelectWinner) {
        onSelectWinner(winnerComp);
      }
      return;
    }

    // Check Draw Condition
    if (newBoard.every(cell => cell !== null)) {
      setWinner('draw');
      return;
    }

    // Next Player Turn
    setCurrentTurn(prev => (prev === 'blue' ? 'red' : 'blue'));
  };

  const isCurrentTurnBlue = currentTurn === 'blue';
  const activeTurnPlayer = isCurrentTurnBlue ? bluePlayer : redPlayer;

  return (
    <div className="w-full flex flex-col items-center justify-center p-6 text-center animate-fadeIn max-w-2xl mx-auto">
      
      {/* ROUND & CURRENT TURN STATUS BANNER */}
      <div className="mb-6 flex flex-col items-center gap-2">
        <span className="text-xs font-black text-indigo-400 uppercase tracking-widest bg-indigo-500/10 px-3 py-1 rounded-full border border-indigo-500/20">
          Round {roundNumber} • Starting Team: {startingTeam.toUpperCase()}
        </span>

        {winner === null ? (
          <div className="flex items-center gap-3 bg-[#121624] px-5 py-2.5 rounded-2xl border border-slate-800 shadow-md mt-1">
            <div className={`w-4 h-4 rounded-full animate-pulse ${isCurrentTurnBlue ? 'bg-sky-400 shadow-[0_0_10px_#38bdf8]' : 'bg-rose-400 shadow-[0_0_10px_#f43f5e]'}`} />
            <span className="text-xs font-black uppercase tracking-wider text-slate-200">
              Current Turn: <strong className={isCurrentTurnBlue ? 'text-sky-300' : 'text-rose-300'}>{activeTurnPlayer.name}</strong> ({isCurrentTurnBlue ? 'Blue X' : 'Red O'})
            </span>
          </div>
        ) : (
          <div className="text-xs font-black uppercase tracking-widest text-slate-400 mt-1">
            Match Status: {winner === 'draw' ? '🤝 DRAW GAME' : '👑 VICTORY'}
          </div>
        )}
      </div>

      {/* 3x3 TIC TAC TOE GRID (HOLDERS) */}
      <div className="grid grid-cols-3 gap-3 bg-[#121624] p-4 rounded-3xl border-2 border-slate-800 shadow-2xl my-2">
        {board.map((cellValue, idx) => {
          const isWinningCell = winningLine && winningLine.includes(idx);
          const isDisabled = cellValue !== null || winner !== null;

          return (
            <button
              key={idx}
              disabled={isDisabled}
              onClick={() => handleCellClick(idx)}
              className={`w-24 h-24 sm:w-28 sm:h-28 rounded-2xl border-2 flex items-center justify-center p-3 transition-all duration-200 transform ${
                isWinningCell
                  ? 'bg-emerald-500/20 border-emerald-400 shadow-[0_0_25px_rgba(52,211,153,0.5)] scale-105 z-10'
                  : cellValue === 'blue'
                    ? 'bg-sky-500/10 border-sky-500/40 shadow-[0_0_15px_rgba(56,189,248,0.2)]'
                    : cellValue === 'red'
                      ? 'bg-rose-500/10 border-rose-500/40 shadow-[0_0_15px_rgba(244,63,94,0.2)]'
                      : 'bg-[#0f121d] border-slate-800 hover:border-indigo-500/50 hover:bg-[#161c2e] active:scale-95 cursor-pointer'
              }`}
            >
              {cellValue === 'blue' && (
                <img
                  src={blueTeamImg}
                  alt="Blue Team X"
                  className="w-full h-full object-contain drop-shadow-[0_0_10px_rgba(56,189,248,0.6)] animate-fadeIn"
                />
              )}
              {cellValue === 'red' && (
                <img
                  src={redTeamImg}
                  alt="Red Team O"
                  className="w-full h-full object-contain drop-shadow-[0_0_10px_rgba(244,63,94,0.6)] animate-fadeIn"
                />
              )}
            </button>
          );
        })}
      </div>

      {/* WINNER / DRAW BANNER (DISPLAYED BELOW GRID) */}
      {winner && (
        <div className={`w-full max-w-md p-5 rounded-3xl border-2 my-4 shadow-2xl animate-bounce-short ${
          winner === 'draw'
            ? 'bg-gradient-to-r from-slate-800/80 via-slate-700/80 to-slate-800/80 border-slate-500 text-slate-200'
            : winner === 'blue'
              ? 'bg-gradient-to-r from-sky-500/20 via-blue-500/30 to-sky-500/20 border-sky-400 text-sky-200 shadow-sky-500/20'
              : 'bg-gradient-to-r from-rose-500/20 via-red-500/30 to-rose-500/20 border-rose-400 text-rose-200 shadow-rose-500/20'
        }`}>
          <div className="text-3xl mb-1">{winner === 'draw' ? '🤝' : '👑'}</div>
          <h3 className="text-xl font-black uppercase mb-1">
            {winner === 'draw' ? 'IT\'S A TIE / DRAW!' : `VICTORY FOR ${winner.toUpperCase()} TEAM!`}
          </h3>
          <p className="text-xs font-bold leading-relaxed">
            {winner === 'draw'
              ? 'All 9 holders were filled with no 3-in-a-row combination!'
              : `${winner === 'blue' ? bluePlayer.name : redPlayer.name} connected 3 symbols in a row!`}
          </p>
        </div>
      )}

      {/* ALWAYS AVAILABLE RESET / NEW ROUND BUTTON */}
      <div className="mt-4">
        <button
          onClick={() => {
            setRoundNumber(prev => prev + 1);
            startNewRound();
          }}
          className="px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-wider transition-all duration-200 shadow-xl bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white cursor-pointer active:scale-95 flex items-center gap-2"
        >
          <span>🔄 Start New Round</span>
        </button>
      </div>

    </div>
  );
}
