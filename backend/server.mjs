import http from "node:http";
import { DatabaseSync } from "node:sqlite";
import { createHmac, randomBytes, scryptSync, timingSafeEqual } from "node:crypto";
import { fileURLToPath } from "node:url";

const DEFAULT_SYMBOLS = [
  { symbol: "AAPL", name: "Apple", price: 234.42, sector: "Technology" },
  { symbol: "NVDA", name: "NVIDIA", price: 181.31, sector: "Technology" },
  { symbol: "MSFT", name: "Microsoft", price: 521.08, sector: "Technology" },
  { symbol: "AMZN", name: "Amazon", price: 243.74, sector: "Consumer" },
  { symbol: "GOOGL", name: "Alphabet", price: 213.52, sector: "Communication" },
  { symbol: "META", name: "Meta Platforms", price: 676.18, sector: "Communication" },
  { symbol: "TSLA", name: "Tesla", price: 412.67, sector: "Automotive" },
  { symbol: "AMD", name: "Advanced Micro Devices", price: 168.93, sector: "Technology" },
  { symbol: "JPM", name: "JPMorgan Chase", price: 301.26, sector: "Financials" },
  { symbol: "HOOD", name: "Robinhood Markets", price: 119.84, sector: "Financials" },
  { symbol: "NFLX", name: "Netflix", price: 132.77, sector: "Communication" },
  { symbol: "PLTR", name: "Palantir", price: 154.61, sector: "Technology" }
];

const INDEX_SNAPSHOTS = [
  { symbol: "S&P 500", price: 6894.21, changePercent: 0.62 },
  { symbol: "NASDAQ", price: 23174.08, changePercent: 0.91 },
  { symbol: "DOW", price: 46812.45, changePercent: -0.14 }
];

const json = (res, status, payload) => {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "access-control-allow-origin": "*",
    "access-control-allow-headers": "authorization, content-type",
    "access-control-allow-methods": "GET,POST,DELETE,OPTIONS"
  });
  res.end(JSON.stringify(payload));
};

const landingPage = res => {
  res.writeHead(200, {
    "content-type": "text/html; charset=utf-8",
    "cache-control": "no-store"
  });
  res.end(`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>PocketAlpha API</title>
  <style>
    :root { color-scheme: dark; font-family: Inter, ui-sans-serif, system-ui, sans-serif; }
    * { box-sizing: border-box; }
    body { margin: 0; min-height: 100vh; display: grid; place-items: center; color: #f4f7f1; background: radial-gradient(circle at 25% 20%, #253614 0, #0b0e0b 32rem); }
    main { width: min(680px, calc(100% - 32px)); padding: 42px; border: 1px solid #303730; border-radius: 28px; background: rgba(17, 20, 17, .92); box-shadow: 0 24px 80px rgba(0,0,0,.45); }
    .brand { display: flex; align-items: center; gap: 12px; letter-spacing: .08em; font-weight: 800; }
    .mark { display: grid; place-items: center; width: 36px; height: 36px; border-radius: 50%; color: #080a09; background: #b7f64a; }
    h1 { margin: 42px 0 12px; max-width: 520px; font-size: clamp(2.4rem, 8vw, 4.5rem); line-height: .96; letter-spacing: -.055em; }
    p { color: #aeb7aa; line-height: 1.65; }
    .status { display: inline-flex; align-items: center; gap: 9px; margin-top: 18px; padding: 8px 12px; border-radius: 999px; color: #b7f64a; background: #243416; font-size: .85rem; font-weight: 700; }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #b7f64a; box-shadow: 0 0 18px #b7f64a; }
    nav { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 34px; }
    a { padding: 16px 18px; border: 1px solid #303730; border-radius: 14px; color: #f4f7f1; background: #191d19; text-decoration: none; font-weight: 700; }
    a:hover { border-color: #b7f64a; color: #b7f64a; }
    small { display: block; margin-top: 34px; color: #778075; }
    @media (max-width: 520px) { main { padding: 28px; } nav { grid-template-columns: 1fr; } }
  </style>
</head>
<body>
  <main>
    <div class="brand"><span class="mark">✓</span> POCKETALPHA</div>
    <h1>Paper investing,<br />end to end.</h1>
    <p>The PocketAlpha API is online. It powers authenticated watchlists, simulated market data, transactional paper orders, and portfolio accounting for the native Android client.</p>
    <div class="status"><span class="dot"></span> All systems operational</div>
    <nav>
      <a href="/health">Health check →</a>
      <a href="/api/market/overview">Market overview →</a>
    </nav>
    <small>Simulated prices and funds · No brokerage connectivity</small>
  </main>
</body>
</html>`);
};

const marketOverviewPage = (res, overview) => {
  const indexCards = overview.indices.map(index => `
    <article class="card">
      <span>${index.symbol}</span>
      <strong>${Number(index.price).toLocaleString("en-US", { minimumFractionDigits: 2 })}</strong>
      <em class="${index.changePercent >= 0 ? "up" : "down"}">${index.changePercent >= 0 ? "+" : ""}${index.changePercent}%</em>
    </article>`).join("");
  const moverRows = overview.movers.map(stock => `
    <tr>
      <td><strong>${stock.symbol}</strong><small>${stock.name}</small></td>
      <td>$${Number(stock.price).toFixed(2)}</td>
      <td class="${stock.changePercent >= 0 ? "up" : "down"}">${stock.changePercent >= 0 ? "+" : ""}${stock.changePercent}%</td>
      <td>${stock.sector}</td>
    </tr>`).join("");

  res.writeHead(200, { "content-type": "text/html; charset=utf-8", "cache-control": "no-store" });
  res.end(`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>PocketAlpha Market Overview</title>
  <style>
    :root { color-scheme: dark; font-family: Inter, ui-sans-serif, system-ui, sans-serif; background: #090b09; color: #f4f7f1; }
    * { box-sizing: border-box; }
    body { margin: 0; min-height: 100vh; background: radial-gradient(circle at 15% 0, #273b13 0, #090b09 34rem); }
    main { width: min(1000px, calc(100% - 32px)); margin: auto; padding: 56px 0 80px; }
    header { display: flex; justify-content: space-between; gap: 24px; align-items: end; margin-bottom: 28px; }
    .eyebrow { color: #b7f64a; font-size: .78rem; font-weight: 800; letter-spacing: .13em; }
    h1 { margin: 10px 0 4px; font-size: clamp(2.4rem, 7vw, 5rem); letter-spacing: -.06em; line-height: .95; }
    p, small { color: #919b8e; }
    .status { padding: 10px 14px; border: 1px solid #3a4c28; border-radius: 999px; color: #b7f64a; background: #18230f; font: inherit; font-weight: 700; white-space: nowrap; cursor: pointer; transition: border-color .2s, transform .2s, background .2s; }
    .status:hover { border-color: #b7f64a; background: #223214; transform: translateY(-1px); }
    .status:disabled { cursor: wait; opacity: .75; transform: none; }
    .indices { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 22px; }
    .card, .panel { border: 1px solid #292f29; background: rgba(19, 23, 19, .92); box-shadow: 0 22px 70px rgba(0,0,0,.25); }
    .card { display: grid; gap: 10px; padding: 22px; border-radius: 20px; }
    .card span { color: #aeb7aa; font-weight: 700; }
    .card strong { font-size: 1.7rem; }
    em { font-style: normal; font-weight: 800; }
    .up { color: #b7f64a; }
    .down { color: #ff7b72; }
    .panel { overflow: hidden; border-radius: 22px; }
    .panel h2 { margin: 0; padding: 22px 24px; border-bottom: 1px solid #292f29; }
    table { width: 100%; border-collapse: collapse; }
    th, td { padding: 16px 24px; text-align: left; border-bottom: 1px solid #242924; }
    th { color: #7f897c; font-size: .74rem; letter-spacing: .1em; text-transform: uppercase; }
    td small { display: block; margin-top: 4px; }
    footer { display: flex; justify-content: space-between; gap: 20px; margin-top: 20px; font-size: .85rem; }
    a { color: #b7f64a; }
    @media (max-width: 700px) { header, footer { align-items: start; flex-direction: column; } .indices { grid-template-columns: 1fr; } th:last-child, td:last-child { display: none; } th, td { padding: 14px; } }
  </style>
</head>
<body>
  <main>
    <header>
      <div><div class="eyebrow">POCKETALPHA · LIVE MARKET SERVICE</div><h1>Market overview</h1><p>Simulated market data for paper-investing workflows.</p></div>
      <button class="status" id="health-check" type="button" title="Click to check the live Go service">● Go service online · Check status</button>
    </header>
    <section class="indices">${indexCards}</section>
    <section class="panel">
      <h2>Top movers</h2>
      <table><thead><tr><th>Company</th><th>Price</th><th>Change</th><th>Sector</th></tr></thead><tbody>${moverRows}</tbody></table>
    </section>
    <footer><span>Updated ${new Date(overview.asOf).toLocaleString("en-US", { timeZone: "UTC" })} UTC</span><a href="/api/market/overview?format=json">View raw JSON →</a></footer>
  </main>
  <script>
    const healthButton = document.querySelector("#health-check");
    healthButton.addEventListener("click", async () => {
      healthButton.disabled = true;
      healthButton.textContent = "Checking live service…";
      try {
        const response = await fetch("/api/market/overview?format=json", { cache: "no-store", headers: { accept: "application/json" } });
        const result = await response.json();
        if (!response.ok || result.service !== "go-market-service") throw new Error("Go service unavailable");
        healthButton.textContent = "✓ Go service online · checked " + new Date().toLocaleTimeString();
      } catch {
        healthButton.textContent = "⚠ Go service check failed · Try again";
      } finally {
        healthButton.disabled = false;
      }
    });
  </script>
</body>
</html>`);
};

const readJson = async req => {
  let text = "";
  for await (const chunk of req) {
    text += chunk;
    if (text.length > 1_000_000) throw new Error("Request body is too large");
  }
  if (!text) return {};
  try { return JSON.parse(text); } catch { throw new Error("Request body must be valid JSON"); }
};

const normalizeEmail = value => String(value ?? "").trim().toLowerCase();
const normalizeSymbol = value => String(value ?? "").trim().toUpperCase();
const roundMoney = value => Math.round((value + Number.EPSILON) * 100) / 100;

const requestMarketOverview = async marketServiceUrl => {
  const response = await fetch(`${marketServiceUrl}/v1/market/overview`, {
    headers: { accept: "application/json" },
    signal: AbortSignal.timeout(1500)
  });
  if (!response.ok) throw new Error(`Go market service returned ${response.status}`);
  const overview = await response.json();
  if (!Array.isArray(overview.indices) || !Array.isArray(overview.movers)) {
    throw new Error("Go market service returned an invalid overview");
  }
  return overview;
};

const hashPassword = password => {
  const salt = randomBytes(16).toString("hex");
  const hash = scryptSync(password, salt, 64).toString("hex");
  return `${salt}:${hash}`;
};

const verifyPassword = (password, stored) => {
  const [salt, hash] = stored.split(":");
  if (!salt || !hash) return false;
  const actual = scryptSync(password, salt, 64);
  const expected = Buffer.from(hash, "hex");
  return actual.length === expected.length && timingSafeEqual(actual, expected);
};

const encode = value => Buffer.from(JSON.stringify(value)).toString("base64url");

export function createApp({
  dbPath = process.env.DB_PATH || "pocketalpha.db",
  secret = process.env.AUTH_SECRET || randomBytes(32).toString("hex"),
  marketServiceUrl = process.env.MARKET_SERVICE_URL?.replace(/\/+$/, "")
} = {}) {
  const db = new DatabaseSync(dbPath);
  db.exec(`
    PRAGMA foreign_keys = ON;
    PRAGMA journal_mode = WAL;
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      cash REAL NOT NULL DEFAULT 10000,
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    );
    CREATE TABLE IF NOT EXISTS watchlist (
      user_id INTEGER NOT NULL,
      symbol TEXT NOT NULL,
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (user_id, symbol),
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
    CREATE TABLE IF NOT EXISTS orders (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id INTEGER NOT NULL,
      symbol TEXT NOT NULL,
      side TEXT NOT NULL CHECK(side IN ('BUY', 'SELL')),
      quantity REAL NOT NULL CHECK(quantity > 0),
      price REAL NOT NULL,
      total REAL NOT NULL,
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
    CREATE TABLE IF NOT EXISTS positions (
      user_id INTEGER NOT NULL,
      symbol TEXT NOT NULL,
      quantity REAL NOT NULL CHECK(quantity >= 0),
      average_cost REAL NOT NULL CHECK(average_cost >= 0),
      PRIMARY KEY (user_id, symbol),
      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );
  `);

  const signToken = userId => {
    const body = encode({ sub: userId, exp: Math.floor(Date.now() / 1000) + 86400 });
    const signature = createHmac("sha256", secret).update(body).digest("base64url");
    return `${body}.${signature}`;
  };

  const verifyToken = token => {
    if (!token?.includes(".")) return null;
    const [body, signature] = token.split(".");
    const expected = createHmac("sha256", secret).update(body).digest();
    const actual = Buffer.from(signature, "base64url");
    if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) return null;
    try {
      const payload = JSON.parse(Buffer.from(body, "base64url").toString("utf8"));
      return payload.exp > Date.now() / 1000 ? Number(payload.sub) : null;
    } catch { return null; }
  };

  const currentPrice = symbol => {
    const stock = DEFAULT_SYMBOLS.find(item => item.symbol === symbol);
    if (!stock) return null;
    const minute = Math.floor(Date.now() / 60000);
    const drift = Math.sin((minute + symbol.charCodeAt(0)) / 7) * 0.004;
    return roundMoney(stock.price * (1 + drift));
  };

  const quoteFor = stock => {
    const price = currentPrice(stock.symbol);
    const seed = [...stock.symbol].reduce((sum, char) => sum + char.charCodeAt(0), 0);
    const changePercent = Number((Math.sin(seed * 1.71 + new Date().getUTCDate()) * 2.8).toFixed(2));
    const previousClose = roundMoney(price / (1 + changePercent / 100));
    return {
      ...stock,
      price,
      previousClose,
      change: roundMoney(price - previousClose),
      changePercent,
      marketCap: Math.round((80 + seed * 3.17) * 1_000_000_000),
      isMarketOpen: true
    };
  };

  const historyFor = (symbol, range) => {
    const price = currentPrice(symbol);
    if (price == null) return null;
    const config = { "1D": [48, 5], "1W": [56, 180], "1M": [60, 720], "3M": [65, 2160], "1Y": [80, 10950] }[range] || [48, 5];
    const [count, minutes] = config;
    const seed = [...symbol].reduce((sum, char) => sum + char.charCodeAt(0), 0);
    const now = Date.now();
    const points = [];
    for (let i = 0; i < count; i++) {
      const progress = i / (count - 1);
      const longWave = Math.sin(seed * 0.13 + progress * 7.2) * 0.025;
      const shortWave = Math.sin(seed + i * 1.87) * 0.006;
      const trend = ((seed % 11) - 4) * 0.006 * (progress - 0.5);
      const value = roundMoney(price * (1 + longWave + shortWave + trend));
      points.push({ timestamp: new Date(now - (count - 1 - i) * minutes * 60000).toISOString(), value });
    }
    points[points.length - 1].value = price;
    return points;
  };

  const authUser = req => {
    const bearer = req.headers.authorization?.match(/^Bearer (.+)$/i)?.[1];
    const id = verifyToken(bearer);
    return id ? db.prepare("SELECT id, name, email, cash FROM users WHERE id = ?").get(id) : null;
  };

  const portfolioFor = user => {
    const rows = db.prepare("SELECT symbol, quantity, average_cost FROM positions WHERE user_id = ? AND quantity > 0.000001 ORDER BY symbol").all(user.id);
    const positions = rows.map(row => {
      const price = currentPrice(row.symbol);
      const marketValue = roundMoney(row.quantity * price);
      const averageCost = roundMoney(row.average_cost);
      const costBasis = row.quantity * row.average_cost;
      return {
        symbol: row.symbol,
        quantity: Number(row.quantity.toFixed(6)),
        averageCost,
        currentPrice: price,
        marketValue,
        gainLoss: roundMoney(marketValue - costBasis),
        gainLossPercent: costBasis ? Number((((marketValue - costBasis) / costBasis) * 100).toFixed(2)) : 0
      };
    });
    const holdingsValue = roundMoney(positions.reduce((sum, p) => sum + p.marketValue, 0));
    return {
      cash: roundMoney(user.cash),
      holdingsValue,
      totalValue: roundMoney(user.cash + holdingsValue),
      positions,
      orders: db.prepare("SELECT id, symbol, side, quantity, price, total, created_at AS createdAt FROM orders WHERE user_id = ? ORDER BY id DESC LIMIT 30").all(user.id)
    };
  };

  const route = async (req, res) => {
    if (req.method === "OPTIONS") return json(res, 204, {});
    const url = new URL(req.url, "http://localhost");
    const path = url.pathname;

    try {
      if (req.method === "GET" && path === "/") {
        return landingPage(res);
      }

      if (req.method === "GET" && path === "/health") {
        return json(res, 200, { status: "ok", service: "pocketalpha-api", timestamp: new Date().toISOString() });
      }

      if (req.method === "POST" && path === "/api/auth/register") {
        const { name, email, password } = await readJson(req);
        const cleanName = String(name ?? "").trim();
        const cleanEmail = normalizeEmail(email);
        if (cleanName.length < 2) return json(res, 400, { error: "Please enter your name." });
        if (!/^\S+@\S+\.\S+$/.test(cleanEmail)) return json(res, 400, { error: "Please enter a valid email." });
        if (String(password ?? "").length < 8) return json(res, 400, { error: "Password must be at least 8 characters." });
        try {
          const result = db.prepare("INSERT INTO users(name, email, password_hash) VALUES (?, ?, ?)").run(cleanName, cleanEmail, hashPassword(password));
          const userId = Number(result.lastInsertRowid);
          for (const symbol of ["AAPL", "NVDA", "HOOD"]) db.prepare("INSERT INTO watchlist(user_id, symbol) VALUES (?, ?)").run(userId, symbol);
          return json(res, 201, { token: signToken(userId), user: { id: userId, name: cleanName, email: cleanEmail } });
        } catch (error) {
          if (String(error).includes("UNIQUE")) return json(res, 409, { error: "An account with that email already exists." });
          throw error;
        }
      }

      if (req.method === "POST" && path === "/api/auth/login") {
        const { email, password } = await readJson(req);
        const user = db.prepare("SELECT * FROM users WHERE email = ?").get(normalizeEmail(email));
        if (!user || !verifyPassword(String(password ?? ""), user.password_hash)) return json(res, 401, { error: "Incorrect email or password." });
        return json(res, 200, { token: signToken(user.id), user: { id: user.id, name: user.name, email: user.email } });
      }

      if (req.method === "GET" && path === "/api/market/overview") {
        let overview;
        if (marketServiceUrl) {
          try {
            overview = await requestMarketOverview(marketServiceUrl);
          } catch (error) {
            console.warn("Go market service unavailable; using local fallback", { error: error.message });
          }
        }
        if (!overview) {
          const movers = DEFAULT_SYMBOLS.map(quoteFor).sort((a, b) => Math.abs(b.changePercent) - Math.abs(a.changePercent)).slice(0, 6);
          overview = { indices: INDEX_SNAPSHOTS, movers, asOf: new Date().toISOString(), source: "PocketAlpha simulated market", service: "node-fallback" };
        }
        const wantsHtml = req.headers.accept?.includes("text/html") && url.searchParams.get("format") !== "json";
        return wantsHtml ? marketOverviewPage(res, overview) : json(res, 200, overview);
      }

      if (req.method === "GET" && path === "/api/quotes") {
        const query = (url.searchParams.get("query") || "").trim().toLowerCase();
        const symbols = (url.searchParams.get("symbols") || "").split(",").map(normalizeSymbol).filter(Boolean);
        let matches = DEFAULT_SYMBOLS;
        if (symbols.length) matches = matches.filter(stock => symbols.includes(stock.symbol));
        if (query) matches = matches.filter(stock => `${stock.symbol} ${stock.name}`.toLowerCase().includes(query));
        return json(res, 200, { quotes: matches.map(quoteFor) });
      }

      const historyMatch = path.match(/^\/api\/stocks\/([A-Za-z.]+)\/history$/);
      if (req.method === "GET" && historyMatch) {
        const symbol = normalizeSymbol(historyMatch[1]);
        const stock = DEFAULT_SYMBOLS.find(item => item.symbol === symbol);
        if (!stock) return json(res, 404, { error: "Symbol not found." });
        const range = String(url.searchParams.get("range") || "1D").toUpperCase();
        return json(res, 200, { quote: quoteFor(stock), range, points: historyFor(symbol, range) });
      }

      const user = authUser(req);
      if (!user) return json(res, 401, { error: "Your session is missing or expired." });

      if (req.method === "GET" && path === "/api/me") {
        return json(res, 200, { user: { id: user.id, name: user.name, email: user.email } });
      }

      if (req.method === "GET" && path === "/api/watchlist") {
        const rows = db.prepare("SELECT symbol FROM watchlist WHERE user_id = ? ORDER BY created_at DESC").all(user.id);
        const quotes = rows.map(row => DEFAULT_SYMBOLS.find(stock => stock.symbol === row.symbol)).filter(Boolean).map(quoteFor);
        return json(res, 200, { quotes });
      }

      if (req.method === "POST" && path === "/api/watchlist") {
        const { symbol: rawSymbol } = await readJson(req);
        const symbol = normalizeSymbol(rawSymbol);
        if (!DEFAULT_SYMBOLS.some(stock => stock.symbol === symbol)) return json(res, 404, { error: "Symbol not found." });
        db.prepare("INSERT OR IGNORE INTO watchlist(user_id, symbol) VALUES (?, ?)").run(user.id, symbol);
        return json(res, 201, { symbol, isSaved: true });
      }

      const watchlistMatch = path.match(/^\/api\/watchlist\/([A-Za-z.]+)$/);
      if (req.method === "DELETE" && watchlistMatch) {
        const symbol = normalizeSymbol(watchlistMatch[1]);
        db.prepare("DELETE FROM watchlist WHERE user_id = ? AND symbol = ?").run(user.id, symbol);
        return json(res, 200, { symbol, isSaved: false });
      }

      if (req.method === "GET" && path === "/api/portfolio") {
        return json(res, 200, portfolioFor(user));
      }

      if (req.method === "POST" && path === "/api/orders") {
        const body = await readJson(req);
        const symbol = normalizeSymbol(body.symbol);
        const side = String(body.side ?? "").toUpperCase();
        const quantity = Number(body.quantity);
        const price = currentPrice(symbol);
        if (price == null) return json(res, 404, { error: "Symbol not found." });
        if (!Number.isFinite(quantity) || quantity <= 0 || quantity > 100000) return json(res, 400, { error: "Enter a valid quantity." });
        if (!["BUY", "SELL"].includes(side)) return json(res, 400, { error: "Order side must be BUY or SELL." });
        const total = roundMoney(price * quantity);
        if (side === "BUY" && user.cash + 0.000001 < total) return json(res, 409, { error: "Not enough buying power." });
        const position = db.prepare("SELECT quantity, average_cost FROM positions WHERE user_id = ? AND symbol = ?").get(user.id, symbol);
        if (side === "SELL") {
          if (!position || position.quantity + 0.000001 < quantity) return json(res, 409, { error: "You do not own enough shares." });
        }
        db.exec("BEGIN IMMEDIATE");
        try {
          db.prepare("INSERT INTO orders(user_id, symbol, side, quantity, price, total) VALUES (?, ?, ?, ?, ?, ?)").run(user.id, symbol, side, quantity, price, total);
          const cashDelta = side === "BUY" ? -total : total;
          db.prepare("UPDATE users SET cash = cash + ? WHERE id = ?").run(cashDelta, user.id);
          if (side === "BUY") {
            if (position) {
              const nextQuantity = position.quantity + quantity;
              const nextAverage = ((position.quantity * position.average_cost) + total) / nextQuantity;
              db.prepare("UPDATE positions SET quantity = ?, average_cost = ? WHERE user_id = ? AND symbol = ?").run(nextQuantity, nextAverage, user.id, symbol);
            } else {
              db.prepare("INSERT INTO positions(user_id, symbol, quantity, average_cost) VALUES (?, ?, ?, ?)").run(user.id, symbol, quantity, price);
            }
          } else {
            const nextQuantity = position.quantity - quantity;
            if (nextQuantity <= 0.000001) db.prepare("DELETE FROM positions WHERE user_id = ? AND symbol = ?").run(user.id, symbol);
            else db.prepare("UPDATE positions SET quantity = ? WHERE user_id = ? AND symbol = ?").run(nextQuantity, user.id, symbol);
          }
          db.exec("COMMIT");
        } catch (error) {
          db.exec("ROLLBACK");
          throw error;
        }
        const refreshed = db.prepare("SELECT id, name, email, cash FROM users WHERE id = ?").get(user.id);
        return json(res, 201, { order: { symbol, side, quantity, price, total }, portfolio: portfolioFor(refreshed) });
      }

      return json(res, 404, { error: "Route not found." });
    } catch (error) {
      console.error(error);
      return json(res, 500, { error: error.message || "Unexpected server error." });
    }
  };

  return { server: http.createServer(route), db };
}

const isMain = process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1];
if (isMain) {
  const { server, db } = createApp();
  const demoEmail = normalizeEmail(process.env.DEMO_EMAIL);
  const demoPassword = process.env.DEMO_PASSWORD;
  if (demoEmail && demoPassword && !db.prepare("SELECT id FROM users WHERE email = ?").get(demoEmail)) {
    const result = db.prepare("INSERT INTO users(name, email, password_hash) VALUES (?, ?, ?)").run("Demo Investor", demoEmail, hashPassword(demoPassword));
    for (const symbol of ["AAPL", "NVDA", "HOOD"]) db.prepare("INSERT INTO watchlist(user_id, symbol) VALUES (?, ?)").run(Number(result.lastInsertRowid), symbol);
  }
  const port = Number(process.env.PORT || 8080);
  server.listen(port, "0.0.0.0", () => console.log(`PocketAlpha API listening on http://localhost:${port}`));
}
