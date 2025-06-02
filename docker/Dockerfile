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

# Application setup
WORKDIR /DynamicAnalysis
COPY build.gradle .
COPY settings.gradle .
COPY unzip_files ./src

# Build the project
RUN gradle build

# Run only gradle clean test - publishing is handled by the main application
CMD ["gradle", "clean", "test"]
