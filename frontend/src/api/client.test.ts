import { describe, expect, it } from 'vitest';
import { buildSearchQuery } from './client';

describe('buildSearchQuery', () => {
  it('always sends location and paging, and omits unset filters', () => {
    const q = buildSearchQuery({ lat: 38.7893, lng: -77.1873, page: 0, size: 20 });
    expect(Object.fromEntries(q)).toEqual({ lat: '38.7893', lng: '-77.1873', page: '0', size: '20' });
  });

  it('uses the API parameter names', () => {
    const q = buildSearchQuery({
      lat: 1,
      lng: 2,
      page: 3,
      size: 20,
      freeText: '  park ',
      minPrice: 100,
      maxPrice: 200,
      minBedrooms: 2,
      minBathrooms: 1.5,
    });
    expect(q.get('free_text')).toBe('park');
    expect(q.get('min_price')).toBe('100');
    expect(q.get('max_price')).toBe('200');
    expect(q.get('min_bedrooms')).toBe('2');
    expect(q.get('min_bathrooms')).toBe('1.5');
  });

  it('skips blank free text', () => {
    expect(buildSearchQuery({ lat: 1, lng: 2, page: 0, size: 20, freeText: '   ' }).has('free_text')).toBe(false);
  });
});
