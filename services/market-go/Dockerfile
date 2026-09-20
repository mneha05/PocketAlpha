FROM golang:1.23-alpine AS build
WORKDIR /src
COPY go.mod ./
COPY main.go ./
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /out/market-service ./main.go

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /out/market-service /market-service
EXPOSE 8081
USER nonroot:nonroot
ENTRYPOINT ["/market-service"]
