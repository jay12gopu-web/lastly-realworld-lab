import { useEffect, useRef, useState } from 'react';

const rupees = value => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 2,
}).format(Number(value));

async function jsonFetch(url, options) {
  let response;
  try { response = await fetch(url, options); }
  catch { throw new Error('Network error. Check whether the backend is running.'); }
  let payload;
  try { payload = await response.json(); }
  catch { payload = {}; }
  if (!response.ok) throw new Error(payload.message || `Server returned ${response.status}`);
  return payload;
}

export default function App() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [order, setOrder] = useState(null);
  const [writesEnabled, setWritesEnabled] = useState(false);
  const [authEnabled, setAuthEnabled] = useState(false);
  // Keep the short-lived access token in memory; refresh requires login again.
  const [token, setToken] = useState('');
  const [authMode, setAuthMode] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState('');
  const [quantity, setQuantity] = useState(1);
  const [selectedId, setSelectedId] = useState(null);
  // Keep the same key when retrying a failed/network-timed-out request.
  const pendingKey = useRef(null);

  async function refresh() {
    const result = await jsonFetch('/api/products');
    setProducts(result);
    if (!selectedId && result.length) setSelectedId(result[0].id);
  }

  useEffect(() => {
    let active = true;
    jsonFetch('/api/products')
      .then(result => {
        if (!active) return;
        setProducts(result);
        if (result.length) setSelectedId(result[0].id);
      })
      .catch(err => { if (active) setError(err.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    jsonFetch('/api/config')
      .then(config => { setWritesEnabled(config.ordersEnabled === true); setAuthEnabled(config.authenticationEnabled === true); })
      .catch(() => setWritesEnabled(false));
  }, []);

  async function submitAuth(event) {
    event.preventDefault();
    setAuthBusy(true); setAuthError('');
    try {
      if (authMode === 'register') {
        await jsonFetch('/api/auth/register', { method: 'POST', headers: {'Content-Type':'application/json'},
          body: JSON.stringify({ email, password }) });
      }
      const response = await jsonFetch('/api/auth/login', { method: 'POST',
        headers: {'Content-Type':'application/json'}, body: JSON.stringify({ email, password }) });
      setToken(response.accessToken); setPassword('');
    } catch (err) { setAuthError(err.message); }
    finally { setAuthBusy(false); }
  }

  function chooseProduct(id) {
    setSelectedId(id);
    setQuantity(1);
    setError('');
    setOrder(null);
    pendingKey.current = null;
  }

  async function placeOrder() {
    if (!writesEnabled || !selectedId || busyId || !token) return;
    setError('');
    setOrder(null);
    const key = pendingKey.current ?? crypto.randomUUID();
    pendingKey.current = key;
    setBusyId(selectedId);
    try {
      const result = await jsonFetch('/api/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ productId: selectedId, quantity, idempotencyKey: key }),
      });
      setOrder(result);
      pendingKey.current = null;
      await refresh();
    } catch (err) { setError(err.message); }
    finally { setBusyId(null); }
  }

  const selected = products.find(p => p.id === selectedId);
  return <div className="app-shell">
    <header className="header">
      <div className="brand"><span className="brand-mark">L</span><div><strong>Lastly</strong><small>ENGINEERING LAB</small></div></div>
      <div className="header-right"><span className="live-dot" /> Full-stack demo <span className="version">v0.1</span></div>
    </header>

    <main className="content">
      <section className="hero">
        <p className="eyebrow">JAVA 21 · SPRING BOOT · REACT · MYSQL</p>
        <h1>Real orders. <em>Real engineering.</em></h1>
        <p>This learning storefront demonstrates database transactions, inventory locks, idempotent requests, API failures, health checks and deployment workflows.</p>
        <div className="pill-row"><span>✓ Concurrent stock protection</span><span>✓ Flyway database migrations</span><span>✓ Traceable API requests</span></div>
      </section>

      {authEnabled && <section className="checkout" style={{maxWidth:'550px',marginBottom:'24px'}}>
        <p className="eyebrow">SECURE CUSTOMER ACCESS</p>
        {token ? <div className="checkout-row"><strong>Signed in: {email}</strong>
          <button type="button" onClick={() => { setToken(''); setOrder(null); }}>Log out</button></div> : <>
          <h2>{authMode === 'register' ? 'Create demo account' : 'Customer login'}</h2>
          <form onSubmit={submitAuth} style={{display:'grid',gap:'10px'}}>
            <label>Email <input aria-label="Email" type="email" required autoComplete="username"
              value={email} onChange={e => setEmail(e.target.value)}/></label>
            <label>Password <input aria-label="Password" type="password" required minLength={authMode==='register'?12:1}
              autoComplete={authMode==='register'?'new-password':'current-password'}
              value={password} onChange={e => setPassword(e.target.value)}/></label>
            <button className="place-button" disabled={authBusy} type="submit">
              {authBusy ? 'Checking…' : authMode === 'register' ? 'Register and sign in' : 'Sign in'}
            </button>
            <button type="button" onClick={() => { setAuthMode(authMode==='login'?'register':'login'); setAuthError(''); }}>
              {authMode==='login' ? 'New here? Create account' : 'Already registered? Sign in'}
            </button>
          </form>
          {authError && <p role="alert">{authError}</p>}
          <p className="tip">Local lab accounts only. Never use a real password you use elsewhere.</p>
        </>}
      </section>}
      <section className="shop-layout">
        <div className="products-area">
          <div className="section-heading"><div><p className="eyebrow">01 / CATALOGUE</p><h2>Demo products</h2></div><span className="subtle">Database-backed inventory</span></div>
          {loading && <p role="status">Loading products from the Spring Boot API…</p>}
          {!loading && !products.length && <p>No products returned. Check the database migrations.</p>}
          <div className="product-grid">
          {products.map((product, index) => <button
            key={product.id}
            className={`product-card ${product.id === selectedId ? 'selected' : ''}`}
            type="button"
            onClick={() => chooseProduct(product.id)}
            aria-pressed={product.id === selectedId}>
              <div className={`product-art art-${index % 3}`} aria-hidden="true">{['🥛', '🌾', '🍞'][index % 3]}</div>
              <div className="product-info"><h3>{product.name}</h3><span className="sku">{product.sku}</span></div>
              <div className="product-bottom"><strong>{rupees(product.price)}</strong><span className={product.stockQuantity > 0 ? 'stock' : 'stock out'}>{product.stockQuantity} left</span></div>
            </button>)}
          </div>
        </div>
        <aside className="checkout">
          <p className="eyebrow">02 / CHECKOUT SIMULATOR</p>
          <h2>Place a test order</h2>
          <p className="checkout-description">No payments or real deliveries. This creates a transaction in MySQL.</p>
          <div className="checkout-row"><span>Selected item</span><strong>{selected?.name || 'Select a product'}</strong></div>
          <div className="checkout-row"><label htmlFor="quantity">Quantity</label>
            <select id="quantity" value={quantity} onChange={e => { setQuantity(Number(e.target.value)); pendingKey.current = null; }}>
              {[1,2,3,4,5].map(n => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
          <div className="total"><span>Estimated total</span><strong>{selected ? rupees(Number(selected.price) * quantity) : '—'}</strong></div>
          <button className="place-button" type="button" onClick={placeOrder} disabled={!writesEnabled || !token || !selected || busyId !== null || selected.stockQuantity < quantity}>
            {busyId ? 'Submitting…' : (!token ? 'Sign in to place order' : (writesEnabled ? 'Place demo order →' : 'Read-only demo mode'))}
          </button>
          <p className="tip">{!writesEnabled && 'Order creation is enabled only in the local/private lab. '}The backend calculates the total from trusted database prices, not this screen.</p>
          {error && <div className="error" role="alert"><strong>Request failed</strong><p>{error}</p><small>Retry the same order to test idempotency.</small></div>}
          {order && <div className="success" role="status"><strong>Order placed</strong><p>{order.productName} × {order.quantity} — {order.status}</p><small>ID: {order.id}</small></div>}
        </aside>
      </section>
      <section className="lessons"><p className="eyebrow">ENGINEERING TOPICS</p><div><span>Database locking</span><span>Retry safety</span><span>Docker builds</span><span>Integration testing</span><span>CI pipelines</span><span>Incident drills</span></div></section>
    </main>
    <footer>Lastly Real-World Lab · Educational demo only · Never enter payment details</footer>
  </div>;
}
