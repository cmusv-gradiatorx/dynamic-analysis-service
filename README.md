# Dynamic Analysis Service

This service takes a program under test (PUT) as input and performs dynamic code analysis via unit tests and code coverage tools. The service handles concurrent submissions by isolating each submission in its own Docker container with unique file naming. Results are zipped and published to PubSub topics using native Java integrations.

## Submission Service
Provides an API for assignment submission with full concurrency support. After receiving a submission, it saves the ZIP file with a unique submission ID, builds a Docker image, and runs the container with the mounted ZIP file. Each submission is processed in isolation to prevent conflicts. Results are published to the pub sub topic from the main application using Google Cloud Java SDK.

| Method | Path              | Description                                                       |
|--------|-------------------|-------------------------------------------------------------------|
| POST   | /submissions      | Create a submission and run unit tests inside an isolated Docker container. |

## Concurrent Processing

The service is designed to handle multiple submissions simultaneously:
- **Unique File Naming**: Each submission ZIP is saved with its submission ID as the filename
- **Isolated Containers**: Each submission runs in its own Docker container instance  
- **No Shared State**: Containers don't share file systems or temporary directories
- **Volume Mounting**: ZIP files are mounted read-only into containers for processing

## Folder Structure
```
dynamic-analysis-service
│
├── docker/                             # Docker configuration and scripts
│   ├── build.gradle                    # Gradle configuration for container builds
│   ├── Dockerfile                      # Docker container configuration with extraction logic
│   └── settings.gradle                 # Gradle project settings for containers
│
├── submissions/                        # Individual submission ZIP files (created at runtime)
│   ├── submission1.zip                 # ZIP file for submission ID "submission1"
│   └── submission2.zip                 # ZIP file for submission ID "submission2"
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
│   │   │       │   └── UnzipSubmission.java        # Handles ZIP file saving with unique names
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
dynamic.analysis.submissions.path=submissions
dynamic.analysis.image.name=dynamic_test
dynamic.analysis.reports.path=build/reports

# Google Cloud Configuration
google.cloud.project.id=gradiatorx
dynamic.analysis.pubsub.topic=dynamic-analysis-result
```

These properties control:
- `docker.path`: Location of Docker configuration files
- `submissions.path`: Directory for storing individual submission ZIP files
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

## Processing Flow

### Concurrent Submission Handling

1. **Receive Submission**: REST endpoint receives POST with PubSub payload
2. **Save ZIP File**: ZIP data is saved as `{submissionId}.zip` in submissions directory
3. **Build Docker Image**: Shared image is built once (cached for subsequent submissions)
4. **Mount & Run**: ZIP file is mounted into container at `/workspace/{submissionId}.zip`
5. **Container Processing**:
   - Creates isolated directory `/workspace/submission_{submissionId}`
   - Extracts ZIP file within container
   - Runs `gradle clean test` on extracted code
   - Generates test reports
6. **Result Collection**: Container output is captured and published to PubSub
7. **Cleanup**: ZIP file and temporary data are cleaned up

### API Specification

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

- **SubmissionService.java**: Main business logic for processing submissions with concurrency support
- **DockerService.java**: Java Docker client for building images and running isolated containers
- **PubSubService.java**: Google Cloud PubSub integration for publishing results

### Models

- **PubSubMessage.java**: Message structure for Pub/Sub communications
- **PubSubPayload.java**: Payload structure for submission data

### Utils

- **UnzipSubmission.java**: Handles saving ZIP files with unique submission ID naming

### Docker Configuration

- **docker/build.gradle**: Gradle configuration script for container builds
- **docker/Dockerfile**: Container with built-in ZIP extraction and processing logic
- **docker/settings.gradle**: Gradle project settings for containers

**InitialApplication.java**: Spring Boot main application class

## Technology Stack

### Core Technologies
- **Java 21**: Main programming language
- **Spring Boot 3.4.4**: Application framework
- **Gradle 8.10+**: Build system

### Integration Libraries
- **Docker Java Client 3.3.6**: Native Java Docker integration with volume mounting
- **Google Cloud PubSub 1.128.1**: Native Java PubSub client
- **Apache Commons Compress 1.26.0**: ZIP file operations

### Key Improvements
- ✅ **Concurrent Processing**: Multiple submissions handled simultaneously without conflicts
- ✅ **Isolated Containers**: Each submission runs in its own container environment
- ✅ **Unique File Naming**: Submission IDs ensure no file name collisions
- ✅ **Native Java Docker Client**: Replaces shell command execution
- ✅ **Native Google Cloud SDK**: Replaces Python script dependencies
- ✅ **Volume Mounting**: Efficient file sharing between host and containers
- ✅ **Automatic Cleanup**: Temporary files and containers are cleaned up after processing

## Docker

### Building and running

#### Building
Build the Docker image from the docker directory:

   ```
   docker build --build-arg JDK_VERSION=21 --build-arg GRADLE_VERSION=8.14.1 -t dynamic_test:latest ./docker
   ```

#### Running

The container now accepts mounted ZIP files and processes them internally:
   ```
   docker run -v /path/to/submission.zip:/workspace/submission.zip -e SUBMISSION_ID=test123 -e ZIP_FILE_NAME=submission.zip dynamic_test:latest
   ```

#### Container Processing

The container automatically:
1. Validates the mounted ZIP file exists
2. Creates an isolated workspace directory
3. Extracts the ZIP file contents
4. Copies build configuration templates
5. Runs `gradle clean test`
6. Reports results to stdout

## Scaling and Performance

- **Horizontal Scaling**: Multiple service instances can run simultaneously
- **Container Isolation**: No shared state between concurrent submissions
- **Resource Management**: Each container has isolated CPU/memory limits
- **Cleanup Automation**: Automatic cleanup prevents resource leaks

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
