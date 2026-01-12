#!/bin/bash
set -e

# Configuration
KAFKA_CONTAINER="kafka"
BOOTSTRAP="kafka:29092"
TOPIC="jackpot-bets"
CONSUMER_GROUP="jackpot-service"
REFRESH_INTERVAL=${1:-2}

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

clear_screen() {
    printf "\033[2J\033[H"
}

print_header() {
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC}            ${CYAN}Kafka Monitor - Jackpot Service${NC}                    ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}            $(date '+%Y-%m-%d %H:%M:%S')                           ${BLUE}║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${NC}"
}

# Get topic details
get_topic_info() {
    echo -e "\n${YELLOW}▸ Topic Configuration${NC}"
    docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $BOOTSTRAP \
        --describe --topic $TOPIC 2>/dev/null | head -5
}

# Get partition offsets (current message count)
get_partition_offsets() {
    echo -e "\n${YELLOW}▸ Partition Offsets${NC}"
    echo -e "Partition | Start | End | Messages"
    echo -e "----------|-------|-----|----------"

    local total=0
    while IFS=: read -r topic partition offset; do
        if [[ -n "$partition" ]]; then
            local start_offset=$(docker exec $KAFKA_CONTAINER kafka-run-class kafka.tools.GetOffsetShell \
                --broker-list $BOOTSTRAP --topic $TOPIC --partitions "$partition" --time -2 2>/dev/null \
                | cut -d: -f3)
            local messages=$((offset - start_offset))
            total=$((total + messages))
            printf "    %-5s |  %-4s | %-4s | %s\n" "$partition" "${start_offset:-0}" "$offset" "$messages"
        fi
    done < <(docker exec $KAFKA_CONTAINER kafka-run-class kafka.tools.GetOffsetShell \
        --broker-list $BOOTSTRAP --topic $TOPIC 2>/dev/null)

    echo -e "----------|-------|-----|----------"
    echo -e "${GREEN}Total messages in topic: ${total}${NC}"
}

# Get consumer group status
get_consumer_group() {
    echo -e "\n${YELLOW}▸ Consumer Group: ${CONSUMER_GROUP}${NC}"

    local output=$(docker exec $KAFKA_CONTAINER kafka-consumer-groups --bootstrap-server $BOOTSTRAP \
        --describe --group $CONSUMER_GROUP 2>&1)

    if echo "$output" | grep -q "Consumer group .* does not exist"; then
        echo -e "${RED}Consumer group not found (app may not be running)${NC}"
        return
    fi

    # Parse and display consumer group info
    echo -e "Partition | Current | End     | Lag    | Consumer"
    echo -e "----------|---------|---------|--------|------------------"

    local total_lag=0
    local processed=0

    while IFS= read -r line; do
        if [[ "$line" =~ ^$TOPIC ]]; then
            local partition=$(echo "$line" | awk '{print $2}')
            local current=$(echo "$line" | awk '{print $3}')
            local end=$(echo "$line" | awk '{print $4}')
            local lag=$(echo "$line" | awk '{print $6}')
            local consumer=$(echo "$line" | awk '{print $7}')

            if [[ "$current" != "-" ]]; then
                processed=$((processed + current))
            fi

            if [[ "$lag" =~ ^[0-9]+$ ]]; then
                total_lag=$((total_lag + lag))
            fi

            printf "    %-5s | %-7s | %-7s | %-6s | %s\n" \
                "$partition" "${current:--}" "${end:--}" "${lag:--}" "${consumer:--}"
        fi
    done <<< "$output"

    echo -e "----------|---------|---------|--------|------------------"

    if [[ $total_lag -eq 0 ]]; then
        echo -e "${GREEN}Total lag: 0 (all caught up!)${NC}"
    else
        echo -e "${YELLOW}Total lag: ${total_lag}${NC}"
    fi

    echo -e "Messages processed: ${processed}"
}

# Get consumer group members
get_consumers() {
    echo -e "\n${YELLOW}▸ Active Consumers${NC}"

    local members=$(docker exec $KAFKA_CONTAINER kafka-consumer-groups --bootstrap-server $BOOTSTRAP \
        --describe --group $CONSUMER_GROUP --members 2>/dev/null)

    if echo "$members" | grep -q "GROUP"; then
        echo "$members" | head -15
    else
        echo -e "${RED}No active consumers${NC}"
    fi
}

# Calculate throughput
PREV_OFFSET=0
PREV_TIME=$(date +%s)

get_throughput() {
    echo -e "\n${YELLOW}▸ Throughput${NC}"

    local current_offset=$(docker exec $KAFKA_CONTAINER kafka-run-class kafka.tools.GetOffsetShell \
        --broker-list $BOOTSTRAP --topic $TOPIC 2>/dev/null \
        | awk -F: '{sum += $3} END {print sum}')

    local current_time=$(date +%s)
    local time_diff=$((current_time - PREV_TIME))

    if [[ $time_diff -gt 0 && $PREV_OFFSET -gt 0 ]]; then
        local offset_diff=$((current_offset - PREV_OFFSET))
        local rps=$((offset_diff / time_diff))
        echo -e "Ingestion rate: ${GREEN}~${rps} msg/sec${NC}"
    else
        echo -e "Ingestion rate: calculating..."
    fi

    PREV_OFFSET=$current_offset
    PREV_TIME=$current_time
}

# Check broker health
check_broker_health() {
    echo -e "\n${YELLOW}▸ Broker Health${NC}"

    if docker exec $KAFKA_CONTAINER kafka-broker-api-versions --bootstrap-server $BOOTSTRAP >/dev/null 2>&1; then
        echo -e "${GREEN}Broker: healthy${NC}"
    else
        echo -e "${RED}Broker: unhealthy${NC}"
    fi

    # Check under-replicated partitions
    local under_replicated=$(docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $BOOTSTRAP \
        --describe --under-replicated-partitions 2>/dev/null | wc -l)

    if [[ $under_replicated -eq 0 ]]; then
        echo -e "${GREEN}Under-replicated partitions: 0${NC}"
    else
        echo -e "${RED}Under-replicated partitions: ${under_replicated}${NC}"
    fi
}

# DLQ (Dead Letter Queue) monitoring
check_dlq() {
    echo -e "\n${YELLOW}▸ Dead Letter Queue${NC}"

    local dlq_topic="${TOPIC}-dlq"
    local dlq_exists=$(docker exec $KAFKA_CONTAINER kafka-topics --bootstrap-server $BOOTSTRAP \
        --list 2>/dev/null | grep "^${dlq_topic}$")

    if [[ -n "$dlq_exists" ]]; then
        local dlq_count=$(docker exec $KAFKA_CONTAINER kafka-run-class kafka.tools.GetOffsetShell \
            --broker-list $BOOTSTRAP --topic "$dlq_topic" 2>/dev/null \
            | awk -F: '{sum += $3} END {print sum}')

        if [[ "${dlq_count:-0}" -gt 0 ]]; then
            echo -e "${RED}DLQ messages: ${dlq_count} (check for processing errors!)${NC}"
        else
            echo -e "${GREEN}DLQ messages: 0${NC}"
        fi
    else
        echo -e "${GREEN}No DLQ topic (no failed messages)${NC}"
    fi
}

# Main monitoring loop
monitor() {
    echo -e "${CYAN}Starting Kafka monitor (refresh: ${REFRESH_INTERVAL}s)${NC}"
    echo -e "${CYAN}Press Ctrl+C to exit${NC}"
    sleep 2

    while true; do
        clear_screen
        print_header

        get_topic_info
        get_consumer_group
        get_consumers
        get_throughput
        check_dlq
        check_broker_health

        echo -e "\n${BLUE}─────────────────────────────────────────────────────────────────${NC}"
        echo -e "Refreshing in ${REFRESH_INTERVAL}s... (Ctrl+C to exit)"

        sleep $REFRESH_INTERVAL
    done
}

# One-shot mode
oneshot() {
    print_header
    get_topic_info
    get_partition_offsets
    get_consumer_group
    get_consumers
    check_dlq
    check_broker_health
}

# Main
case "${1:-}" in
    --once|-o)
        oneshot
        ;;
    *)
        trap "echo -e '\n${GREEN}Monitor stopped${NC}'; exit 0" INT
        monitor
        ;;
esac
