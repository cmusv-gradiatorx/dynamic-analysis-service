# Dynamic Analysis Service

This service takes a program under test (PUT) as input and performs dynamic code analysis via unit tests and code coverage tools. If successful, the results are zipped and published to its specified PubSub topic using native Java integrations.

## Submission Service
Provides an API for assignment submission. After receiving the submission it attempts to build a Docker image and run the container using Java Docker client. The results are zipped and published to the pub sub topic from the main application using Google Cloud Java SDK.

| Method | Path              | Description                                                       |
|--------|-------------------|-------------------------------------------------------------------|
| POST   | /submissions      | Create a submission and run unit tests inside a Docker container. |

## Folder Structure
```
dynamic-analysis-service
│
├── docker/                             # Docker configuration and scripts
│   ├── build.gradle                    # Gradle configuration for container builds
│   ├── Dockerfile                      # Docker container configuration
│   ├── settings.gradle                 # Gradle project settings for containers
│   └── unzip_files/                    # Directory for extracted submission files
│
├── src/                                # Source code directory
│   ├── main/                           # Main application source code
│   │   ├── java/                       # Java source files
│   │   │   └── edu/cmu/gradiatorx/dynamic/
│   │   │       │
│   │   │       ├── config/             # Configuration classes
│   │   │       │   └── ServiceConfig.java    # Centralized service configuration
│   │   │       │
│   │   │       ├── controller/         # REST API Controllers
│   │   │       │   └── SubmissionsController.java  # Handles submission endpoints
│   │   │       │
│   │   │       ├── models/             # Data models and DTOs
│   │   │       │   ├── PubSubMessage.java          # Message structure for Pub/Sub
│   │   │       │   └── PubSubPayload.java          # Payload structure for submissions
│   │   │       │
│   │   │       ├── service/            # Business logic services
│   │   │       │   ├── SubmissionService.java      # Main submission processing logic
│   │   │       │   ├── DockerService.java          # Java Docker client operations
│   │   │       │   └── PubSubService.java          # Google Cloud PubSub operations
│   │   │       │
│   │   │       ├── utils/              # Utility classes and helper methods
│   │   │       │   └── UnzipSubmission.java        # Handles file decompression
│   │   │       │
│   │   │       └── InitialApplication.java         # Spring Boot main application class
│   │   │
│   │   └── resources/                  # Application resources
│   │       └── application.properties  # Application configuration
│   │
│   └── test/                           # Test source code
│       └── java/                       # Test classes
│
├── build.gradle                        # Main Gradle build configuration
├── settings.gradle                     # Main Gradle project settings
└── README.md                           # Project documentation

```

## Requirements
- Java 21 (or later)
- Gradle 8.10 (or later)
- Docker Desktop
- Google Cloud credentials (credentials.json or environment variable)

## Installation

1. Download Docker Desktop
   ```
   https://www.docker.com/products/docker-desktop/
   ```

2. Clone the repository:
    ```
    git clone https://github.com/cmusv-gradiatorx/dynamic-analysis-service.git
    cd dynamic-analysis-service
    ```

3. Set up Google Cloud credentials:
   - Place `credentials.json` in the project root, OR
   - Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable

## Configuration

The service can be configured via `src/main/resources/application.properties`:

```properties
# Dynamic Analysis Service Configuration
dynamic.analysis.docker.path=docker
dynamic.analysis.unzip.path=docker/unzip_files
dynamic.analysis.image.name=dynamic_test
dynamic.analysis.reports.path=build/reports

# Google Cloud Configuration
google.cloud.project.id=gradiatorx
dynamic.analysis.pubsub.topic=dynamic-analysis-result
```

These properties control:
- `docker.path`: Location of Docker configuration files
- `unzip.path`: Directory for extracting submission files
- `image.name`: Default Docker image name for dynamic analysis
- `reports.path`: Relative path to test reports within submission
- `project.id`: Google Cloud project ID
- `pubsub.topic`: PubSub topic for publishing results

## Running the project

1. Open Docker Desktop
   ```
   This starts the Docker engine and provides management tools for your images and containers
   ```

2. Run the project:
   ```
   Navigate to InitialApplication.java
   Run 'InitialApplication main()'
   ```

## API Specification

Comprehensive API specification is available via Swagger UI, which allows you to explore all available endpoints interactively.

### Accessing Swagger UI

1. Run Spring Boot application:
   ```
   Run 'InitialApplication main()'
   ```

2. Open your browser and navigate to the link below:
   ```
   http://localhost:8080/swagger-ui/index.html
   ```

## Project Structure Details

### Configuration

- **ServiceConfig.java**: Centralized configuration management for paths and service settings

### Controllers

- **SubmissionsController.java**: Handles HTTP requests for submission endpoints with proper REST responses

### Services

- **SubmissionService.java**: Main business logic for processing submissions
- **DockerService.java**: Java Docker client for building images and running containers (replaces shell commands)
- **PubSubService.java**: Google Cloud PubSub integration for publishing results (replaces Python script)

### Models

- **PubSubMessage.java**: Message structure for Pub/Sub communications
- **PubSubPayload.java**: Payload structure for submission data

### Utils

- **UnzipSubmission.java**: Handles decompression of submission files with configurable paths

### Docker Configuration

- **docker/build.gradle**: Gradle configuration script for container builds
- **docker/Dockerfile**: Simplified Docker container configuration (no Python dependencies)
- **docker/settings.gradle**: Gradle project settings for containers

**InitialApplication.java**: Spring Boot main application class

## Technology Stack

### Core Technologies
- **Java 21**: Main programming language
- **Spring Boot 3.4.4**: Application framework
- **Gradle 8.10+**: Build system

### Integration Libraries
- **Docker Java Client 3.3.6**: Native Java Docker integration
- **Google Cloud PubSub 1.128.1**: Native Java PubSub client
- **Apache Commons Compress 1.26.0**: ZIP file operations

### Key Improvements
- ✅ **Native Java Docker Client**: Replaces shell command execution
- ✅ **Native Google Cloud SDK**: Replaces Python script dependencies
- ✅ **Improved Error Handling**: Better exception management and logging
- ✅ **Type Safety**: Full Java type checking instead of shell scripts
- ✅ **Better Resource Management**: Proper connection and resource cleanup

## Docker

### Building and running

#### Building
Build the Docker image from the docker directory:

   ```
   docker build --build-arg JDK_VERSION=21 --build-arg GRADLE_VERSION=8.10 -t dynamic_test:latest ./docker
   ```

#### Running

Run the container with the following command:
   ```
   docker run -it dynamic_test:latest
   ```

Note: The container now only runs `gradle clean test`. Result publishing is handled by the main Spring Boot application using Java.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
