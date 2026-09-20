# PocketAlpha Go Market Service

A small stateless Go service used by PocketAlpha for the market overview route.

## Endpoints

- `GET /health`
- `GET /v1/market/overview`

The service uses Go's standard `net/http` `ServeMux`, JSON encoding, `log/slog` structured request logs, server timeouts, and graceful shutdown. PocketAlpha's Node API calls this service over HTTP and keeps the Android-facing `/api/market/overview` contract unchanged.

## Run

```bash
go test ./...
go run .
```
