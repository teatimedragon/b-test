import { Route, Routes } from 'react-router-dom';
import { DefaultLocationProvider } from './context/DefaultLocationContext';
import MarketplacePage from './pages/MarketplacePage';
import AdminPage from './pages/AdminPage';

/** Routes only; the provider sits above them so both pages share the same default location. */
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<MarketplacePage />} />
      <Route path="/admin" element={<AdminPage />} />
      <Route path="*" element={<MarketplacePage />} />
    </Routes>
  );
}

export default function App() {
  return (
    <DefaultLocationProvider>
      <AppRoutes />
    </DefaultLocationProvider>
  );
}
