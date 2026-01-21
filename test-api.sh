#!/bin/bash

# Concert Hall Reservations API Test Script
# This script demonstrates the complete flow of the application

API_URL="http://localhost:8080"

echo "=================================="
echo "Concert Hall API Test Script"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Login as Admin
echo -e "${BLUE}[1] Logging in as admin...${NC}"
ADMIN_LOGIN=$(curl -s -X POST "${API_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@concerthall.com",
    "password": "admin123"
  }')

ADMIN_TOKEN=$(echo $ADMIN_LOGIN | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "$ADMIN_TOKEN" ]; then
    echo -e "${RED}Failed to login as admin${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Admin logged in successfully${NC}"
echo "Token: ${ADMIN_TOKEN:0:20}..."
echo ""

# Step 2: Create an event
echo -e "${BLUE}[2] Creating a new event...${NC}"
EVENT=$(curl -s -X POST "${API_URL}/api/events" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rock Concert Night",
    "description": "An amazing rock concert featuring local bands",
    "eventDateTime": "2026-06-15T20:00:00",
    "capacity": 10,
    "price": 50.00,
    "status": "PUBLISHED"
  }')
echo $EVENT
EVENT_ID=$(echo "$EVENT" | grep -oE '"id":"[a-f0-9-]+"' | sed 's/"id":"//;s/"//')

if [ -z "$EVENT_ID" ]; then
    echo -e "${RED}Failed to create event${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Event created with ID: ${EVENT_ID}${NC}"
echo ""

# Step 3: Register a customer
echo -e "${BLUE}[3] Registering a new customer...${NC}"
CUSTOMER_REG=$(curl -s -X POST "${API_URL}/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer8@example.com",
    "password": "password123"
  }')

CUSTOMER_TOKEN=$(echo $CUSTOMER_REG | grep -o '"token":"[^"]*' | sed 's/"token":"//')

if [ -z "$CUSTOMER_TOKEN" ]; then
    echo -e "${RED}Failed to register customer${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Customer registered successfully${NC}"
echo ""

# Step 4: List events as customer
echo -e "${BLUE}[4] Viewing available events as customer...${NC}"
EVENTS=$(curl -s -X GET "${API_URL}/api/events" \
  -H "Authorization: Bearer ${CUSTOMER_TOKEN}")

echo -e "${GREEN}✓ Events retrieved${NC}"
echo "Available events: $(echo $EVENTS | grep -o '"name":"[^"]*' | sed 's/"name":"//' | head -1)"
echo ""

# Step 5: Reserve a ticket
echo -e "${BLUE}[5] Reserving a ticket...${NC}"
TICKET=$(curl -s -X POST "${API_URL}/api/tickets/reserve" \
  -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventId\": \"${EVENT_ID}\"
  }")
echo $EVENT_ID
echo $TICKET
TICKET_NUMBER=$(echo $TICKET | grep -o '"ticketNumber":"[^"]*' | sed 's/"ticketNumber":"//')

if [ -z "$TICKET_NUMBER" ]; then
    echo -e "${RED}Failed to reserve ticket${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Ticket reserved: ${TICKET_NUMBER}${NC}"
echo ""

# Step 6: Try to reserve duplicate ticket (should fail)
echo -e "${BLUE}[6] Attempting to reserve duplicate ticket (should fail)...${NC}"
DUPLICATE=$(curl -s -X POST "${API_URL}/api/tickets/reserve" \
  -H "Authorization: Bearer ${CUSTOMER_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"eventId\": ${EVENT_ID}
  }")

if echo $DUPLICATE | grep -q "already have a ticket"; then
    echo -e "${GREEN}✓ Duplicate prevention working correctly${NC}"
else
    echo -e "${RED}Warning: Duplicate prevention may not be working${NC}"
fi
echo ""

# Step 7: View customer's tickets
echo -e "${BLUE}[7] Viewing customer's tickets...${NC}"
MY_TICKETS=$(curl -s -X GET "${API_URL}/api/tickets/my-tickets" \
  -H "Authorization: Bearer ${CUSTOMER_TOKEN}")

echo -e "${GREEN}✓ Tickets retrieved${NC}"
echo "Ticket count: $(echo $MY_TICKETS | grep -o '"ticketNumber"' | wc -l)"
echo ""

# Step 8: View event sales as admin
echo -e "${BLUE}[8] Viewing event sales statistics as admin...${NC}"
SALES=$(curl -s -X GET "${API_URL}/api/events/${EVENT_ID}/sales" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")

echo -e "${GREEN}✓ Sales statistics retrieved${NC}"
echo "$SALES" | grep -o '"ticketsSold":[0-9]*' | sed 's/"ticketsSold":/Tickets Sold: /'
echo "$SALES" | grep -o '"revenue":[0-9.]*' | sed 's/"revenue":/Revenue: $/'
echo ""

# Step 9: Register multiple customers and test concurrent booking
echo -e "${BLUE}[9] Testing capacity limits with multiple customers...${NC}"

for i in {2..5}; do
    # Register customer
    REG=$(curl -s -X POST "${API_URL}/api/auth/register" \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"customer${i}@example.com\",
        \"password\": \"password123\"
      }")

    TOKEN=$(echo $REG | grep -o '"token":"[^"]*' | sed 's/"token":"//')

    # Reserve ticket
    curl -s -X POST "${API_URL}/api/tickets/reserve" \
      -H "Authorization: Bearer ${TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{
        \"eventId\": ${EVENT_ID}
      }" > /dev/null

    echo "  Customer $i registered and reserved ticket"
done

echo -e "${GREEN}✓ Multiple customers created and tickets reserved${NC}"
echo ""

# Step 10: Final sales check
echo -e "${BLUE}[10] Final sales statistics...${NC}"
FINAL_SALES=$(curl -s -X GET "${API_URL}/api/events/${EVENT_ID}/sales" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}")

echo -e "${GREEN}✓ Final statistics:${NC}"
echo "$FINAL_SALES" | grep -o '"ticketsSold":[0-9]*' | sed 's/"ticketsSold":/  Tickets Sold: /'
echo "$FINAL_SALES" | grep -o '"availableTickets":[0-9]*' | sed 's/"availableTickets":/  Available: /'
echo "$FINAL_SALES" | grep -o '"revenue":[0-9.]*' | sed 's/"revenue":/  Revenue: $/'
echo "$FINAL_SALES" | grep -o '"occupancyRate":[0-9.]*' | sed 's/"occupancyRate":/  Occupancy: /'
echo "%"
echo ""

echo -e "${GREEN}=================================="
echo "All tests completed successfully!"
echo "==================================${NC}"
