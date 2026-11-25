import type { Listing } from '../api/types';
import { formatAddress, formatDetails, formatPrice } from '../format';

export default function ListingCard({ listing, onOpen }: { listing: Listing; onOpen: (l: Listing) => void }) {
  return (
    <li>
      <button
        type="button"
        className="listing-card"
        data-testid="listing-card"
        onClick={() => onOpen(listing)}
        aria-label={`${formatPrice(listing.price)}, ${formatAddress(listing)}`}
      >
        <span className="card-price">{formatPrice(listing.price)}</span>
        <span className="card-details">{formatDetails(listing)}</span>
        <span className="card-address">{formatAddress(listing)}</span>
      </button>
    </li>
  );
}
