import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import Pagination from './Pagination';

const meta = (page: number, totalPages: number) => ({ page, size: 20, totalPages, totalElements: totalPages * 20 });

describe('Pagination', () => {
  it('disables Previous on the first page', async () => {
    const onPage = vi.fn();
    render(<Pagination meta={meta(0, 3)} onPage={onPage} />);

    expect(screen.getByText('Page 1 of 3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();

    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));
    expect(onPage).not.toHaveBeenCalled();
  });

  it('disables Next on the last page', async () => {
    const onPage = vi.fn();
    render(<Pagination meta={meta(2, 3)} onPage={onPage} />);

    expect(screen.getByText('Page 3 of 3')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled();

    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    expect(onPage).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));
    expect(onPage).toHaveBeenCalledWith(1);
  });

  it('enables both on a middle page', async () => {
    const onPage = vi.fn();
    render(<Pagination meta={meta(1, 3)} onPage={onPage} />);

    await userEvent.click(screen.getByRole('button', { name: 'Next' }));
    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));
    expect(onPage.mock.calls).toEqual([[2], [0]]);
  });

  it('disables both when there is only one page', () => {
    render(<Pagination meta={meta(0, 1)} onPage={vi.fn()} />);
    expect(screen.getByText('Page 1 of 1')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
  });

  it('renders nothing when there are no results', () => {
    const { container } = render(<Pagination meta={meta(0, 0)} onPage={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });
});
