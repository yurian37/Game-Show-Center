import React, { useState } from 'react';
import SvgEmoji from '../../../SvgEmoji';

export default function GeoLocationSetup({ value, onChange }) {
  const [newLocName, setNewLocName] = useState('');
  const [newImageUrls, setNewImageUrls] = useState({}); // key: locIndex, value: URL string input

  const minImagesRequired = value?.images_per_round || 3;

  const handleNumericChange = (field, val) => {
    onChange({
      ...value,
      [field]: Math.max(1, parseInt(val) || 1)
    });
  };

  const handleAddLocation = (e) => {
    e.preventDefault();
    const name = newLocName.trim();
    if (!name) return;

    const locations = value?.locations || [];
    onChange({
      ...value,
      locations: [...locations, { location_name: name, images: [] }]
    });
    setNewLocName('');
  };

  const handleRemoveLocation = (locIdx) => {
    const locations = value?.locations || [];
    onChange({
      ...value,
      locations: locations.filter((_, idx) => idx !== locIdx)
    });
  };

  const handleLocationNameChange = (locIdx, newName) => {
    const locations = [...(value?.locations || [])];
    locations[locIdx].location_name = newName;
    onChange({
      ...value,
      locations
    });
  };

  const handleAddFilesToLocation = (locIdx, files) => {
    if (!files || files.length === 0) return;
    const newImageUrlsList = Array.from(files).map((file) => URL.createObjectURL(file));

    const locations = [...(value?.locations || [])];
    locations[locIdx].images = [...(locations[locIdx].images || []), ...newImageUrlsList];

    onChange({
      ...value,
      locations
    });
  };

  const handleAddUrlToLocation = (locIdx) => {
    const url = (newImageUrls[locIdx] || '').trim();
    if (!url) return;

    const locations = [...(value?.locations || [])];
    locations[locIdx].images = [...(locations[locIdx].images || []), url];

    onChange({
      ...value,
      locations
    });

    setNewImageUrls({
      ...newImageUrls,
      [locIdx]: ''
    });
  };

  const handleRemoveImageFromLocation = (locIdx, imgIdx) => {
    const locations = [...(value?.locations || [])];
    locations[locIdx].images = locations[locIdx].images.filter((_, idx) => idx !== imgIdx);
    onChange({
      ...value,
      locations
    });
  };

  return (
    <div className="bg-[#1b2238] p-6 rounded-2xl border border-slate-800 animate-fadeIn">
      <h3 className="text-lg font-black text-amber-400 mb-2 flex items-center gap-2">
        GeoLocation Setup <span className="text-[10px] bg-amber-500/20 text-amber-400 px-2 py-0.5 rounded uppercase font-bold">Premium</span>
      </h3>
      <p className="text-xs text-slate-400 mb-6">
        Configure geolocation game parameters and manage locations with their visual image pools.
      </p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
            Rounds per Player
          </label>
          <input
            type="number"
            value={value?.rounds_per_player || 2}
            onChange={(e) => handleNumericChange('rounds_per_player', e.target.value)}
            className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
            min="1"
            max="10"
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-indigo-300 uppercase tracking-wider mb-2">
            Min. Images per Round
          </label>
          <input
            type="number"
            value={value?.images_per_round || 3}
            onChange={(e) => handleNumericChange('images_per_round', e.target.value)}
            className="w-full bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200"
            min="1"
            max="10"
          />
        </div>
      </div>

      <div className="border-t border-slate-800/80 pt-6">
        <h4 className="text-xs font-black text-indigo-300 uppercase tracking-wider mb-3">
          Add New Location
        </h4>
        <form onSubmit={handleAddLocation} className="flex gap-2 mb-6">
          <input
            type="text"
            placeholder="Location Name (e.g. Chichen Itza)..."
            value={newLocName}
            onChange={(e) => setNewLocName(e.target.value)}
            className="flex-grow bg-[#121624] border border-slate-800 rounded-xl px-4 py-2.5 text-sm font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-700"
          />
          <button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-500 text-white font-bold px-6 rounded-xl text-xs uppercase tracking-wider transition-colors"
          >
            Add Location
          </button>
        </form>

        <h4 className="text-xs font-black text-indigo-300 uppercase tracking-wider mb-3">
          Configured Locations ({value?.locations?.length || 0})
        </h4>

        <div className="space-y-4 max-h-[460px] overflow-y-auto bg-[#0f121d] p-4 rounded-xl border border-slate-900">
          {value?.locations?.map((loc, locIdx) => {
            const currentImgCount = loc.images?.length || 0;
            const isMinMet = currentImgCount >= minImagesRequired;

            return (
              <div key={locIdx} className="bg-[#1b2238] border border-slate-800 p-4 rounded-xl space-y-4">
                <div className="flex justify-between items-center gap-4 border-b border-slate-800 pb-3">
                  <div className="flex items-center gap-3">
                    <input
                      type="text"
                      value={loc.location_name}
                      onChange={(e) => handleLocationNameChange(locIdx, e.target.value)}
                      className="bg-transparent text-sm font-black text-indigo-300 focus:outline-none border-b border-transparent focus:border-indigo-500 pb-0.5"
                    />
                    {isMinMet ? (
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                        <span className="inline-flex items-center gap-1"><SvgEmoji name="check" /> {currentImgCount} / {minImagesRequired} min</span>
                      </span>
                    ) : (
                      <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 border border-amber-500/30">
                        <span className="inline-flex items-center gap-1"><SvgEmoji name="warning" /> {currentImgCount} / {minImagesRequired} min</span> (Need {minImagesRequired - currentImgCount} more)
                      </span>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => handleRemoveLocation(locIdx)}
                    className="text-xs text-rose-500 hover:text-rose-400 font-bold uppercase transition-colors"
                  >
                    Delete Location
                  </button>
                </div>

                <div className="space-y-3">
                  <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider">
                    Image Pool ({currentImgCount} images)
                  </label>

                  {/* VISUAL IMAGE TILES GRID */}
                  <div className="flex flex-wrap gap-3 items-center">
                    {/* UPLOAD TILE (LIKE PROFILE AVATAR PICKER) */}
                    <label className="w-20 h-20 shrink-0 rounded-xl bg-[#121624] border-2 border-dashed border-slate-700 hover:border-indigo-500 transition-colors flex flex-col items-center justify-center cursor-pointer text-slate-500 hover:text-indigo-400 group">
                      <span className="text-xl font-bold text-slate-400 group-hover:text-indigo-400">+</span>
                      <span className="text-[9px] font-bold uppercase tracking-wider text-slate-400 group-hover:text-indigo-400">Upload</span>
                      <input
                        type="file"
                        accept="image/*"
                        multiple
                        onChange={(e) => handleAddFilesToLocation(locIdx, e.target.files)}
                        className="hidden"
                      />
                    </label>

                    {/* IMAGES LIST */}
                    {loc.images?.map((img, imgIdx) => (
                      <div
                        key={imgIdx}
                        className="relative w-20 h-20 rounded-xl overflow-hidden border border-slate-700 bg-[#0f121d] group shrink-0 shadow-md"
                      >
                        <img
                          src={img}
                          alt={`Location ${loc.location_name} ${imgIdx + 1}`}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            // Fallback if URL is broken
                            e.target.onerror = null;
                            e.target.src = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="80" height="80" viewBox="0 0 24 24" fill="none" stroke="%2394a3b8" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>';
                          }}
                        />
                        <span className="absolute bottom-1 left-1 bg-black/75 backdrop-blur-xs text-white text-[9px] px-1.5 py-0.5 rounded font-mono font-bold">
                          #{imgIdx + 1}
                        </span>
                        <button
                          type="button"
                          onClick={() => handleRemoveImageFromLocation(locIdx, imgIdx)}
                          className="absolute top-1 right-1 bg-rose-600/90 hover:bg-rose-600 text-white w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold transition-all opacity-0 group-hover:opacity-100 shadow"
                          title="Remove Image"
                        >
                          <SvgEmoji name="close" />
                        </button>
                      </div>
                    ))}
                  </div>

                  {/* OPTIONAL IMAGE URL INPUT ROW */}
                  <div className="flex gap-2 pt-1">
                    <input
                      type="text"
                      placeholder="Or paste image URL (https://...)..."
                      value={newImageUrls[locIdx] || ''}
                      onChange={(e) => setNewImageUrls({ ...newImageUrls, [locIdx]: e.target.value })}
                      className="flex-grow bg-[#121624] border border-slate-850 rounded-lg px-3 py-1.5 text-xs font-bold focus:outline-none focus:border-purple-500 text-slate-200 placeholder:text-slate-700"
                    />
                    <button
                      type="button"
                      onClick={() => handleAddUrlToLocation(locIdx)}
                      className="bg-purple-600 hover:bg-purple-500 text-white font-bold px-3 rounded-lg text-[10px] uppercase tracking-wider transition-colors"
                    >
                      Add URL
                    </button>
                  </div>
                </div>
              </div>
            );
          })}

          {(!value?.locations || value.locations.length === 0) && (
            <p className="text-xs text-slate-600 py-4 text-center">No locations created yet.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export function serialize(value) {
  return {
    game: 'GeoLocation',
    roundsPerPlayer: value?.rounds_per_player || 2,
    imagesPerRound: value?.images_per_round || 3,
    locations: (value?.locations || []).map(loc => ({
      locationName: loc.location_name,
      images: loc.images || []
    }))
  };
}
