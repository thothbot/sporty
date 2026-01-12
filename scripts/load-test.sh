#!/bin/bash
set -e

# Configuration
TOTAL_BETS=${1:-10000}
CONCURRENT=${2:-100}
BASE_URL=${3:-"http://localhost:8080"}
KAFKA_BOOTSTRAP="localhost:9092"
TOPIC="jackpot-bets"
NUM_JACKPOTS=${4:-5}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Stats
SUCCESS_COUNT=0
FAILURE_COUNT=0
START_TIME=0
END_TIME=0

# Array to store jackpot IDs
declare -a JACKPOT_IDS

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}   Jackpot Service Load Test - ${TOTAL_BETS} Bets${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"

    command -v curl >/dev/null 2>&1 || { echo -e "${RED}curl is required${NC}"; exit 1; }
    command -v jq >/dev/null 2>&1 || { echo -e "${RED}jq is required${NC}"; exit 1; }

    echo -e "${GREEN}Prerequisites OK${NC}"
}

# Check if services are running
check_services() {
    echo -e "${YELLOW}Checking services...${NC}"

    # Check app health
    if ! curl -sf "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        echo -e "${RED}App is not running at ${BASE_URL}${NC}"
        echo -e "${YELLOW}Start with: docker-compose up -d${NC}"
        exit 1
    fi
    echo -e "${GREEN}App is healthy${NC}"

    # Check Kafka
    if ! docker exec kafka kafka-topics --bootstrap-server kafka:29092 --list > /dev/null 2>&1; then
        echo -e "${RED}Kafka is not running${NC}"
        exit 1
    fi
    echo -e "${GREEN}Kafka is running${NC}"
}

# Get or create jackpots for testing
setup_jackpots() {
    echo -e "\n${BLUE}=== Setting Up Jackpots ===${NC}"

    # First, check for existing jackpots
    local existing=$(curl -sf "${BASE_URL}/api/v1/jackpots" 2>/dev/null)

    if [[ -n "$existing" ]]; then
        local count=$(echo "$existing" | jq 'length')
        if [[ "$count" -gt 0 ]]; then
            echo -e "${GREEN}Found ${count} existing jackpot(s)${NC}"

            # Extract jackpot IDs
            while IFS= read -r id; do
                JACKPOT_IDS+=("$id")
            done < <(echo "$existing" | jq -r '.[].id')

            echo -e "Using jackpot IDs:"
            for id in "${JACKPOT_IDS[@]}"; do
                echo -e "  - ${CYAN}${id}${NC}"
            done
            return
        fi
    fi

    # No existing jackpots, create new ones
    echo -e "${YELLOW}No existing jackpots found. Creating ${NUM_JACKPOTS} test jackpots...${NC}"

    for ((i=1; i<=NUM_JACKPOTS; i++)); do
        local jackpot_data=$(cat <<EOF
{
    "name": "Load Test Jackpot ${i}",
    "initialPoolValue": $((i * 10000)),
    "contributionType": "VARIABLE",
    "contributionPercentage": 0.05,
    "rewardType": "VARIABLE",
    "rewardChancePercentage": 0.001,
    "maxPoolLimit": 1000000
}
EOF
)
        local response=$(curl -sf -X POST "${BASE_URL}/api/v1/jackpots" \
            -H "Content-Type: application/json" \
            -d "$jackpot_data" 2>/dev/null)

        if [[ -n "$response" ]]; then
            local jackpot_id=$(echo "$response" | jq -r '.id')
            if [[ "$jackpot_id" != "null" ]]; then
                JACKPOT_IDS+=("$jackpot_id")
                echo -e "  Created jackpot ${i}: ${CYAN}${jackpot_id}${NC}"
            fi
        fi
    done

    if [[ ${#JACKPOT_IDS[@]} -eq 0 ]]; then
        echo -e "${RED}Failed to create any jackpots!${NC}"
        exit 1
    fi

    echo -e "${GREEN}Created ${#JACKPOT_IDS[@]} jackpot(s)${NC}"
}

# Export jackpot IDs for worker subshells
export_jackpots() {
    # Write jackpot IDs to a temp file for workers
    JACKPOT_FILE=$(mktemp)
    printf '%s\n' "${JACKPOT_IDS[@]}" > "$JACKPOT_FILE"
    export JACKPOT_FILE
}

# Get Kafka topic info
kafka_info() {
    echo -e "\n${BLUE}=== Kafka Topic Info ===${NC}"
    docker exec kafka kafka-topics --bootstrap-server kafka:29092 --describe --topic $TOPIC 2>/dev/null || echo "Topic not yet created"
}

# Get consumer group lag
kafka_lag() {
    echo -e "\n${BLUE}=== Consumer Group Lag ===${NC}"
    docker exec kafka kafka-consumer-groups --bootstrap-server kafka:29092 \
        --describe --group jackpot-service 2>/dev/null | head -20 || echo "No consumer group yet"
}

# Get topic message count
kafka_count() {
    local count=$(docker exec kafka kafka-run-class kafka.tools.GetOffsetShell \
        --broker-list kafka:29092 --topic $TOPIC 2>/dev/null \
        | awk -F: '{sum += $3} END {print sum}')
    echo "${count:-0}"
}

# Generate a random bet request using existing jackpot
generate_bet() {
    local user_id=$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid)

    # Pick a random jackpot from the file
    local jackpot_count=$(wc -l < "$JACKPOT_FILE")
    local random_line=$((RANDOM % jackpot_count + 1))
    local jackpot_id=$(sed -n "${random_line}p" "$JACKPOT_FILE")

    local amount=$((RANDOM % 1000 + 10))

    echo "{\"userId\":\"${user_id}\",\"jackpotId\":\"${jackpot_id}\",\"betAmount\":${amount}}"
}

# Send a single bet
send_bet() {
    local bet_data="$1"
    local response
    local http_code

    response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/bets" \
        -H "Content-Type: application/json" \
        -d "$bet_data" 2>/dev/null)

    http_code=$(echo "$response" | tail -1)

    if [[ "$http_code" == "201" ]]; then
        return 0
    else
        return 1
    fi
}

# Run the load test
run_load_test() {
    echo -e "\n${BLUE}=== Starting Load Test ===${NC}"
    echo -e "Total bets: ${TOTAL_BETS}"
    echo -e "Concurrency: ${CONCURRENT}"
    echo -e "Target: ${BASE_URL}"
    echo -e "Using ${#JACKPOT_IDS[@]} jackpot(s)"
    echo ""

    local bets_per_worker=$((TOTAL_BETS / CONCURRENT))
    local remainder=$((TOTAL_BETS % CONCURRENT))

    # Create temp directory for results
    local tmp_dir=$(mktemp -d)

    START_TIME=$(date +%s.%N)

    echo -e "${YELLOW}Launching ${CONCURRENT} parallel workers...${NC}"

    # Launch workers in parallel
    for ((w=0; w<CONCURRENT; w++)); do
        local count=$bets_per_worker
        if [[ $w -lt $remainder ]]; then
            ((count++))
        fi

        (
            local s=0 f=0
            for ((i=0; i<count; i++)); do
                local bet=$(generate_bet)
                if send_bet "$bet"; then
                    ((s++))
                else
                    ((f++))
                fi
            done
            echo "${s}:${f}" > "$tmp_dir/worker_$w"
        ) &
    done

    # Progress indicator
    echo -e "${YELLOW}Processing...${NC}"
    while [[ $(jobs -r | wc -l) -gt 0 ]]; do
        local done_count=$(ls "$tmp_dir" 2>/dev/null | wc -l)
        local percent=$((done_count * 100 / CONCURRENT))
        printf "\r  Workers completed: %d/%d (%d%%)" "$done_count" "$CONCURRENT" "$percent"
        sleep 0.5
    done

    wait

    END_TIME=$(date +%s.%N)

    # Collect results
    for f in "$tmp_dir"/worker_*; do
        if [[ -f "$f" ]]; then
            local result=$(cat "$f")
            local s=$(echo "$result" | cut -d: -f1)
            local fail=$(echo "$result" | cut -d: -f2)
            SUCCESS_COUNT=$((SUCCESS_COUNT + s))
            FAILURE_COUNT=$((FAILURE_COUNT + fail))
        fi
    done

    rm -rf "$tmp_dir"
    rm -f "$JACKPOT_FILE"

    echo ""
}

# Print results
print_results() {
    local duration=$(echo "$END_TIME - $START_TIME" | bc)
    local rps=$(echo "scale=2; $SUCCESS_COUNT / $duration" | bc)
    local success_rate=$(echo "scale=2; $SUCCESS_COUNT * 100 / $TOTAL_BETS" | bc)

    echo -e "\n${BLUE}=== Load Test Results ===${NC}"
    echo -e "Duration:      ${duration} seconds"
    echo -e "Total sent:    ${TOTAL_BETS}"
    echo -e "${GREEN}Successful:    ${SUCCESS_COUNT}${NC}"
    echo -e "${RED}Failed:        ${FAILURE_COUNT}${NC}"
    echo -e "Success rate:  ${success_rate}%"
    echo -e "${GREEN}Throughput:    ${rps} RPS${NC}"
}

# Monitor Kafka processing
monitor_kafka() {
    echo -e "\n${BLUE}=== Kafka Processing Status ===${NC}"

    local initial_count=$(kafka_count)
    echo -e "Messages in topic: ${initial_count}"

    # Wait for processing
    echo -e "\n${YELLOW}Waiting for Kafka consumer to process messages...${NC}"
    local max_wait=120
    local waited=0

    while [[ $waited -lt $max_wait ]]; do
        local lag=$(docker exec kafka kafka-consumer-groups --bootstrap-server kafka:29092 \
            --describe --group jackpot-service 2>/dev/null \
            | grep -v "^TOPIC\|^Consumer\|^GROUP\|^$" \
            | awk '{sum += $6} END {print sum}')

        if [[ "${lag:-1}" == "0" ]]; then
            echo -e "\n${GREEN}All messages processed (lag = 0)${NC}"
            break
        fi

        printf "\r  Current lag: %-6s (waited %ds / %ds max)" "${lag:-?}" "$waited" "$max_wait"
        sleep 2
        waited=$((waited + 2))
    done

    echo ""

    # Final Kafka stats
    kafka_lag
}

# Check database for processed bets
check_processed() {
    echo -e "\n${BLUE}=== Processing Summary ===${NC}"

    # Check DLQ
    local dlq_topic="${TOPIC}-dlq"
    local dlq_exists=$(docker exec kafka kafka-topics --bootstrap-server kafka:29092 --list 2>/dev/null | grep "^${dlq_topic}$")

    if [[ -n "$dlq_exists" ]]; then
        local dlq_count=$(docker exec kafka kafka-run-class kafka.tools.GetOffsetShell \
            --broker-list kafka:29092 --topic "$dlq_topic" 2>/dev/null \
            | awk -F: '{sum += $3} END {print sum}')

        if [[ "${dlq_count:-0}" -gt 0 ]]; then
            echo -e "${RED}Dead Letter Queue messages: ${dlq_count}${NC}"
        else
            echo -e "${GREEN}Dead Letter Queue: 0 (no failed messages)${NC}"
        fi
    else
        echo -e "${GREEN}No DLQ topic created (no failures)${NC}"
    fi

    # Show jackpot pool values
    echo -e "\n${BLUE}=== Jackpot Pool Values ===${NC}"
    local jackpots=$(curl -sf "${BASE_URL}/api/v1/jackpots" 2>/dev/null)
    if [[ -n "$jackpots" ]]; then
        echo "$jackpots" | jq -r '.[] | "  \(.name): $\(.currentPoolValue)"' 2>/dev/null || echo "Unable to parse jackpots"
    fi
}

# Main execution
main() {
    check_prerequisites
    check_services

    setup_jackpots
    export_jackpots

    kafka_info

    run_load_test
    print_results

    monitor_kafka
    check_processed

    echo -e "\n${BLUE}================================================${NC}"
    echo -e "${GREEN}   Load Test Complete${NC}"
    echo -e "${BLUE}================================================${NC}"
}

main
