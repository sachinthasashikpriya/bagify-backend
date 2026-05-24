#!/bin/bash
TOKEN=$(curl -s -X POST http://localhost:8085/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "sachinthasashikpriya4@gmail.com", "password": "password"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  # Try without gateway if port 8085 is user service
  TOKEN=$(curl -s -X POST http://localhost:8085/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email": "sachinthasashikpriya4@gmail.com", "password": "password"}' | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
fi

echo "Token: $TOKEN"
curl -s -v -X GET http://localhost:8080/api/v1/orders/seller \
  -H "Authorization: Bearer $TOKEN"
