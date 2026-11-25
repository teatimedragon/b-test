import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';

export interface Location {
  latitude: number;
  longitude: number;
}

export const INITIAL_LOCATION: Location = { latitude: 38.7893, longitude: -77.1873 };

interface DefaultLocationValue {
  location: Location;
  setLocation: (location: Location) => void;
}

const DefaultLocationContext = createContext<DefaultLocationValue | null>(null);

/**
 * Holds the default location in memory only. It is never sent anywhere except as search
 * parameters, and it resets to INITIAL_LOCATION on a full page reload.
 */
export function DefaultLocationProvider({
  children,
  initial = INITIAL_LOCATION,
}: {
  children: ReactNode;
  initial?: Location;
}) {
  const [location, setLocation] = useState<Location>(initial);
  const value = useMemo(() => ({ location, setLocation }), [location]);
  return <DefaultLocationContext.Provider value={value}>{children}</DefaultLocationContext.Provider>;
}

export function useDefaultLocation(): DefaultLocationValue {
  const value = useContext(DefaultLocationContext);
  if (!value) {
    throw new Error('useDefaultLocation must be used inside DefaultLocationProvider');
  }
  return value;
}
