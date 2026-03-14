# PR Review Tool

AI-powered GitHub pull request reviewer built with Spring Boot, WebFlux, and a locally running Ollama model.

The application receives GitHub `pull_request` webhooks, validates the webhook signature, fetches the PR diff, sends the diff to `deepseek-coder:6.7b` running in Ollama, and posts a markdown review comment back to the pull request.

Repository: [pranj2410/pr-review-tool](https://github.com/pranj2410/pr-review-tool)

## Demo

- Live example PR review comment: [Flyerr PR #1 comment](https://github.com/pranj2410/Flyerr/pull/1#issuecomment-4060448025)

## Features

- Receives GitHub webhooks at `POST /webhook`
- Validates `X-Hub-Signature-256` using HMAC-SHA256
- Processes pull request actions asynchronously
- Supports `opened`, `synchronize`, `edited`, and `reopened` events
- Fetches PR diffs from GitHub and truncates large diffs for the model context window
- Falls back to direct PR diff retrieval if the webhook diff URL is empty
- Uses a local Ollama model instead of a hosted LLM API
- Posts formatted markdown review comments directly on GitHub PRs
- Posts a fallback warning comment if review generation fails

## Tech Stack

- Java 17
- Spring Boot 3
- Spring WebFlux
- Lombok
- Jackson
- Gradle (Groovy DSL)
- Ollama
- GitHub REST API v3
- ngrok

## Architecture

```text
GitHub PR Event
      |
      v
GitHub Webhook
      |
      v
WebhookController
  - validates signature
  - filters event type
      |
      v
ReviewService (@Async)
  - filters PR action
  - fetches diff
  - calls Ollama
  - posts GitHub comment
      |
      +--> GitHubService --> GitHub REST API
      |
      +--> OllamaService --> Local Ollama model
```

## Project Structure

```text
src/main/java/com/codebot/
├── CodeReviewBotApplication.java
├── config/
│   ├── AppConfig.java
│   ├── AsyncConfig.java
│   ├── GitHubProperties.java
│   └── OllamaProperties.java
├── controller/
│   └── WebhookController.java
├── model/
│   ├── GitHubCommentRequest.java
│   ├── OllamaGenerateRequest.java
│   ├── OllamaGenerateResponse.java
│   └── PullRequestEvent.java
└── service/
    ├── GitHubService.java
    ├── OllamaService.java
    └── ReviewService.java
```

## Configuration

Configuration is stored in `src/main/resources/application.yml`.

The application is set up to read credentials from environment variables so secrets do not need to be committed:

```yaml
github:
  token: ${GITHUB_TOKEN:your_github_personal_access_token}
  webhook-secret: ${GITHUB_WEBHOOK_SECRET:your_webhook_secret}

ollama:
  base-url: http://localhost:11434
  model: deepseek-coder:6.7b

server:
  port: 8080
```

Set these before running:

```bash
export GITHUB_TOKEN=your_github_personal_access_token
export GITHUB_WEBHOOK_SECRET=your_webhook_secret
```

## Prerequisites

### 1. Java 17

```bash
java -version
```

Install Java 17 if needed from [Adoptium](https://adoptium.net).

### 2. Ollama

Install Ollama:

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

Pull the model:

```bash
ollama pull deepseek-coder:6.7b
```

Start the server:

```bash
ollama serve
```

### 3. ngrok

Start a tunnel to the local app:

```bash
ngrok http 8080
```

### 4. GitHub Personal Access Token

Use a fine-grained token with repository access for the repos you want to review.

Recommended permissions:

- `Pull requests`: `Read and write`
- `Issues`: `Read and write`
- `Contents`: `Read-only`
- `Webhooks`: `Read and write` if you want to create webhooks through the API

### 5. GitHub Webhook

For each repo you want the bot to review:

1. Go to `Settings -> Webhooks -> Add webhook`
2. Set `Payload URL` to `https://your-ngrok-domain/webhook`
3. Set `Content type` to `application/json`
4. Set `Secret` to the same value as `GITHUB_WEBHOOK_SECRET`
5. Select only the `Pull requests` event

## Running Locally

Start services in this order:

1. `ollama serve`
2. `ngrok http 8080`
3. `./gradlew bootRun`

Run the application:

```bash
./gradlew bootRun
```

Build the project:

```bash
./gradlew build
```

The built JAR will be available in `build/libs/`.

## Review Flow

1. GitHub sends a `pull_request` event to `/webhook`
2. The app validates the HMAC signature
3. The request is queued asynchronously
4. The bot fetches the PR diff from GitHub
5. Large diffs are truncated to fit the local model context
6. The diff is sent to Ollama using `deepseek-coder:6.7b`
7. The generated markdown review is posted to the pull request as a comment

If Ollama fails or times out, the bot posts:

```text
⚠️ Code review bot encountered an error. Please review manually.
```

## Notable Implementation Details

- Uses `@Async` with a dedicated thread pool for review jobs
- Uses `WebClient` for GitHub and Ollama integration
- Returns immediately from the webhook endpoint so GitHub does not time out
- Supports both webhook-provided diff URLs and direct PR diff fallback retrieval
- Keeps setup lightweight by using a local Ollama server instead of a hosted inference API

## Future Improvements

- Move from PAT-based auth to a GitHub App for multi-repo support
- Add unit and integration tests around webhook validation and review orchestration
- Add structured prompt templates and comment deduplication
- Add Docker support for easier local deployment

## Resume Summary

- Built a self-hosted AI pull request review bot with Java 17, Spring Boot, WebFlux, and Ollama that analyzes GitHub PR diffs and posts automated review comments.
- Implemented secure GitHub webhook validation, asynchronous review processing, diff retrieval fallbacks, and resilient error handling.
- Integrated GitHub REST APIs, ngrok, and a local LLM pipeline to deliver real-time PR feedback without relying on cloud-hosted model inference.
