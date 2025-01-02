# Use an official Go image as the builder
FROM golang:1.22-alpine AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy go mod and sum files to cache dependencies
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download

# Copy the entire project source code
COPY . .

# Build the Go application
RUN go build -o crypto-trading-service .

# Use a minimal base image for the final container
FROM alpine:latest

# Set the working directory in the container
WORKDIR /root/

# Copy the built Go binary from the builder stage
COPY --from=builder /app/crypto-trading-service .

# Expose the application port
EXPOSE 8014

# Command to run the Go service
CMD ["./crypto-trading-service"]
