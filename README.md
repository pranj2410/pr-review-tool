# code-review-bot

A Spring Boot 3 application that receives GitHub pull request webhooks, sends the pull request diff to a locally running Ollama model, and posts the generated review back to GitHub as a pull request comment.

## Tech stack

- Java 17
- Spring Boot 3
- Spring WebFlux
- Lombok
- Jackson
- Gradle (Groovy DSL)
- Ollama running locally at `http://localhost:11434`
- GitHub REST API v3

## Project structure

```text
code-review-bot/
├── src/main/java/com/codebot/
│   ├── CodeReviewBotApplication.java
│   ├── config/
│   │   ├── AppConfig.java
│   │   ├── AsyncConfig.java
│   │   ├── GitHubProperties.java
│   │   └── OllamaProperties.java
│   ├── controller/
│   │   └── WebhookController.java
│   ├── model/
│   │   ├── GitHubCommentRequest.java
│   │   ├── OllamaGenerateRequest.java
│   │   ├── OllamaGenerateResponse.java
│   │   └── PullRequestEvent.java
│   └── service/
│       ├── GitHubService.java
│       ├── OllamaService.java
│       └── ReviewService.java
├── src/main/resources/
│   └── application.yml
├── build.gradle
├── settings.gradle
└── README.md
```

## Prerequisites

### 1. Java 17

Check your Java version:

```bash
java -version
```

It should show `17.x.x`. If Java 17 is not installed, download it from [Adoptium](https://adoptium.net).

### 2. Ollama

Install Ollama:

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

Pull the model:

```bash
ollama pull deepseek-coder:6.7b
```

Start the Ollama server:

```bash
ollama serve
```

Ollama must be running at `http://localhost:11434` before starting the Spring Boot application.

### 3. GitHub Personal Access Token

1. Go to GitHub -> Settings -> Developer Settings -> Personal Access Tokens -> Fine-grained tokens.
2. Create a token with repository read and write access for pull requests and issues.
3. Update `src/main/resources/application.yml` with your token:

```yaml
github:
  token: your_github_personal_access_token
```

Also set a webhook secret value:

```yaml
github:
  webhook-secret: your_webhook_secret
```

### 4. ngrok

Download ngrok from [ngrok.com/download](https://ngrok.com/download), then expose your local Spring Boot server:

```bash
ngrok http 8080
```

Copy the generated `https://xxxx.ngrok.io` URL for GitHub webhook configuration.

### 5. GitHub webhook

In your GitHub repository:

1. Go to `Settings -> Webhooks -> Add webhook`
2. Set `Payload URL` to `https://xxxx.ngrok.io/webhook`
3. Set `Content type` to `application/json`
4. Set `Secret` to the same value as `github.webhook-secret` in `application.yml`
5. Select the `Pull requests` event only

## Configuration

Default configuration lives in [`src/main/resources/application.yml`](/Users/ritika/Documents/New project 2/src/main/resources/application.yml):

```yaml
github:
  token: your_github_personal_access_token
  webhook-secret: your_webhook_secret

ollama:
  base-url: http://localhost:11434
  model: deepseek-coder:6.7b

server:
  port: 8080
```

## Run the application

Always start services in this order:

1. `ollama serve`
2. `ngrok http 8080`
3. `./gradlew bootRun`

Start the app:

```bash
./gradlew bootRun
```

## Build a JAR

Build the project with:

```bash
./gradlew build
```

The JAR will be created under `build/libs/`.

## How it works

1. GitHub sends a `pull_request` webhook to `/webhook`
2. The app validates `X-Hub-Signature-256` using HMAC-SHA256 and your configured webhook secret
3. Supported actions are `opened` and `synchronize`
4. The app fetches the pull request diff from GitHub
5. The diff is truncated to 8000 characters when needed
6. The diff is sent to the local Ollama model for review
7. The generated markdown review is posted back to the pull request as a GitHub issue comment

If Ollama fails or times out, the bot posts this fallback comment:

```text
⚠️ Code review bot encountered an error. Please review manually.
```

If the diff is empty, the bot skips the review and logs a warning.
