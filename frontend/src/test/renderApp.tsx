import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AppRoutes } from '../App';
import { DefaultLocationProvider } from '../context/DefaultLocationContext';

export function renderApp(path = '/') {
  return render(
    <DefaultLocationProvider>
      <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AppRoutes />
      </MemoryRouter>
    </DefaultLocationProvider>,
  );
}
