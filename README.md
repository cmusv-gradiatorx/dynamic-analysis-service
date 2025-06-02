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
├── docker/                             # Docker configuration and scripts
│   ├── build.gradle                    # Gradle configuration for container builds
│   ├── Dockerfile                      # Docker container configuration
│   ├── settings.gradle                 # Gradle project settings for containers
│   ├── zip_and_publish.py              # Python script for processing submissions
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
│   │   │       │   ├── SubmissionsController.java  # Handles submission endpoints
│   │   │       │   └── DockerController.java       # Docker container management
│   │   │       │
│   │   │       ├── models/             # Data models and DTOs
│   │   │       │   ├── PubSubMessage.java          # Message structure for Pub/Sub
│   │   │       │   └── PubSubPayload.java          # Payload structure for submissions
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

## Configuration

The service can be configured via `src/main/resources/application.properties`:

```properties
# Dynamic Analysis Service Configuration
dynamic.analysis.docker.path=docker
dynamic.analysis.unzip.path=docker/unzip_files
dynamic.analysis.image.name=dynamic_test
```

These properties control:
- `docker.path`: Location of Docker configuration files
- `unzip.path`: Directory for extracting submission files
- `image.name`: Default Docker image name for dynamic analysis

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

- **SubmissionsController.java**: Handles HTTP requests for submission endpoints
- **DockerController.java**: Docker container management logic with improved logging

### Models

- **PubSubMessage.java**: Message structure for Pub/Sub communications
- **PubSubPayload.java**: Payload structure for submission data

### Utils

- **UnzipSubmission.java**: Handles decompression of submission files with configurable paths

### Docker Configuration

- **docker/build.gradle**: Gradle configuration script for container builds
- **docker/Dockerfile**: Docker container configuration for Java programs
- **docker/settings.gradle**: Gradle project settings for containers
- **docker/zip_and_publish.py**: Python script for processing submissions

**InitialApplication.java**: Spring Boot main application class

## Docker

### Building and running

#### Building
The docker-build bash script contains the docker build command with build time arguments. Excluding the build-arg 
argument will build the image with version defaults found in the Dockerfile.

JDK_VERSION controls the major version of Java development kit being used.
GRADLE_VERSION controls the version of Gradle being used.

   ```
   docker build --build-arg JDK_VERSION=17 --build-arg GRADLE_VERSION=7.3 -t image_name:latest ./docker
   ```

#### Running

Run the container with the following command
   ```
   docker run -it container_name:latest
   ```


#### Compatibility Matrix
See the compatibility matrix below to view the minimum Gradle version needed for the specified JDK.
(visit https://docs.gradle.org/current/userguide/compatibility.html for full compatibility matrix)
   ```
   +----------+------------------------+--------------------------+
   | Java     | Support for            | Support for running      |
   | version  | toolchains             | Gradle                   |
   +----------+------------------------+--------------------------+
   | 11       | N/A                    | 5.0                      |
   | 12       | N/A                    | 5.4                      |
   | 13       | N/A                    | 6.0                      |
   | 14       | N/A                    | 6.3                      |
   | 15       | 6.7                    | 6.7                      |
   | 16       | 7.0                    | 7.0                      |
   | 17       | 7.3                    | 7.3                      |
   | 18       | 7.5                    | 7.5                      |
   | 19       | 7.6                    | 7.6                      |
   | 20       | 8.1                    | 8.3                      |
   | 21       | 8.4                    | 8.5                      |
   | 22       | 8.7                    | 8.8                      |
   | 23       | 8.10                   | 8.10                     |
   | 24       | N/A                    | N/A                      |
   +----------+------------------------+--------------------------+
   ```
### Useful commands
Search for an image by name
   ```
   docker search --filter is-official=true <image_name>
   ```

Pull and run image
   ```
   docker run -p 8080:80 --rm <image_name>
   ```

Build and push an image
   ```
   docker build -t <YOUR-USERNAME>/<image_name> ./docker
   ```

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
