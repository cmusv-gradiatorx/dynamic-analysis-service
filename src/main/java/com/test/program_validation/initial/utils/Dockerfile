# Build-time argument for JDK. Default is 21
ARG JDK_VERSION=21

# Base image of JDK_VERSION
FROM amazoncorretto:${JDK_VERSION}

# Build-time argument for Gradle version. Default is 8.10
ARG GRADLE_VERSION=8.10

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

# Enable Python 3.8 from amazon-linux-extras and install
RUN amazon-linux-extras enable python3.8 && \
    yum clean metadata && \
    yum install -y python3.8 && \
    alternatives --install /usr/bin/python3 python3 /usr/bin/python3.8 1 && \
    alternatives --install /usr/bin/pip3 pip3 /usr/bin/pip3.8 1 && \
    yum clean all

# Set Gcloud environment variables
ENV GCLOUD_VERSION=456.0.0
ENV GCLOUD_FILE=google-cloud-cli-${GCLOUD_VERSION}-linux-x86_64.tar.gz

# Download and extract gcloud CLI
RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/${GCLOUD_FILE}
RUN tar -xzvf ${GCLOUD_FILE} && \
    google-cloud-sdk/install.sh

# Add Gcloud to PATH
ENV PATH="/google-cloud-sdk/bin:${PATH}"

# Gradle nvironment setup
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

# Application setup
WORKDIR /DynamicAnalysis
COPY build.gradle .
COPY settings.gradle .
COPY unzip_files ./src

COPY zip_and_publish.py .
RUN chmod +x ./zip_and_publish.py

COPY credentials.json .
RUN chmod +r ./credentials.json

ENV GOOGLE_APPLICATION_CREDENTIALS=./credentials.json

# Build and runtime config
RUN gradle build

# Authenticate with service account
RUN gcloud auth activate-service-account --key-file=./credentials.json && \
    gcloud config set project gradiatorx

RUN pip3 install google-cloud-pubsub
RUN pip3 install urllib3==1.26.16

# Run gradle clean test, zip, and publish results to PubSub topic
CMD ["/bin/sh", "-c", \
  "gradle clean test && \
  python3 ./zip_and_publish.py"]
