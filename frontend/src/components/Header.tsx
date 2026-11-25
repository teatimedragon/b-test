import type { ReactNode } from 'react';

export default function Header({ action }: { action?: ReactNode }) {
  return (
    <header className="app-header">
      <h1 className="brand">Some Company</h1>
      <div className="header-action">{action}</div>
    </header>
  );
}
