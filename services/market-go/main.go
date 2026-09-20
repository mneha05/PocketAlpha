package main

import (
	"context"
	"encoding/json"
	"errors"
	"log/slog"
	"math"
	"net/http"
	"os"
	"os/signal"
	"sort"
	"strconv"
	"strings"
	"syscall"
	"time"
)

type stock struct {
	Symbol string  `json:"symbol"`
	Name   string  `json:"name"`
	Price  float64 `json:"price"`
	Sector string  `json:"sector"`
}

type quote struct {
	stock
	PreviousClose float64 `json:"previousClose"`
	Change        float64 `json:"change"`
	ChangePercent float64 `json:"changePercent"`
	MarketCap     int64   `json:"marketCap"`
	IsMarketOpen  bool    `json:"isMarketOpen"`
}

type indexSnapshot struct {
	Symbol        string  `json:"symbol"`
	Price         float64 `json:"price"`
	ChangePercent float64 `json:"changePercent"`
}

type marketOverview struct {
	Indices []indexSnapshot `json:"indices"`
	Movers  []quote         `json:"movers"`
	AsOf    string          `json:"asOf"`
	Source  string          `json:"source"`
	Service string          `json:"service"`
}

var symbols = []stock{
	{"AAPL", "Apple", 234.42, "Technology"},
	{"NVDA", "NVIDIA", 181.31, "Technology"},
	{"MSFT", "Microsoft", 521.08, "Technology"},
	{"AMZN", "Amazon", 243.74, "Consumer"},
	{"GOOGL", "Alphabet", 213.52, "Communication"},
	{"META", "Meta Platforms", 676.18, "Communication"},
	{"TSLA", "Tesla", 412.67, "Automotive"},
	{"AMD", "Advanced Micro Devices", 168.93, "Technology"},
	{"JPM", "JPMorgan Chase", 301.26, "Financials"},
	{"HOOD", "Robinhood Markets", 119.84, "Financials"},
	{"NFLX", "Netflix", 132.77, "Communication"},
	{"PLTR", "Palantir", 154.61, "Technology"},
}

var indices = []indexSnapshot{
	{"S&P 500", 6894.21, 0.62},
	{"NASDAQ", 23174.08, 0.91},
	{"DOW", 46812.45, -0.14},
}

func roundMoney(v float64) float64 {
	return math.Round((v+math.SmallestNonzeroFloat64)*100) / 100
}

func currentPrice(s stock, now time.Time) float64 {
	minute := now.Unix() / 60
	drift := math.Sin((float64(minute)+float64(s.Symbol[0]))/7.0) * 0.004
	return roundMoney(s.Price * (1 + drift))
}

func quoteFor(s stock, now time.Time) quote {
	price := currentPrice(s, now)
	seed := 0
	for _, r := range s.Symbol {
		seed += int(r)
	}
	changePercent := math.Round(math.Sin(float64(seed)*1.71+float64(now.UTC().Day()))*2.8*100) / 100
	previousClose := roundMoney(price / (1 + changePercent/100))
	return quote{
		stock:         s,
		PreviousClose: previousClose,
		Change:        roundMoney(price - previousClose),
		ChangePercent: changePercent,
		MarketCap:     int64(math.Round((80 + float64(seed)*3.17) * 1_000_000_000)),
		IsMarketOpen:  true,
	}
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "method not allowed"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]any{
		"status":    "ok",
		"service":   "pocketalpha-market-go",
		"timestamp": time.Now().UTC().Format(time.RFC3339Nano),
	})
}

func overviewHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "method not allowed"})
		return
	}

	now := time.Now().UTC()
	movers := make([]quote, 0, len(symbols))
	for _, s := range symbols {
		movers = append(movers, quoteFor(s, now))
	}
	sort.Slice(movers, func(i, j int) bool {
		return math.Abs(movers[i].ChangePercent) > math.Abs(movers[j].ChangePercent)
	})
	if len(movers) > 6 {
		movers = movers[:6]
	}

	writeJSON(w, http.StatusOK, marketOverview{
		Indices: indices,
		Movers:  movers,
		AsOf:    now.Format(time.RFC3339Nano),
		Source:  "PocketAlpha simulated market",
		Service: "go-market-service",
	})
}

func logging(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		started := time.Now()
		next.ServeHTTP(w, r)
		slog.Info("request",
			"method", r.Method,
			"path", r.URL.Path,
			"duration_ms", time.Since(started).Milliseconds(),
			"remote", r.RemoteAddr,
		)
	})
}

func main() {
	handler := http.NewServeMux()
	handler.HandleFunc("GET /health", healthHandler)
	handler.HandleFunc("GET /v1/market/overview", overviewHandler)

	port := strings.TrimSpace(os.Getenv("PORT"))
	if port == "" {
		port = "8081"
	}
	if _, err := strconv.Atoi(port); err != nil {
		slog.Error("invalid PORT", "value", port)
		os.Exit(1)
	}

	server := &http.Server{
		Addr:              ":" + port,
		Handler:           logging(handler),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      10 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	go func() {
		slog.Info("market service listening", "port", port)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			slog.Error("server stopped unexpectedly", "error", err)
			os.Exit(1)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	ctx, cancel := context.WithTimeout(context.Background(), 8*time.Second)
	defer cancel()
	if err := server.Shutdown(ctx); err != nil {
		slog.Error("graceful shutdown failed", "error", err)
		os.Exit(1)
	}
	slog.Info("market service stopped")
}
