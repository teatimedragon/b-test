import { useCallback, useEffect, useState, type ChangeEvent } from 'react';
import { fetchLogs, uploadListings } from '../../api/client';
import type { LogsResponse, UploadResult } from '../../api/types';
import { formatDateTime } from '../../format';
import Pagination from '../Pagination';

export const LOG_PAGE_SIZE = 20;

async function readFileText(file: File): Promise<string> {
  if (typeof file.text === 'function') return file.text();
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () => reject(reader.error);
    reader.readAsText(file);
  });
}

/** Upload a JSON file, then show the processing log (most recent first). */
export default function UploadAndLog() {
  const [message, setMessage] = useState<{ kind: 'error' | 'success'; text: string } | null>(null);
  const [summary, setSummary] = useState<UploadResult | null>(null);
  const [uploading, setUploading] = useState(false);
  const [logs, setLogs] = useState<LogsResponse | null>(null);
  const [logError, setLogError] = useState<string | null>(null);
  const [logPage, setLogPage] = useState(0);

  const loadLogs = useCallback(async (page: number) => {
    try {
      setLogs(await fetchLogs(page, LOG_PAGE_SIZE));
      setLogError(null);
    } catch {
      setLogError('Could not load the listing log.');
    }
  }, []);

  useEffect(() => {
    void loadLogs(logPage);
  }, [loadLogs, logPage]);

  const onFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const input = event.target;
    const file = input.files?.[0];
    if (!file) return;
    setMessage(null);
    setSummary(null);

    let parsed: unknown;
    try {
      parsed = JSON.parse(await readFileText(file));
    } catch {
      parsed = undefined;
    }
    input.value = '';
    if (!Array.isArray(parsed)) {
      setMessage({ kind: 'error', text: 'File not in correct format' });
      return;
    }

    setUploading(true);
    try {
      const result = await uploadListings(parsed);
      setSummary(result);
      setMessage({ kind: 'success', text: `Uploaded ${file.name}` });
      // Show the newest entries.
      if (logPage === 0) {
        await loadLogs(0);
      } else {
        setLogPage(0);
      }
    } catch {
      setMessage({ kind: 'error', text: 'Upload failed' });
    } finally {
      setUploading(false);
    }
  };

  return (
    <>
      <section className="card-section" aria-labelledby="upload-heading">
        <h3 id="upload-heading">Upload listings</h3>
        <p className="hint">Choose a .json file containing an array of listings.</p>
        <label className="file-button">
          {uploading ? 'Uploading…' : 'Choose JSON file'}
          <input
            type="file"
            accept=".json,application/json"
            aria-label="Upload listings JSON file"
            onChange={onFile}
            disabled={uploading}
          />
        </label>
        {message && (
          <p className={message.kind === 'error' ? 'inline-error' : 'success'} role={message.kind === 'error' ? 'alert' : 'status'}>
            {message.text}
          </p>
        )}
        {summary && (
          <dl className="summary" aria-label="Upload summary">
            <div>
              <dt>Received</dt>
              <dd>{summary.received}</dd>
            </div>
            <div>
              <dt>Accepted</dt>
              <dd>{summary.accepted}</dd>
            </div>
            <div>
              <dt>Rejected</dt>
              <dd>{summary.rejected}</dd>
            </div>
          </dl>
        )}
      </section>

      <section className="card-section" aria-labelledby="log-heading">
        <h3 id="log-heading">Listing log</h3>
        {logError && (
          <p className="inline-error" role="alert">
            {logError}
          </p>
        )}
        {logs && logs.logs.length === 0 && <p className="empty-state">No uploads yet.</p>}
        {logs && logs.logs.length > 0 && (
          <div className="table-wrap">
            <table className="log-table">
              <thead>
                <tr>
                  <th>Log time</th>
                  <th>Source</th>
                  <th>Id</th>
                  <th>Address</th>
                  <th>Status</th>
                  <th>Accepted</th>
                </tr>
              </thead>
              {logs.logs.map((entry) => (
                <tbody key={entry.logId} data-testid="log-entry" className={entry.accepted ? '' : 'rejected'}>
                  <tr>
                    <td>{formatDateTime(entry.logTime)}</td>
                    <td>{entry.source}</td>
                    <td>{entry.id}</td>
                    <td>{entry.address}</td>
                    <td>{entry.status}</td>
                    <td>
                      <span className={`badge ${entry.accepted ? 'ok' : 'bad'}`}>{entry.accepted ? 'Yes' : 'No'}</span>
                    </td>
                  </tr>
                  {entry.errors.length > 0 && (
                    <tr className="error-row">
                      <td colSpan={6}>
                        <ul>
                          {entry.errors.map((error) => (
                            <li key={error.errorId}>
                              <strong>{error.errorSource}</strong>: {error.errorDetails}
                            </li>
                          ))}
                        </ul>
                      </td>
                    </tr>
                  )}
                </tbody>
              ))}
            </table>
          </div>
        )}
        {logs && <Pagination meta={logs.meta} onPage={setLogPage} />}
      </section>
    </>
  );
}
