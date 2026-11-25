import { useState, type FormEvent } from 'react';
import { useDefaultLocation } from '../../context/DefaultLocationContext';

function parseCoordinate(value: string, limit: number): number | null {
  if (value.trim() === '') return null;
  const n = Number(value);
  return Number.isFinite(n) && Math.abs(n) <= limit ? n : null;
}

/** Sets the in-memory default location used to rank searches by distance. */
export default function LocationForm() {
  const { location, setLocation } = useDefaultLocation();
  const [latitude, setLatitude] = useState(String(location.latitude));
  const [longitude, setLongitude] = useState(String(location.longitude));
  const [errors, setErrors] = useState<string[]>([]);
  const [saved, setSaved] = useState<string | null>(null);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    const lat = parseCoordinate(latitude, 90);
    const lng = parseCoordinate(longitude, 180);
    const problems: string[] = [];
    if (lat === null) problems.push('Latitude must be a number from -90 to 90.');
    if (lng === null) problems.push('Longitude must be a number from -180 to 180.');
    setErrors(problems);
    if (lat === null || lng === null) {
      setSaved(null);
      return;
    }
    setLocation({ latitude: lat, longitude: lng });
    setSaved(`Default location set to ${lat}, ${lng}.`);
  };

  return (
    <section className="card-section" aria-labelledby="location-heading">
      <h3 id="location-heading">Default location</h3>
      <p className="hint">Used to rank marketplace results by distance. Resets when the page is reloaded.</p>
      <form className="location-form" onSubmit={submit} noValidate>
        <label>
          Latitude
          <input value={latitude} inputMode="decimal" onChange={(e) => setLatitude(e.target.value)} />
        </label>
        <label>
          Longitude
          <input value={longitude} inputMode="decimal" onChange={(e) => setLongitude(e.target.value)} />
        </label>
        <button type="submit" className="primary">
          Set default location
        </button>
      </form>
      {errors.length > 0 && (
        <ul className="inline-error" role="alert">
          {errors.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      )}
      {saved && (
        <p className="success" role="status">
          {saved}
        </p>
      )}
    </section>
  );
}
