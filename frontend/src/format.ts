import type { Listing } from './api/types';

const wholeDollars = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
});
const withCents = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const plainNumber = new Intl.NumberFormat('en-US');

/** $450,000 (cents are shown only when the price has them). */
export function formatPrice(price: number): string {
  return Number.isInteger(price) ? wholeDollars.format(price) : withCents.format(price);
}

export function capitalize(value: string): string {
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : value;
}

/** "3 bd | 2.5 ba | 1,850 sqft | Active" */
export function formatDetails(listing: Pick<Listing, 'bedrooms' | 'bathrooms' | 'sqft' | 'status'>): string {
  return [
    `${listing.bedrooms} bd`,
    `${listing.bathrooms} ba`,
    `${plainNumber.format(listing.sqft)} sqft`,
    capitalize(listing.status),
  ].join(' | ');
}

/** "123 Main St, Alexandria, VA 22306" */
export function formatAddress(listing: Pick<Listing, 'address' | 'city' | 'state' | 'zip'>): string {
  return `${listing.address}, ${listing.city}, ${listing.state} ${listing.zip}`;
}

/** "Nov 20, 2026" for "2026-11-20", without time zone shifts. */
export function formatDate(isoDate: string): string {
  const [y, m, d] = isoDate.split('-').map(Number);
  if (!y || !m || !d) return isoDate;
  return new Date(Date.UTC(y, m - 1, d)).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  });
}

export function formatDateTime(iso: string): string {
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString('en-US');
}
