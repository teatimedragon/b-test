import Dropdown from './Dropdown';

export const BEDROOM_OPTIONS: { label: string; value?: number }[] = [
  { label: 'Any' },
  { label: '1+', value: 1 },
  { label: '2+', value: 2 },
  { label: '3+', value: 3 },
  { label: '4+', value: 4 },
];

export const BATHROOM_OPTIONS: { label: string; value?: number }[] = [
  { label: 'Any' },
  { label: '1+', value: 1 },
  { label: '1½+', value: 1.5 },
  { label: '2+', value: 2 },
  { label: '3+', value: 3 },
];

interface Props {
  minBedrooms?: number;
  minBathrooms?: number;
  onChange: (minBedrooms: number | undefined, minBathrooms: number | undefined) => void;
}

function ToggleRow({
  name,
  options,
  selected,
  onSelect,
}: {
  name: string;
  options: { label: string; value?: number }[];
  selected?: number;
  onSelect: (value?: number) => void;
}) {
  return (
    <div className="toggle-row" role="group" aria-label={name}>
      <span className="toggle-label">{name}</span>
      <div className="toggle-buttons">
        {options.map((option) => (
          <button
            key={option.label}
            type="button"
            className="toggle"
            aria-pressed={option.value === selected}
            onClick={() => onSelect(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
}

/** "Any" means the parameter is not sent. Each click applies immediately. */
export default function BedBathFilter({ minBedrooms, minBathrooms, onChange }: Props) {
  const parts = [
    minBedrooms !== undefined ? `${minBedrooms}+ bd` : null,
    minBathrooms !== undefined ? `${minBathrooms}+ ba` : null,
  ].filter(Boolean);
  const label = parts.length ? parts.join(', ') : 'Bedrooms & Bathrooms';

  return (
    <Dropdown label={label} active={parts.length > 0}>
      {() => (
        <div className="bedbath-panel">
          <ToggleRow
            name="Bedrooms"
            options={BEDROOM_OPTIONS}
            selected={minBedrooms}
            onSelect={(value) => onChange(value, minBathrooms)}
          />
          <ToggleRow
            name="Bathrooms"
            options={BATHROOM_OPTIONS}
            selected={minBathrooms}
            onSelect={(value) => onChange(minBedrooms, value)}
          />
        </div>
      )}
    </Dropdown>
  );
}
