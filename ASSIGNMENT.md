# Jackpot - Backend Engineer Home Assignment

> **Sporty Group**

---

## Overview

The purpose of the assignment is to assess object oriented analysis and modelling skills, Java coding skills, and code structuring. Take your time on the task, but don't get too carried away. If you submit a solution that is in any way incomplete, the parts that you decided to focus on are relevant.

Keeping the objective in mind, you are free to use whatever tools, libraries, frameworks at your disposal. Artificial Intelligence (AI) usage is encouraged. Please include a `README` in any format on how to run and use the program.

---

## Requirements

Your task is to write a backend application that will receive a bet and process it for:
1. Jackpot pool contribution
2. Evaluate the bet for jackpot reward

You need to implement the following:

- An API endpoint to publish a bet to Kafka
- A Kafka consumer that listens to `jackpot-bets` Kafka named-topic
- Each bet must contribute to a matching jackpot pool
- Each bet must be evaluated for jackpot reward
- An API endpoint to evaluate if a bet wins a jackpot reward

The details of these Use Cases can be found below. There are no further defined requirements for the API, it is up to you to design and implement the necessary code in order for the API to support the mentioned operations above. This refers to the entire flow starting from the API, down to the persistence layer.

> **Note:** It is expected for you to spend around 90 minutes to complete the exercise.

---

## Delivery

- The solution needs to be **100% executable**
- Provide a link to the GitHub repository where your solution is committed
  - Please make sure it is not private or restricted
- Provide a `README` file (documentation) how to run and use the solution

---

## Jackpot Service

> We want to launch a new backend service that will manage jackpot contributions and rewards. Here you have the relevant Use Cases to support:

### 1. API Endpoint to Publish a Bet to Kafka

This endpoint should publish a bet to Kafka. A bet is represented by:

| Field | Description |
|-------|-------------|
| Bet ID | Unique identifier for the bet |
| User ID | Identifier of the user placing the bet |
| Jackpot ID | Identifier of the target jackpot |
| Bet Amount | The amount being bet |

The topic should be `jackpot-bets`.

### 2. Kafka Consumer

A Kafka consumer that listens to `jackpot-bets` Kafka named-topic.

### 3. Jackpot Contribution

Each bet must contribute to a matching jackpot.

#### Configuration Options

Each jackpot will start with a configurable initial pool value. Each Jackpot can have a different configuration for contribution. Initially support two options, with the ability to add more in the future:

| Type | Description |
|------|-------------|
| **Fixed** | Contribution as a percentage of the Bet Amount |
| **Variable** | Contribution as a percentage of the Bet Amount. In the beginning the contribution is bigger and over time it becomes lower at a fixed rate as the jackpot pool increases |

#### Matching Logic

The System checks for a matching jackpot based on Jackpot ID. If there is such a jackpot, the system should contribute to the jackpot pool according to its configuration.

#### Jackpot Contribution Schema

A Jackpot Contribution in database is composed by:

| Field | Description |
|-------|-------------|
| Bet ID | Reference to the original bet |
| User ID | User who made the contribution |
| Jackpot ID | Target jackpot |
| Stake Amount | Original bet amount |
| Contribution Amount | Calculated contribution |
| Current Jackpot Amount | Pool value after contribution |
| Created At Date | Timestamp of contribution |

### 4. API Endpoint to Evaluate Jackpot Reward

This endpoint should check if a contributing bet wins the jackpot reward and return the reward as a response.

#### Reward Configuration Options

Each Jackpot can have a different configuration for rewards. Initially support two options, with the ability to add more in the future:

| Type | Description |
|------|-------------|
| **Fixed** | Chance for a reward as a percentage |
| **Variable** | Chance as a percentage. In the beginning the chance for reward is smaller and over time it becomes bigger as the jackpot pool increases. If the jackpot pool hits a limit, the chance becomes 100% |

#### Reset Behavior

When a jackpot is rewarded, it should be reset to the initial pool value.

#### Jackpot Reward Schema

A Jackpot Reward in database is composed by:

| Field | Description |
|-------|-------------|
| Bet ID | Reference to the winning bet |
| User ID | User who won the reward |
| Jackpot ID | Jackpot that was won |
| Jackpot Reward Amount | Amount awarded |
| Created At Date | Timestamp of reward |

---

## Conditions

- If the Kafka setup is too complex, use mocks for the Kafka producer. Just log the payload.
- Use an in-memory database for the bets and jackpots.
