import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { callsTo, jsonResponse, makeListing, meta, mockFetch } from '../test/fixtures';
import { renderApp } from '../test/renderApp';

const SEARCH = '/marketplace/listing';

function searchReturning(listings = [makeListing()], total = listings.length) {
  return mockFetch({
    [`GET ${SEARCH}`]: (url) =>
      jsonResponse({ listings, meta: meta(total, Number(url.searchParams.get('page') ?? 0)) }),
  });
}

function lastSearch(fetchMock: ReturnType<typeof mockFetch>) {
  const calls = callsTo(fetchMock, SEARCH);
  return calls[calls.length - 1];
}

describe('MarketplacePage', () => {
  it('shows the Some Company header and an admin button', async () => {
    searchReturning();
    renderApp();
    expect(screen.getByRole('heading', { name: 'Some Company' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /marketplace admin/i })).toHaveAttribute('href', '/admin');
    await screen.findByTestId('listing-card');
  });

  it('first search sends the default location and no filters', async () => {
    const fetchMock = searchReturning();
    renderApp();
    await screen.findByTestId('listing-card');

    const params = lastSearch(fetchMock);
    expect(params.get('lat')).toBe('38.7893');
    expect(params.get('lng')).toBe('-77.1873');
    expect(params.get('page')).toBe('0');
    expect(params.get('size')).toBe('20');
    for (const name of ['min_price', 'max_price', 'min_bedrooms', 'min_bathrooms', 'free_text']) {
      expect(params.has(name)).toBe(false);
    }
  });

  it('renders cards with price, pipe-separated details and full address', async () => {
    searchReturning([
      makeListing(),
      makeListing({ listingId: 'redfin:9', price: 1250000, bedrooms: 5, bathrooms: 4, sqft: 3200, status: 'pending',
        address: '9 Oak Ave', city: 'Fairfax', zip: '22030' }),
    ]);
    renderApp();

    const cards = await screen.findAllByTestId('listing-card');
    expect(cards).toHaveLength(2);
    expect(within(cards[0]).getByText('$450,000')).toBeInTheDocument();
    expect(within(cards[0]).getByText('3 bd | 2.5 ba | 1,850 sqft | Active')).toBeInTheDocument();
    expect(within(cards[0]).getByText('123 Main St, Alexandria, VA 22306')).toBeInTheDocument();
    expect(within(cards[1]).getByText('$1,250,000')).toBeInTheDocument();
    expect(within(cards[1]).getByText('5 bd | 4 ba | 3,200 sqft | Pending')).toBeInTheDocument();
    expect(within(cards[1]).getByText('9 Oak Ave, Fairfax, VA 22030')).toBeInTheDocument();
  });

  it('runs free text search on Enter and on the icon', async () => {
    const user = userEvent.setup();
    const fetchMock = searchReturning();
    renderApp();
    await screen.findByTestId('listing-card');

    await user.type(screen.getByRole('searchbox'), 'Park View{Enter}');
    await waitFor(() => expect(lastSearch(fetchMock).get('free_text')).toBe('Park View'));

    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'alexandria');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => expect(lastSearch(fetchMock).get('free_text')).toBe('alexandria'));
  });

  it('applies the price range', async () => {
    const user = userEvent.setup();
    const fetchMock = searchReturning();
    renderApp();
    await screen.findByTestId('listing-card');

    await user.click(screen.getByRole('button', { name: /^price/i }));
    await user.type(screen.getByLabelText('Minimum'), '200000');
    await user.type(screen.getByLabelText('Maximum'), '500000');
    await user.click(screen.getByRole('button', { name: 'Apply' }));

    await waitFor(() => {
      expect(lastSearch(fetchMock).get('min_price')).toBe('200000');
      expect(lastSearch(fetchMock).get('max_price')).toBe('500000');
    });
  });

  it('bed and bath buttons: Any omits the parameter and 1½+ sends 1.5', async () => {
    const user = userEvent.setup();
    const fetchMock = searchReturning();
    renderApp();
    await screen.findByTestId('listing-card');

    await user.click(screen.getByRole('button', { name: /bedrooms & bathrooms/i }));
    const baths = screen.getByRole('group', { name: 'Bathrooms' });
    const beds = screen.getByRole('group', { name: 'Bedrooms' });
    expect(within(beds).getByRole('button', { name: 'Any' })).toHaveAttribute('aria-pressed', 'true');
    expect(within(baths).getByRole('button', { name: 'Any' })).toHaveAttribute('aria-pressed', 'true');

    await user.click(within(baths).getByRole('button', { name: '1½+' }));
    await waitFor(() => expect(lastSearch(fetchMock).get('min_bathrooms')).toBe('1.5'));
    expect(lastSearch(fetchMock).has('min_bedrooms')).toBe(false);

    await user.click(within(screen.getByRole('group', { name: 'Bedrooms' })).getByRole('button', { name: '2+' }));
    await waitFor(() => expect(lastSearch(fetchMock).get('min_bedrooms')).toBe('2'));

    await user.click(within(screen.getByRole('group', { name: 'Bathrooms' })).getByRole('button', { name: 'Any' }));
    await waitFor(() => expect(lastSearch(fetchMock).has('min_bathrooms')).toBe(false));
    expect(lastSearch(fetchMock).get('min_bedrooms')).toBe('2');
  });

  it('changing a filter goes back to page 0', async () => {
    const user = userEvent.setup();
    const fetchMock = searchReturning([makeListing()], 45);
    renderApp();
    await screen.findByText('Page 1 of 3');

    await user.click(screen.getByRole('button', { name: 'Next' }));
    await screen.findByText('Page 2 of 3');
    expect(lastSearch(fetchMock).get('page')).toBe('1');

    await user.type(screen.getByRole('searchbox'), 'park{Enter}');
    await waitFor(() => expect(lastSearch(fetchMock).get('page')).toBe('0'));
    expect(lastSearch(fetchMock).get('free_text')).toBe('park');
  });

  it('shows 422 messages above the results', async () => {
    mockFetch({
      [`GET ${SEARCH}`]: () =>
        jsonResponse(
          { search: [{ field: 'min_price', message: 'Validation Failure, min price is greater than max price' }] },
          422,
        ),
    });
    renderApp();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Validation Failure, min price is greater than max price',
    );
  });

  it('shows an empty state', async () => {
    searchReturning([], 0);
    renderApp();
    expect(await screen.findByText(/no listings match/i)).toBeInTheDocument();
  });

  it('opens a popup with the map, description, date and status, and closes it', async () => {
    const user = userEvent.setup();
    searchReturning([makeListing({ status: 'pending' })]);
    renderApp();
    await user.click(await screen.findByTestId('listing-card'));

    const dialog = screen.getByRole('dialog');
    const map = within(dialog).getByTestId('listing-map');
    expect(map.querySelector('.leaflet-container')).not.toBeNull();
    expect(within(dialog).getByText('Bright colonial near the park.')).toBeInTheDocument();
    expect(within(dialog).getByText('Nov 20, 2026')).toBeInTheDocument();
    expect(within(dialog).getByText('Pending')).toBeInTheDocument();

    // Order: map, then description, then date and status.
    const description = within(dialog).getByText('Bright colonial near the park.');
    const date = within(dialog).getByText('Nov 20, 2026');
    expect(map.compareDocumentPosition(description) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(description.compareDocumentPosition(date) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();

    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await user.click(screen.getByTestId('listing-card'));
    await user.click(screen.getByRole('button', { name: 'Close' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await user.click(screen.getByTestId('listing-card'));
    await act(async () => {
      screen.getByTestId('modal-backdrop').dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
