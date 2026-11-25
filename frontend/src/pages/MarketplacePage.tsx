import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { SearchValidationError, searchListings } from '../api/client';
import type { Listing, PageMeta, SearchFilters } from '../api/types';
import BedBathFilter from '../components/BedBathFilter';
import Header from '../components/Header';
import ListingCard from '../components/ListingCard';
import ListingModal from '../components/ListingModal';
import Pagination from '../components/Pagination';
import PriceFilter from '../components/PriceFilter';
import SearchBar from '../components/SearchBar';
import { useDefaultLocation } from '../context/DefaultLocationContext';

export const PAGE_SIZE = 20;

export default function MarketplacePage() {
  const { location } = useDefaultLocation();
  const [filters, setFilters] = useState<SearchFilters>({});
  const [page, setPage] = useState(0);
  const [listings, setListings] = useState<Listing[]>([]);
  const [meta, setMeta] = useState<PageMeta | null>(null);
  const [errors, setErrors] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<Listing | null>(null);
  // Bumped on every search so pressing Enter again re-runs the same search.
  const [searchCount, setSearchCount] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    searchListings({ ...filters, lat: location.latitude, lng: location.longitude, page, size: PAGE_SIZE })
      .then((response) => {
        if (cancelled) return;
        setListings(response.listings);
        setMeta(response.meta);
        setErrors([]);
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        setListings([]);
        setMeta(null);
        setErrors(
          error instanceof SearchValidationError
            ? error.errors.map((e) => e.message)
            : ['Something went wrong while searching. Please try again.'],
        );
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filters, page, location, searchCount]);

  /** Any filter change goes back to page 0 and searches again. */
  const updateFilters = (change: Partial<SearchFilters>) => {
    setFilters((current) => ({ ...current, ...change }));
    setPage(0);
    setSearchCount((n) => n + 1);
  };

  return (
    <div className="page">
      <Header
        action={
          <Link className="button-link" to="/admin">
            Marketplace Admin
          </Link>
        }
      />
      <main>
        <div className="filter-bar">
          <SearchBar onSearch={(text) => updateFilters({ freeText: text || undefined })} />
          <PriceFilter
            minPrice={filters.minPrice}
            maxPrice={filters.maxPrice}
            onApply={(minPrice, maxPrice) => updateFilters({ minPrice, maxPrice })}
          />
          <BedBathFilter
            minBedrooms={filters.minBedrooms}
            minBathrooms={filters.minBathrooms}
            onChange={(minBedrooms, minBathrooms) => updateFilters({ minBedrooms, minBathrooms })}
          />
        </div>

        {errors.length > 0 && (
          <div className="error-box" role="alert">
            <ul>
              {errors.map((message) => (
                <li key={message}>{message}</li>
              ))}
            </ul>
          </div>
        )}

        {meta && (
          <p className="result-count" aria-live="polite">
            {meta.totalElements} {meta.totalElements === 1 ? 'listing' : 'listings'}
          </p>
        )}

        {!loading && errors.length === 0 && listings.length === 0 && meta && (
          <p className="empty-state">No listings match your search. Try widening your filters.</p>
        )}

        <ul className="listing-grid" aria-busy={loading}>
          {listings.map((listing) => (
            <ListingCard key={listing.listingId} listing={listing} onOpen={setSelected} />
          ))}
        </ul>

        {meta && <Pagination meta={meta} onPage={setPage} />}
      </main>

      {selected && <ListingModal listing={selected} onClose={() => setSelected(null)} />}
    </div>
  );
}
