# Build-time argument for JDK. Default is 21
ARG JDK_VERSION=21

# Base image of JDK_VERSION
FROM amazoncorretto:${JDK_VERSION}

# Build-time argument for Gradle version
ARG GRADLE_VERSION=8.14.1

# Install Gradle and other tools
RUN yum update -y && \
    amazon-linux-extras install epel -y && \
    yum install -y wget unzip shadow-utils xmlstarlet && \
    wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip && \
    yum install -y zip && \
    yum install -y curl && \
    yum install -y tar unzip gzip iputils && \
    yum clean all && \
    rm /tmp/gradle-${GRADLE_VERSION}-bin.zip

# Gradle environment setup
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

# Create workspace directory for submissions
WORKDIR /workspace

# Copy build configuration templates
COPY build.gradle ./build.gradle.template
COPY settings.gradle ./settings.gradle.template

# Create entrypoint script for processing submissions
RUN cat > /workspace/process_submission.sh << 'EOF'
#!/bin/bash
set -e

echo "Starting submission processing..."
echo "Submission ID: $SUBMISSION_ID"
echo "Zip file: $ZIP_FILE_NAME"

# Check if zip file exists
if [ ! -f "/workspace/$ZIP_FILE_NAME" ]; then
    echo "ERROR: Zip file /workspace/$ZIP_FILE_NAME not found!"
    exit 1
fi

# Create submission-specific directory
mkdir -p "/workspace/submission_$SUBMISSION_ID"
cd "/workspace/submission_$SUBMISSION_ID"

# Extract the zip file
echo "Extracting zip file..."
unzip -q "/workspace/$ZIP_FILE_NAME" -d .

# Copy build configuration
cp ../build.gradle.template ./build.gradle
cp ../settings.gradle.template ./settings.gradle

# List extracted contents for debugging
echo "Extracted contents:"
find . -type f | head -20

# Run gradle build and test
echo "Running gradle clean test..."
gradle clean test

# Check if reports were generated
if [ -d "build/reports" ]; then
    echo "Test reports generated successfully"
    find build/reports -type f | head -10
else
    echo "WARNING: No test reports found in build/reports"
fi

echo "Processing completed successfully"
EOF

# Make the script executable
RUN chmod +x /workspace/process_submission.sh

# Set the entrypoint to process submissions
ENTRYPOINT ["/workspace/process_submission.sh"]
