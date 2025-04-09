# Build-time argument for .NET SDK. Default is 8.0.
ARG SDK_VERSION=8.0

# Base image of .NET SDK version
FROM mcr.microsoft.com/dotnet/sdk:${SDK_VERSION}

# Install xmlstarlet and other utilities
RUN apt-get update && \
    apt-get install -y wget unzip xmlstarlet && \
    apt-get install -y zip curl tar gzip iputils-ping && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install Python 3.8
RUN apt-get update && \
    apt-get install -y python3.8 python3-pip && \
    update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.8 1 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV GCLOUD_VERSION=456.0.0
ENV GCLOUD_FILE=google-cloud-cli-${GCLOUD_VERSION}-linux-x86_64.tar.gz

# Download and extract gcloud CLI
RUN curl -O https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/${GCLOUD_FILE}
RUN tar -xzvf ${GCLOUD_FILE} && \
    google-cloud-sdk/install.sh

# Add to PATH
ENV PATH="/google-cloud-sdk/bin:${PATH}"

# Application setup
WORKDIR /DynamicAnalysis
COPY *.csproj .
COPY unzip_files ./src

COPY zip-and-publish.sh .
RUN chmod +x ./zip-and-publish.sh

COPY gradiator-x-454207-6e09134229e4.json .
RUN chmod +r ./gradiator-x-454207-6e09134229e4.json

ENV GOOGLE_APPLICATION_CREDENTIALS=./auth.json

# Build and runtime config
RUN dotnet restore

# Authenticate with service account
RUN gcloud auth activate-service-account --key-file=./gradiator-x-454207-6e09134229e4.json && \
    gcloud config set project gradiator-x-454207

CMD ["/bin/sh", "-c", \
  "dotnet clean && dotnet test && \
  ./zip-and-publish.sh"]