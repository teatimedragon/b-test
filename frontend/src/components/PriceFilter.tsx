import { useState } from 'react';
import Dropdown from './Dropdown';

interface Props {
  minPrice?: number;
  maxPrice?: number;
  onApply: (minPrice: number | undefined, maxPrice: number | undefined) => void;
}

function parse(value: string): number | undefined | 'invalid' {
  const trimmed = value.trim();
  if (trimmed === '') return undefined;
  const n = Number(trimmed);
  return Number.isInteger(n) && n >= 0 ? n : 'invalid';
}

/** Price dropdown. Min above max shows a message and disables Apply; empty means "no bound". */
export default function PriceFilter({ minPrice, maxPrice, onApply }: Props) {
  const [min, setMin] = useState(minPrice?.toString() ?? '');
  const [max, setMax] = useState(maxPrice?.toString() ?? '');

  const minValue = parse(min);
  const maxValue = parse(max);
  const invalid = minValue === 'invalid' || maxValue === 'invalid';
  const minAboveMax =
    typeof minValue === 'number' && typeof maxValue === 'number' && minValue > maxValue;

  const label =
    minPrice !== undefined || maxPrice !== undefined
      ? `Price: ${minPrice ?? 'Any'} – ${maxPrice ?? 'Any'}`
      : 'Price';

  return (
    <Dropdown label={label} active={minPrice !== undefined || maxPrice !== undefined}>
      {(close) => (
        <form
          className="price-panel"
          onSubmit={(e) => {
            e.preventDefault();
            if (invalid || minAboveMax) return;
            onApply(minValue as number | undefined, maxValue as number | undefined);
            close();
          }}
        >
          <div className="price-inputs">
            <label>
              Minimum
              <input
                type="number"
                inputMode="numeric"
                min={0}
                step={1}
                placeholder="No min"
                value={min}
                onChange={(e) => setMin(e.target.value)}
              />
            </label>
            <label>
              Maximum
              <input
                type="number"
                inputMode="numeric"
                min={0}
                step={1}
                placeholder="No max"
                value={max}
                onChange={(e) => setMax(e.target.value)}
              />
            </label>
          </div>
          {minAboveMax && (
            <p className="inline-error" role="alert">
              Minimum price can’t be higher than maximum price.
            </p>
          )}
          {invalid && (
            <p className="inline-error" role="alert">
              Prices must be whole numbers of 0 or more.
            </p>
          )}
          <button type="submit" className="primary" disabled={invalid || minAboveMax}>
            Apply
          </button>
        </form>
      )}
    </Dropdown>
  );
}
