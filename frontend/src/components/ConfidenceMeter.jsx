import { useEffect, useState } from 'react';

// Mirrors CONFIDENCE_THRESHOLD in ai-service/.env. Keep the two in sync.
export const CONFIDENCE_THRESHOLD = 0.75;

export default function ConfidenceMeter({ value }) {
  const pct = Math.round(value * 100);
  const cutoff = Math.round(CONFIDENCE_THRESHOLD * 100);
  const held = value < CONFIDENCE_THRESHOLD;
  const tone = held ? 'is-held' : 'is-ok';

  // Start empty, then fill, so the bar visibly answers the click that opened it
  const [width, setWidth] = useState(0);
  useEffect(() => {
    const id = setTimeout(() => setWidth(pct), 40);
    return () => clearTimeout(id);
  }, [pct]);

  return (
    <div className="meter">
      <div className="meter-head">
        <span className="meter-label">Model confidence</span>
        <span className={`meter-value ${tone}`}>{pct}%</span>
      </div>
      <div
        className="meter-track"
        role="img"
        aria-label={`Confidence ${pct} percent. Documents below ${cutoff} percent are held for review.`}
      >
        <div className={`meter-fill ${tone}`} style={{ width: `${width}%` }} />
        <div className="meter-notch" style={{ left: `${cutoff}%` }} />
      </div>
      <div className="meter-scale">
        <span className="meter-cutoff" style={{ left: `${cutoff}%` }}>
          Review cutoff {cutoff}%
        </span>
      </div>
    </div>
  );
}
