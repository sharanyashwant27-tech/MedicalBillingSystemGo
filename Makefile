.PHONY: run build test docker docker-up docker-down tidy

run:
	go run ./cmd/server/

build:
	go build -o bin/medibill ./cmd/server/

test:
	go test ./...

tidy:
	go mod tidy

docker:
	docker build -t medical-billing-system-go:1.1.0 .

docker-up:
	docker compose up --build -d

docker-down:
	docker compose down
