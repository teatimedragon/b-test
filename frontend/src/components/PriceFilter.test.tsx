import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import PriceFilter from './PriceFilter';

async function open() {
  const user = userEvent.setup();
  const onApply = vi.fn();
  render(<PriceFilter onApply={onApply} />);
  await user.click(screen.getByRole('button', { name: /price/i }));
  return { user, onApply };
}

describe('PriceFilter', () => {
  it('opens a panel with minimum, maximum and Apply', async () => {
    await open();
    expect(screen.getByLabelText('Minimum')).toBeInTheDocument();
    expect(screen.getByLabelText('Maximum')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Apply' })).toBeEnabled();
  });

  it('blocks a minimum above the maximum', async () => {
    const { user, onApply } = await open();
    await user.type(screen.getByLabelText('Minimum'), '500000');
    await user.type(screen.getByLabelText('Maximum'), '300000');

    expect(screen.getByRole('alert')).toHaveTextContent(/minimum price can.t be higher than maximum/i);
    const apply = screen.getByRole('button', { name: 'Apply' });
    expect(apply).toBeDisabled();
    await user.click(apply);
    expect(onApply).not.toHaveBeenCalled();
  });

  it('allows equal bounds and applies them', async () => {
    const { user, onApply } = await open();
    await user.type(screen.getByLabelText('Minimum'), '300000');
    await user.type(screen.getByLabelText('Maximum'), '300000');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(onApply).toHaveBeenCalledWith(300000, 300000);
  });

  it('treats an empty input as no bound', async () => {
    const { user, onApply } = await open();
    await user.type(screen.getByLabelText('Maximum'), '250000');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(onApply).toHaveBeenCalledWith(undefined, 250000);
  });
});
