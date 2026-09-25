import React, { useState, useEffect } from 'react';
import soundManager from '../../../../services/soundManager';
import SvgEmoji from '../../../SvgEmoji';

export default function RapidRhythmSetup({ value, onChange }) {
  const [newUrl, setNewUrl] = useState('');
  const [newAnswer, setNewAnswer] = useState('');
  const [newTrackName, setNewTrackName] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Preview playback state for individual tracks
  const [previewingTrackId, setPreviewingTrackId] = useState(null);

  // Clean up any preview audio on unmount
  useEffect(() => {
    return () => {
      soundManager.stopAll();
    };
  }, []);

  // Normalize tracks from value
  const tracks = Array.isArray(value?.tracks) ? value.tracks : (
    Array.isArray(value?.media_pool) ? value.media_pool.map((url, idx) => ({
      id: `track_${Date.now()}_${idx}`,
      name: `Track #${idx + 1}`,
      audioUrl: url,
      answer: `Song #${idx + 1}`,
      startTime: 0,
      spanTime: 30,
      endTime: 30,
      duration: 30
    })) : []
  );

  const roundsPerPlayer = value?.rounds_per_player || value?.roundsPerPlayer || 1;

  const handleRoundsChange = (val) => {
    const rpp = Math.max(1, parseInt(val) || 1);
    onChange({
      ...value,
      rounds_per_player: rpp,
      roundsPerPlayer: rpp,
      tracks
    });
  };

  const stopPreview = () => {
    soundManager.stopAll();
    setPreviewingTrackId(null);
  };

  const handlePreviewToggle = (track) => {
    if (previewingTrackId === track.id) {
      stopPreview();
      return;
    }

    stopPreview();

    const start = Math.max(0, track.startTime || 0);
    const end = track.endTime || (start + (track.spanTime || 30));

    soundManager.playTrack(track.audioUrl, {
      startTime: start,
      endTime: end,
      volume: 0.8,
      onEnded: () => {
        setPreviewingTrackId(null);
      },
      onError: (err) => {
        setErrorMessage(`Cannot play audio preview for "${track.name}": ${err.message || 'Error'}`);
        setPreviewingTrackId(null);
      }
    }).then(() => {
      setPreviewingTrackId(track.id);
    }).catch(err => {
      setErrorMessage(`Audio play error: ${err.message}`);
      setPreviewingTrackId(null);
    });
  };

  const extractAudioDuration = (fileOrUrl) => {
    return new Promise((resolve) => {
      const audio = new Audio();
      const isFile = fileOrUrl instanceof File;
      const src = isFile ? URL.createObjectURL(fileOrUrl) : fileOrUrl;

      audio.onloadedmetadata = () => {
        const dur = Math.round(audio.duration) || 30;
        resolve({ src, duration: dur });
      };

      audio.onerror = () => {
        resolve({ src, duration: 30 });
      };

      audio.src = src;
    });
  };

  const handleAddFiles = async (files) => {
    if (!files || files.length === 0) return;
    setErrorMessage('');
    setIsLoading(true);

    const newTrackItems = [];

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const isMp3 = file.name.toLowerCase().endsWith('.mp3') || file.type.includes('audio');
      if (!isMp3) {
        continue;
      }

      const { src, duration } = await extractAudioDuration(file);
      const cleanName = file.name.replace(/\.[^/.]+$/, "");
      const defaultSpan = Math.min(30, duration);

      newTrackItems.push({
        id: `track_${Date.now()}_${i}`,
        name: cleanName,
        audioUrl: src,
        answer: cleanName, // Default answer set to filename without extension
        startTime: 0,
        spanTime: defaultSpan,
        endTime: defaultSpan,
        duration: duration
      });
    }

    if (newTrackItems.length > 0) {
      const updated = [...tracks, ...newTrackItems];
      onChange({
        ...value,
        rounds_per_player: roundsPerPlayer,
        tracks: updated,
        media_pool: updated.map(t => t.audioUrl)
      });
    }

    setIsLoading(false);
  };

  const handleAddUrl = async (e) => {
    if (e) e.preventDefault();
    const url = newUrl.trim();
    if (!url) return;

    const answer = newAnswer.trim();
    if (!answer) {
      setErrorMessage("Please provide a mandatory Answer for this track before adding.");
      return;
    }

    setErrorMessage('');
    setIsLoading(true);

    const { duration } = await extractAudioDuration(url);
    const defaultSpan = Math.min(30, duration);
    const title = newTrackName.trim() || `Song Track #${tracks.length + 1}`;

    const newTrack = {
      id: `track_${Date.now()}`,
      name: title,
      audioUrl: url,
      answer: answer,
      startTime: 0,
      spanTime: defaultSpan,
      endTime: defaultSpan,
      duration: duration
    };

    const updated = [...tracks, newTrack];
    onChange({
      ...value,
      rounds_per_player: roundsPerPlayer,
      tracks: updated,
      media_pool: updated.map(t => t.audioUrl)
    });

    setNewUrl('');
    setNewAnswer('');
    setNewTrackName('');
    setIsLoading(false);
  };

  const handleRemoveTrack = (indexToRemove) => {
    stopPreview();
    const updated = tracks.filter((_, idx) => idx !== indexToRemove);
    onChange({
      ...value,
      rounds_per_player: roundsPerPlayer,
      tracks: updated,
      media_pool: updated.map(t => t.audioUrl)
    });
  };

  const handleTrackFieldChange = (index, field, val) => {
    const updated = tracks.map((t, idx) => {
      if (idx !== index) return t;

      const updatedTrack = { ...t };
      const dur = t.duration || 30;

      if (field === 'answer' || field === 'name') {
        updatedTrack[field] = val;
      } else if (field === 'startTime') {
        let start = Math.max(0, Math.min(dur - 1, parseInt(val) || 0));
        let end = updatedTrack.endTime || (start + 30);
        // Online constraint: window max 30s
        if (end - start > 30) {
          end = Math.min(dur, start + 30);
        }
        if (end <= start) {
          end = Math.min(dur, start + 1);
        }
        updatedTrack.startTime = start;
        updatedTrack.endTime = end;
        updatedTrack.spanTime = end - start;
      } else if (field === 'endTime') {
        let end = Math.max(1, Math.min(dur, parseInt(val) || dur));
        let start = updatedTrack.startTime || 0;
        // Online constraint: window max 30s
        if (end - start > 30) {
          start = Math.max(0, end - 30);
        }
        if (end <= start) {
          start = Math.max(0, end - 1);
        }
        updatedTrack.startTime = start;
        updatedTrack.endTime = end;
        updatedTrack.spanTime = end - start;
      } else if (field === 'spanTime') {
        let span = Math.max(1, Math.min(30, parseInt(val) || 1));
        let start = updatedTrack.startTime || 0;
        let end = Math.min(dur, start + span);
        if (end - start < span && start > 0) {
          start = Math.max(0, end - span);
        }
        updatedTrack.startTime = start;
        updatedTrack.endTime = end;
        updatedTrack.spanTime = end - start;
      }

      return updatedTrack;
    });

    onChange({
      ...value,
      rounds_per_player: roundsPerPlayer,
      tracks: updated,
      media_pool: updated.map(t => t.audioUrl)
    });
  };

  const hasEmptyAnswers = tracks.some(t => !t.answer || t.answer.trim() === '');

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn text-slate-200">
      <div className="flex justify-between items-center mb-2">
        <h3 className="text-lg font-black text-amber-400 flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5"><SvgEmoji name="music" /> Rapid Rhythm Setup</span>
          <span className="text-[10px] bg-amber-500/20 text-amber-400 px-2 py-0.5 rounded uppercase font-bold">Premium</span>
        </h3>
        <span className="text-xs font-bold text-slate-400 bg-slate-800/80 px-3 py-1 rounded-full border border-slate-700">
          Tracks: <strong className="text-amber-400">{tracks.length}</strong>
        </span>
      </div>

      <p className="text-xs text-slate-400 mb-6">
        Configure musical rounds, upload audio clips, define mandatory answers for each song, and customize the playback time window (Online max: 30s).
      </p>

      {/* ROUNDS PER PLAYER INPUT */}
      <div className="w-full max-w-xs mb-6">
        <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
          Rounds per Competitor
        </label>
        <div className="flex items-center gap-3">
          <input
            type="number"
            value={roundsPerPlayer}
            onChange={(e) => handleRoundsChange(e.target.value)}
            className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
            min="1"
            max="20"
          />
          <span className="text-xs text-slate-500 font-bold whitespace-nowrap">
            rounds / player
          </span>
        </div>
      </div>

      {/* ERROR ALERT */}
      {errorMessage && (
        <div className="mb-4 bg-rose-500/20 border border-rose-500/40 text-rose-300 text-xs p-3 rounded-xl font-bold flex items-center justify-between animate-fadeIn">
          <span className="inline-flex items-center gap-1.5"><SvgEmoji name="warning" /> {errorMessage}</span>
          <button
            type="button"
            onClick={() => setErrorMessage('')}
            className="text-rose-400 hover:text-white font-bold ml-2 text-sm cursor-pointer"
          >
            <SvgEmoji name="close" />
          </button>
        </div>
      )}

      {/* MANDATORY ANSWER NOTICE ALERT IF ANY TRACK HAS EMPTY ANSWER */}
      {hasEmptyAnswers && (
        <div className="mb-4 bg-amber-500/20 border border-amber-500/40 text-amber-300 text-xs p-3 rounded-xl font-bold flex items-center gap-2">
          <span className="inline-flex items-center gap-1.5"><SvgEmoji name="warning" /> Every audio track must have a non-empty Answer. Please fill in the missing answers below.</span>
        </div>
      )}

      <div className="border-t border-slate-800/80 pt-6">
        <div className="flex justify-between items-center mb-3">
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider">
            Audio Tracks Pool & Trimming Controls
          </label>
          <span className="text-[10px] font-bold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-md border border-amber-500/20">
            <span className="inline-flex items-center gap-1.5"><SvgEmoji name="stopwatch" /> Online Max Window: 30s | Mandatory Answers</span>
          </span>
        </div>

        {/* MP3 FILE UPLOAD DROPZONE */}
        <label className="w-full p-4 rounded-xl bg-[#121624] border-2 border-dashed border-slate-700 hover:border-purple-500 transition-colors flex items-center justify-center gap-3 cursor-pointer text-slate-400 hover:text-purple-300 mb-4 group">
          <span className="text-2xl inline-flex items-center justify-center"><SvgEmoji name="music" /></span>
          <div className="text-left">
            <span className="text-xs font-bold uppercase tracking-wider block text-slate-300 group-hover:text-purple-300">
              {isLoading ? 'Analyzing Audio Files & Metadata...' : 'Upload MP3 Tracks'}
            </span>
            <span className="text-[10px] text-slate-500 block">
              Click to select one or multiple audio tracks (.mp3, .wav, .m4a)
            </span>
          </div>
          <input
            type="file"
            accept="audio/*,.mp3,.wav,.m4a"
            multiple
            disabled={isLoading}
            onChange={(e) => handleAddFiles(e.target.files)}
            className="hidden"
          />
        </label>

        {/* INPUT VIA URL WITH ANSWER FIELD */}
        <form onSubmit={handleAddUrl} className="bg-[#121624] p-4 rounded-xl border border-slate-800 mb-6 space-y-3">
          <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest block">
            Add Single Track via Direct URL
          </span>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
            <input
              type="text"
              placeholder="Track Title (e.g. Bohemian Rhapsody)..."
              value={newTrackName}
              onChange={(e) => setNewTrackName(e.target.value)}
              className="bg-[#0f121d] border border-slate-800 rounded-xl px-3 py-2 text-xs font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-600"
            />
            <input
              type="text"
              placeholder="Direct Audio URL (https://.../song.mp3)..."
              value={newUrl}
              onChange={(e) => setNewUrl(e.target.value)}
              className="bg-[#0f121d] border border-slate-800 rounded-xl px-3 py-2 text-xs font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-600"
            />
            <input
              type="text"
              placeholder="Mandatory Answer (e.g. Queen - Bohemian Rhapsody)*..."
              value={newAnswer}
              onChange={(e) => setNewAnswer(e.target.value)}
              className="bg-[#0f121d] border border-amber-500/40 rounded-xl px-3 py-2 text-xs font-black focus:outline-none focus:border-amber-400 text-amber-200 placeholder:text-amber-600/70"
            />
          </div>
          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-purple-600 hover:bg-purple-500 text-white font-bold py-2 rounded-xl text-xs uppercase tracking-wider transition-colors cursor-pointer shadow-md disabled:opacity-50"
          >
            + Add Track to Pool
          </button>
        </form>

        {/* AUDIO TRACKS LIST WITH ANSWER AND TRIMMING SLIDERS */}
        <div className="space-y-4 max-h-[500px] overflow-y-auto bg-[#0f121d] p-4 rounded-xl border border-slate-900">
          {tracks.map((track, idx) => {
            const hasAnswer = track.answer && track.answer.trim().length > 0;
            const isPreviewing = previewingTrackId === track.id;
            const dur = Math.max(1, track.duration || 30);
            const start = track.startTime || 0;
            const end = track.endTime || Math.min(dur, start + (track.spanTime || 30));
            const span = end - start;

            return (
              <div
                key={track.id || idx}
                className={`bg-[#1b2238] border p-4 rounded-2xl flex flex-col gap-3 shadow-lg transition-all ${
                  !hasAnswer ? 'border-amber-500/60 ring-1 ring-amber-500/30' : 'border-slate-800'
                }`}
              >
                {/* TRACK HEADER: NUMBER, NAME, PREVIEW & DELETE */}
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 border-b border-slate-800/80 pb-2.5">
                  <div className="flex items-center gap-3 w-full sm:w-auto">
                    <span className="w-8 h-8 rounded-lg bg-purple-500/20 border border-purple-500/30 flex items-center justify-center text-purple-300 font-black text-xs shrink-0">
                      #{idx + 1}
                    </span>
                    <input
                      type="text"
                      value={track.name || `Track #${idx + 1}`}
                      onChange={(e) => handleTrackFieldChange(idx, 'name', e.target.value)}
                      placeholder="Track title"
                      className="bg-transparent text-xs font-black text-indigo-300 focus:outline-none border-b border-transparent focus:border-indigo-500 pb-0.5 truncate max-w-[200px]"
                    />
                  </div>

                  <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                    {/* PREVIEW BUTTON */}
                    <button
                      type="button"
                      onClick={() => handlePreviewToggle(track)}
                      className={`px-3 py-1 rounded-lg text-xs font-black uppercase tracking-wider flex items-center gap-1.5 transition-all cursor-pointer ${
                        isPreviewing
                          ? 'bg-rose-500 text-white shadow-[0_0_10px_rgba(244,63,94,0.5)] animate-pulse'
                          : 'bg-indigo-600/30 hover:bg-indigo-600 text-indigo-300 hover:text-white border border-indigo-500/30'
                      }`}
                    >
                      <span className="inline-flex items-center gap-1.5">{isPreviewing ? <><SvgEmoji name="stop" /> Stop Clip</> : <><SvgEmoji name="play" /> Preview Clip</>}</span>
                      <span className="font-mono text-[10px]">({start}s - {end}s)</span>
                    </button>

                    {/* DELETE BUTTON */}
                    <button
                      type="button"
                      onClick={() => handleRemoveTrack(idx)}
                      className="bg-rose-500/20 hover:bg-rose-600 text-rose-400 hover:text-white p-1.5 rounded-lg text-xs font-bold transition-colors cursor-pointer"
                      title="Remove track"
                    >
                      <SvgEmoji name="close" />
                    </button>
                  </div>
                </div>

                {/* MANDATORY ANSWER ROW */}
                <div className="grid grid-cols-1 md:grid-cols-[120px_1fr] items-center gap-2 bg-[#121624] p-2.5 rounded-xl border border-slate-800">
                  <label className="text-[11px] font-black uppercase tracking-wider flex items-center gap-1 text-emerald-400">
                    <span className="inline-flex items-center gap-1.5"><SvgEmoji name="bulb" /> Answer:</span>
                    <span className="text-rose-400">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type="text"
                      value={track.answer || ''}
                      onChange={(e) => handleTrackFieldChange(idx, 'answer', e.target.value)}
                      placeholder="Enter the correct answer (cannot be empty)..."
                      className={`w-full bg-[#0f121d] border rounded-lg px-3 py-2 text-xs font-bold text-emerald-300 focus:outline-none placeholder:text-slate-600 ${
                        !hasAnswer ? 'border-rose-500 shadow-[0_0_8px_rgba(244,63,94,0.3)]' : 'border-emerald-500/30 focus:border-emerald-400'
                      }`}
                    />
                    {!hasAnswer && (
                      <span className="absolute right-2 top-1/2 -translate-y-1/2 text-[10px] font-black text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded uppercase">
                        Required
                      </span>
                    )}
                  </div>
                </div>

                {/* TIME RANGE CONFIGURATION (START TIME, SPAN, END TIME) */}
                <div className="bg-[#121624] p-3 rounded-xl border border-slate-800 space-y-3">
                  <div className="flex flex-wrap items-center justify-between text-[11px] font-bold text-slate-400 gap-2">
                    <div className="flex items-center gap-2">
                      <span className="text-indigo-400 inline-flex items-center gap-1"><SvgEmoji name="stopwatch" /> Playback Segment:</span>
                      <span className="text-slate-200 font-mono font-black inline-flex items-center gap-1">{start}s <SvgEmoji name="arrow-right" /> {end}s</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="bg-purple-500/20 text-purple-300 px-2 py-0.5 rounded border border-purple-500/30 text-[10px] font-black">
                        Window Span: {span}s / max 30s
                      </span>
                      <span className="text-slate-500 text-[10px]">Total Track: {dur}s</span>
                    </div>
                  </div>

                  {/* RANGE SLIDERS & NUMBER INPUTS */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* START TIME */}
                    <div className="space-y-1">
                      <div className="flex justify-between text-[10px] font-black uppercase text-indigo-300">
                        <span>Start Time (0s - {dur - 1}s)</span>
                        <span className="font-mono text-indigo-400">{start}s</span>
                      </div>
                      <input
                        type="range"
                        min="0"
                        max={Math.max(0, dur - 1)}
                        value={start}
                        onChange={(e) => handleTrackFieldChange(idx, 'startTime', e.target.value)}
                        className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                      />
                    </div>

                    {/* END TIME */}
                    <div className="space-y-1">
                      <div className="flex justify-between text-[10px] font-black uppercase text-purple-300">
                        <span>End Time (max {Math.min(dur, start + 30)}s)</span>
                        <span className="font-mono text-purple-400">{end}s</span>
                      </div>
                      <input
                        type="range"
                        min={start + 1}
                        max={Math.min(dur, start + 30)}
                        value={end}
                        onChange={(e) => handleTrackFieldChange(idx, 'endTime', e.target.value)}
                        className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-purple-500"
                      />
                    </div>
                  </div>
                </div>

              </div>
            );
          })}

          {tracks.length === 0 && (
            <div className="text-center py-8 text-slate-500 text-xs">
              Audio pool is empty. Upload MP3 files or add audio URLs above to configure Rapid Rhythm tracks.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  const tracks = Array.isArray(value?.tracks) ? value.tracks : (
    Array.isArray(value?.media_pool) ? value.media_pool.map((url, idx) => ({
      id: `track_${idx}`,
      name: `Track #${idx + 1}`,
      audioUrl: url,
      answer: `Song #${idx + 1}`,
      startTime: 0,
      spanTime: 30,
      endTime: 30,
      duration: 30
    })) : []
  );

  return {
    game: 'Rapid_Rhythm',
    roundsPerPlayer: value?.rounds_per_player || value?.roundsPerPlayer || 1,
    rounds_per_player: value?.rounds_per_player || value?.roundsPerPlayer || 1,
    tracks: tracks.map(t => ({
      id: t.id,
      name: t.name || 'Song Track',
      audioUrl: t.audioUrl,
      answer: t.answer || '',
      startTime: t.startTime || 0,
      spanTime: (t.endTime || 30) - (t.startTime || 0),
      endTime: t.endTime || 30,
      duration: t.duration || 30
    })),
    mediaPool: tracks.map(t => t.audioUrl)
  };
}