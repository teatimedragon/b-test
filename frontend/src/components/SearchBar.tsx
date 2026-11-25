import { useState, type FormEvent } from 'react';

interface Props {
  initialValue?: string;
  onSearch: (text: string) => void;
}

export function SearchIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
      <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" strokeWidth="2" />
      <line x1="16.5" y1="16.5" x2="21" y2="21" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

/** Search box with the icon on its left. Enter or the icon runs the search. */
export default function SearchBar({ initialValue = '', onSearch }: Props) {
  const [text, setText] = useState(initialValue);

  const submit = (event: FormEvent) => {
    event.preventDefault();
    onSearch(text.trim());
  };

  return (
    <form className="search-bar" role="search" onSubmit={submit}>
      <button type="submit" className="search-icon" aria-label="Search">
        <SearchIcon />
      </button>
      <input
        type="search"
        aria-label="Search by city, state, zip or description"
        placeholder="City, state, zip or keyword"
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
    </form>
  );
}
