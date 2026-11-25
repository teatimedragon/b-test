import { useEffect, useRef } from 'react';
import type { Listing } from '../api/types';
import { capitalize, formatAddress, formatDate, formatPrice } from '../format';
import ListingMap from './ListingMap';

/** Map, then description, then listed date and status. Closes on ×, Escape or a backdrop click. */
export default function ListingModal({ listing, onClose }: { listing: Listing; onClose: () => void }) {
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    closeRef.current?.focus();
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div className="modal-backdrop" data-testid="modal-backdrop" onMouseDown={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="listing-modal-title"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2 id="listing-modal-title">
            {formatPrice(listing.price)} <span className="modal-address">{formatAddress(listing)}</span>
          </h2>
          <button ref={closeRef} type="button" className="close" aria-label="Close" onClick={onClose}>
            ×
          </button>
        </div>
        <ListingMap latitude={listing.latitude} longitude={listing.longitude} />
        <p className="modal-description">{listing.description}</p>
        <dl className="modal-facts">
          <div>
            <dt>Listed</dt>
            <dd>{formatDate(listing.listedDate)}</dd>
          </div>
          <div>
            <dt>Status</dt>
            <dd>{capitalize(listing.status)}</dd>
          </div>
        </dl>
      </div>
    </div>
  );
}
