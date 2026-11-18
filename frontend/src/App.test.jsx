import { afterEach, expect, test, vi } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import App from './App.jsx';

afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

test('loads products from the API into the catalogue', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => ({
    ok: true,
    json: async () => [{id: 1, name: 'Farm Fresh Milk (1 L)', sku:'MILK-01', price:58, stockQuantity:5}],
  })));
  render(<App />);
  expect((await screen.findAllByText('Farm Fresh Milk (1 L)')).length).toBeGreaterThan(0);
});

test('shows a network failure rather than silently failing', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => { throw new Error('offline'); }));
  render(<App />);
  expect(await screen.findByText('Network error. Check whether the backend is running.')).toBeTruthy();
});
