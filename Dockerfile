# Build stage
FROM golang:1.26-alpine AS builder
WORKDIR /app
RUN apk add --no-cache git
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -o medibill ./cmd/server/

# Runtime stage
FROM alpine:3.20
WORKDIR /app
RUN apk add --no-cache ca-certificates tzdata
COPY --from=builder /app/medibill .
COPY web/static ./web/static
COPY web/templates ./web/templates
RUN mkdir -p data uploads backups
ENV PORT=8086
ENV DB_DRIVER=sqlite
ENV DB_DSN=data/medical_billing.db
EXPOSE 8086
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -qO- http://localhost:8086/login || exit 1
CMD ["./medibill"]
