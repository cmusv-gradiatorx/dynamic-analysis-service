# Dynamic Analysis Service

This service take a program under test (PUT) as input and performs dynamic code analysis via unit tests and code coverage tools. If successful, the results are zipped and published to it's specified PubSub topic.

## Submission Service
Provides an API for assignment submission.

Provides an API to receive and process submissions. After receiving the submission it attempts to build a Docker image and run the container. The results are zipped and published to the pub sub topic from the container.

| Method | Path              | Description                                                       |
|--------|-------------------|-------------------------------------------------------------------|
| POST   | /submissions      | Create a submission and run unit tests inside a Docker container. |
