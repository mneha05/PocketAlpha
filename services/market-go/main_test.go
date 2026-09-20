package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestHealth(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rec := httptest.NewRecorder()
	healthHandler(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}

	var body map[string]any
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatal(err)
	}
	if body["status"] != "ok" || body["service"] != "pocketalpha-market-go" {
		t.Fatalf("unexpected body: %#v", body)
	}
}

func TestOverview(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/v1/market/overview", nil)
	rec := httptest.NewRecorder()
	overviewHandler(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected 200, got %d", rec.Code)
	}

	var body marketOverview
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatal(err)
	}
	if len(body.Indices) != 3 {
		t.Fatalf("expected 3 indices, got %d", len(body.Indices))
	}
	if len(body.Movers) != 6 {
		t.Fatalf("expected 6 movers, got %d", len(body.Movers))
	}
	if body.Source != "PocketAlpha simulated market" {
		t.Fatalf("unexpected source: %s", body.Source)
	}
}
