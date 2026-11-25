import type { PageMeta } from '../api/types';

export default function Pagination({ meta, onPage }: { meta: PageMeta; onPage: (page: number) => void }) {
  if (meta.totalPages <= 0) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" disabled={meta.page <= 0} onClick={() => onPage(meta.page - 1)}>
        Previous
      </button>
      <span>
        Page {meta.page + 1} of {meta.totalPages}
      </span>
      <button type="button" disabled={meta.page >= meta.totalPages - 1} onClick={() => onPage(meta.page + 1)}>
        Next
      </button>
    </nav>
  );
}
