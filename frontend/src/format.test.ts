import { describe, expect, it } from 'vitest';
import { formatAddress, formatDate, formatDetails, formatPrice } from './format';
import { makeListing } from './test/fixtures';

describe('format', () => {
  it('formats price as currency', () => {
    expect(formatPrice(450000)).toBe('$450,000');
    expect(formatPrice(1250000)).toBe('$1,250,000');
    expect(formatPrice(450000.5)).toBe('$450,000.50');
  });

  it('builds the pipe-separated details line', () => {
    expect(formatDetails(makeListing())).toBe('3 bd | 2.5 ba | 1,850 sqft | Active');
    expect(formatDetails(makeListing({ bedrooms: 0, bathrooms: 1, sqft: 450, status: 'sold' }))).toBe(
      '0 bd | 1 ba | 450 sqft | Sold',
    );
  });

  it('builds the full address', () => {
    expect(formatAddress(makeListing())).toBe('123 Main St, Alexandria, VA 22306');
  });

  it('formats an ISO date without shifting the day', () => {
    expect(formatDate('2026-11-20')).toBe('Nov 20, 2026');
    expect(formatDate('2026-01-01')).toBe('Jan 1, 2026');
  });
});
