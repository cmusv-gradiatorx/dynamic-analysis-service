# Dynamic Analysis Service

This service take a program under test (PUT) as input and performs dynamic code analysis via unit tests and code coverage tools. If successful, the results are zipped and published to it's specified PubSub topic.

## Submission Service
Provides an API for assignment submission. After receiving the submission it attempts to build a Docker image and run the container. The results are zipped and published to the pub sub topic from the container.


| Method | Path              | Description                                                       |
|--------|-------------------|-------------------------------------------------------------------|
| POST   | /submissions      | Create a submission and run unit tests inside a Docker container. |

## Folder Structure
```
dynamic-analysis-service
│
├── src/  # Source code directory
│   └── main/                   # Main application source code
│       └── java/               # Java source files
│           └── com.test.program_validation.initial/
│               │
│               ├── controller/                           # REST API Controllers
│               │   └── SubmissionsController            # Handles HTTP requests for submission endpoints
│               │
│               ├── models/                               # Data models and DTOs
│               │   ├── PubSubMessage                    # Message structure for Pub/Sub communications
│               │   └── PubSubPayload                    # Payload structure for submission data
│               │
│               ├── utils/                                # Utility classes and helper methods
│               │   ├── build.gradle                     # Gradle configuration script
│               │   ├── DockerController                 # Docker container management logic
│               │   ├── Dockerfile                       # Docker container configuration
│               │   ├── settings.gradle                  # Gradle project settings
│               │   ├── UnzipSubmission                  # Handles decompression of submission files
│               │   └── zip_and_publish.py               # Python script for processing submissions
│               │
│               └── InitialApplication                    # Spring Boot main application class

```

## Requirements
- Java 21 (or later)
- Gradle 8.10 (or later)


## Installation

1. Clone the repository:
    ```
    git clone https://github.com/cmusv-gradiatorx/dynamic-analysis-service.git
    cd dynamic-analysis-service
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

### Controllers

- **SubmissionsController.java**: Handles HTTP requests for submission endpoints

### Models

- **PubSubMessage.java**: Message structure for Pub/Sub communications
- **PubSubPayload.java**: Payload structure for submission data

### Utils

- **utils/build.gradle**: Gradle configuration script
- **utils/DockerController.java**: Docker container management logic
- **utils/java.Dockerfile**: Dockerfile configuration for Java programs
- **utils/settings.gradle**: Gradle project settings
- **utils/UnzipSubmission.java**: Handles decompression of submission files
- **utils/zip_and_publish.py**: Python script for processing submissions

**InitialApplication.java**: Spring Boot main application class

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
