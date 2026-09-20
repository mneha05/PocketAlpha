import test from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { createApp } from "../server.mjs";

async function withApi(run, options = {}) {
  const { server, db } = createApp({ dbPath: ":memory:", secret: "test-secret", ...options });
  await new Promise(resolve => server.listen(0, "127.0.0.1", resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try { await run(base); } finally {
    await new Promise(resolve => server.close(resolve));
    db.close();
  }
}

const call = (base, path, { method = "GET", token, body } = {}) => fetch(base + path, {
  method,
  headers: { ...(body ? { "content-type": "application/json" } : {}), ...(token ? { authorization: `Bearer ${token}` } : {}) },
  body: body ? JSON.stringify(body) : undefined
});

test("health and public market endpoints respond", async () => withApi(async base => {
  const landing = await call(base, "/");
  assert.equal(landing.status, 200);
  assert.match(await landing.text(), /PocketAlpha API is online/);
  const health = await call(base, "/health");
  assert.equal(health.status, 200);
  assert.equal((await health.json()).status, "ok");
  const market = await call(base, "/api/market/overview");
  assert.equal(market.status, 200);
  const overview = await market.json();
  assert.equal(overview.movers.length, 6);
  assert.equal(overview.service, "node-fallback");
}));

test("market overview is served by the Go service", async () => {
  let requests = 0;
  const goService = http.createServer((req, res) => {
    requests += 1;
    assert.equal(req.url, "/v1/market/overview");
    res.writeHead(200, { "content-type": "application/json" });
    res.end(JSON.stringify({ indices: [], movers: [], service: "go-market-service" }));
  });
  await new Promise(resolve => goService.listen(0, "127.0.0.1", resolve));
  const marketServiceUrl = `http://127.0.0.1:${goService.address().port}`;
  try {
    await withApi(async base => {
      const response = await call(base, "/api/market/overview");
      assert.equal(response.status, 200);
      assert.equal((await response.json()).service, "go-market-service");
      assert.equal(requests, 1);
    }, { marketServiceUrl });
  } finally {
    await new Promise(resolve => goService.close(resolve));
  }
});

test("market overview falls back when the Go service is unavailable", async () => withApi(async base => {
  const response = await call(base, "/api/market/overview");
  assert.equal(response.status, 200);
  const overview = await response.json();
  assert.equal(overview.service, "node-fallback");
  assert.equal(overview.movers.length, 6);
}, { marketServiceUrl: "http://127.0.0.1:1" }));

test("register, reject duplicate, and login", async () => withApi(async base => {
  const account = { name: "Neha", email: "neha@example.com", password: "StrongPass123!" };
  const registered = await call(base, "/api/auth/register", { method: "POST", body: account });
  assert.equal(registered.status, 201);
  assert.ok((await registered.json()).token);
  const duplicate = await call(base, "/api/auth/register", { method: "POST", body: account });
  assert.equal(duplicate.status, 409);
  const login = await call(base, "/api/auth/login", { method: "POST", body: { email: account.email, password: account.password } });
  assert.equal(login.status, 200);
  assert.ok((await login.json()).token);
}));

test("authenticated watchlist and paper-order flow persists", async () => withApi(async base => {
  const registered = await call(base, "/api/auth/register", { method: "POST", body: { name: "Tester", email: "test@example.com", password: "StrongPass123!" } });
  const { token } = await registered.json();
  const saved = await call(base, "/api/watchlist", { method: "POST", token, body: { symbol: "MSFT" } });
  assert.equal(saved.status, 201);
  const list = await call(base, "/api/watchlist", { token });
  assert.ok((await list.json()).quotes.some(item => item.symbol === "MSFT"));

  const buy = await call(base, "/api/orders", { method: "POST", token, body: { symbol: "AAPL", side: "BUY", quantity: 2 } });
  assert.equal(buy.status, 201);
  const bought = await buy.json();
  assert.equal(bought.portfolio.positions[0].quantity, 2);
  assert.ok(bought.portfolio.cash < 10000);

  const oversell = await call(base, "/api/orders", { method: "POST", token, body: { symbol: "AAPL", side: "SELL", quantity: 3 } });
  assert.equal(oversell.status, 409);
  const sell = await call(base, "/api/orders", { method: "POST", token, body: { symbol: "AAPL", side: "SELL", quantity: 1 } });
  assert.equal(sell.status, 201);
  assert.equal((await sell.json()).portfolio.positions[0].quantity, 1);
}));

test("private endpoints reject missing sessions and bad orders", async () => withApi(async base => {
  assert.equal((await call(base, "/api/portfolio")).status, 401);
  const registered = await call(base, "/api/auth/register", { method: "POST", body: { name: "Tester", email: "orders@example.com", password: "StrongPass123!" } });
  const { token } = await registered.json();
  const order = await call(base, "/api/orders", { method: "POST", token, body: { symbol: "NVDA", side: "BUY", quantity: -1 } });
  assert.equal(order.status, 400);
}));
