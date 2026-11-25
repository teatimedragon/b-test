import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { callsTo, jsonResponse, makeListing, makeLog, meta, mockFetch } from '../test/fixtures';
import { renderApp } from '../test/renderApp';

const LOGS = 'GET /marketplace-admin/logs';
const UPLOAD = 'POST /marketplace-admin/listings';
const SEARCH = 'GET /marketplace/listing';

function jsonFile(content: string, name = 'listings.json') {
  return new File([content], name, { type: 'application/json' });
}

function uploadCalls(fetchMock: ReturnType<typeof mockFetch>) {
  return fetchMock.mock.calls.filter(([, init]) => init?.method === 'POST');
}

describe('AdminPage', () => {
  it('has a way back to the landing page', async () => {
    mockFetch({ [LOGS]: () => jsonResponse({ logs: [], meta: meta(0) }) });
    renderApp('/admin');
    expect(screen.getByRole('link', { name: /back to marketplace/i })).toHaveAttribute('href', '/');
    expect(await screen.findByText('No uploads yet.')).toBeInTheDocument();
  });

  describe('default location', () => {
    it('is pre-filled and validated', async () => {
      const user = userEvent.setup();
      mockFetch({ [LOGS]: () => jsonResponse({ logs: [], meta: meta(0) }) });
      renderApp('/admin');

      const lat = screen.getByLabelText('Latitude');
      const lng = screen.getByLabelText('Longitude');
      expect(lat).toHaveValue('38.7893');
      expect(lng).toHaveValue('-77.1873');

      await user.clear(lat);
      await user.type(lat, '91');
      await user.clear(lng);
      await user.type(lng, 'abc');
      await user.click(screen.getByRole('button', { name: 'Set default location' }));

      const alert = screen.getByRole('alert');
      expect(alert).toHaveTextContent('Latitude must be a number from -90 to 90.');
      expect(alert).toHaveTextContent('Longitude must be a number from -180 to 180.');
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });

    it('changes the location sent by the next marketplace search', async () => {
      const user = userEvent.setup();
      const fetchMock = mockFetch({
        [LOGS]: () => jsonResponse({ logs: [], meta: meta(0) }),
        [SEARCH]: () => jsonResponse({ listings: [makeListing()], meta: meta(1) }),
      });
      renderApp('/admin');

      const lat = screen.getByLabelText('Latitude');
      const lng = screen.getByLabelText('Longitude');
      await user.clear(lat);
      await user.type(lat, '40.7128');
      await user.clear(lng);
      await user.type(lng, '-74.006');
      await user.click(screen.getByRole('button', { name: 'Set default location' }));
      expect(screen.getByRole('status')).toHaveTextContent('Default location set to 40.7128, -74.006.');

      await user.click(screen.getByRole('link', { name: /back to marketplace/i }));
      await screen.findByTestId('listing-card');

      const searches = callsTo(fetchMock, '/marketplace/listing');
      expect(searches[searches.length - 1].get('lat')).toBe('40.7128');
      expect(searches[searches.length - 1].get('lng')).toBe('-74.006');

      // Returning to admin shows the saved value.
      await user.click(screen.getByRole('link', { name: /marketplace admin/i }));
      expect(screen.getByLabelText('Latitude')).toHaveValue('40.7128');
    });
  });

  describe('upload', () => {
    it('rejects a file that is not JSON without calling the server', async () => {
      const user = userEvent.setup();
      const fetchMock = mockFetch({ [LOGS]: () => jsonResponse({ logs: [], meta: meta(0) }) });
      renderApp('/admin');

      await user.upload(screen.getByLabelText('Upload listings JSON file'), jsonFile('{ not json'));

      expect(await screen.findByText('File not in correct format')).toBeInTheDocument();
      expect(uploadCalls(fetchMock)).toHaveLength(0);
    });

    it('rejects JSON that is not an array without calling the server', async () => {
      const user = userEvent.setup();
      const fetchMock = mockFetch({ [LOGS]: () => jsonResponse({ logs: [], meta: meta(0) }) });
      renderApp('/admin');

      await user.upload(screen.getByLabelText('Upload listings JSON file'), jsonFile('{"id": "1"}'));

      expect(await screen.findByText('File not in correct format')).toBeInTheDocument();
      expect(uploadCalls(fetchMock)).toHaveLength(0);
    });

    it('shows "Upload failed" when the server returns an error', async () => {
      const user = userEvent.setup();
      mockFetch({
        [LOGS]: () => jsonResponse({ logs: [], meta: meta(0) }),
        [UPLOAD]: () => jsonResponse({ error: 'Internal server error' }, 500),
      });
      renderApp('/admin');

      await user.upload(screen.getByLabelText('Upload listings JSON file'), jsonFile('[]'));
      expect(await screen.findByText('Upload failed')).toBeInTheDocument();
    });

    it('shows "Upload failed" on a network error', async () => {
      const user = userEvent.setup();
      mockFetch({
        [LOGS]: () => jsonResponse({ logs: [], meta: meta(0) }),
        [UPLOAD]: () => {
          throw new TypeError('Failed to fetch');
        },
      });
      renderApp('/admin');

      await user.upload(screen.getByLabelText('Upload listings JSON file'), jsonFile('[{"id":"1"}]'));
      expect(await screen.findByText('Upload failed')).toBeInTheDocument();
    });

    it('posts the array, shows the summary and refreshes the log list', async () => {
      const user = userEvent.setup();
      let uploaded = false;
      const fetchMock = mockFetch({
        [LOGS]: () =>
          jsonResponse(
            uploaded
              ? {
                  logs: [
                    makeLog({
                      logId: 'new-bad',
                      id: '999',
                      address: '9 Elm St',
                      accepted: false,
                      errors: [
                        { errorId: 'e1', errorSource: 'Price', errorDetails: "Price 'abc' is not a valid number" },
                      ],
                    }),
                    makeLog({ logId: 'new-good', id: '12345' }),
                  ],
                  meta: meta(2),
                }
              : { logs: [], meta: meta(0) },
          ),
        [UPLOAD]: () => {
          uploaded = true;
          return jsonResponse({ received: 2, accepted: 1, rejected: 1, results: [] });
        },
      });
      renderApp('/admin');
      await screen.findByText('No uploads yet.');

      const file = jsonFile(JSON.stringify([{ id: '12345' }, { id: '999', price: 'abc' }]));
      await user.upload(screen.getByLabelText('Upload listings JSON file'), file);

      const summary = await screen.findByLabelText('Upload summary');
      expect(within(summary).getByText('Received').nextSibling).toHaveTextContent('2');
      expect(within(summary).getByText('Accepted').nextSibling).toHaveTextContent('1');
      expect(within(summary).getByText('Rejected').nextSibling).toHaveTextContent('1');

      const [, init] = uploadCalls(fetchMock)[0];
      expect(JSON.parse(String(init?.body))).toEqual([{ id: '12345' }, { id: '999', price: 'abc' }]);

      const entries = await screen.findAllByTestId('log-entry');
      expect(entries).toHaveLength(2);
      expect(within(entries[0]).getByText('9 Elm St')).toBeInTheDocument();
      expect(within(entries[0]).getByText('No')).toBeInTheDocument();
      expect(within(entries[0]).getByText('Price')).toBeInTheDocument();
      expect(within(entries[0]).getByText(/Price 'abc' is not a valid number/)).toBeInTheDocument();
      expect(within(entries[1]).getByText('Yes')).toBeInTheDocument();
      await waitFor(() => expect(callsTo(fetchMock, '/marketplace-admin/logs').length).toBeGreaterThanOrEqual(2));
    });
  });

  it('paginates the log list', async () => {
    const user = userEvent.setup();
    const fetchMock = mockFetch({
      [LOGS]: (url) => {
        const page = Number(url.searchParams.get('page'));
        return jsonResponse({ logs: [makeLog({ logId: `p${page}`, id: `id-${page}` })], meta: meta(25, page) });
      },
    });
    renderApp('/admin');
    expect(await screen.findByText('Page 1 of 2')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(await screen.findByText('id-1')).toBeInTheDocument();
    const calls = callsTo(fetchMock, '/marketplace-admin/logs');
    expect(calls[calls.length - 1].get('page')).toBe('1');
  });
});
