# Build-time argument for JDK. Default is 21.
ARG JDK_VERSION=21

# Base image of JDK_VERSION
FROM amazoncorretto:${JDK_VERSION}

# Build-time argument for Gradle version. Default is 8.10.
ARG GRADLE_VERSION=8.10

# Install xmlstarlet and gradle
RUN yum update -y && \
    amazon-linux-extras install epel -y && \
    yum install -y wget unzip shadow-utils xmlstarlet && \
    wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-${GRADLE_VERSION}-bin.zip && \
    rm /tmp/gradle-${GRADLE_VERSION}-bin.zip

# Environment setup
ENV GRADLE_HOME=/opt/gradle/gradle-${GRADLE_VERSION}
ENV PATH=$PATH:$GRADLE_HOME/bin

# Application setup
WORKDIR /DynamicAnalysis
COPY build.gradle .
COPY settings.gradle .
COPY unzip_files ./src

## Give execute permission to hello script
#COPY jdk_version.sh .
#RUN chmod +x ./jdk_version.sh
#
## Give execute permission to xml-to-csv
#COPY xml-to-csv.sh .
#RUN chmod +x ./xml-to-csv.sh
#
## Give execute permission to xml-to-csv-coverage
#COPY xml-to-csv-coverage.sh .
#RUN chmod +x ./xml-to-csv-coverage.sh

# Build and runtime config
RUN gradle build
#EXPOSE 8080

# Runtime commands
#CMD ["/bin/sh", "-c", \
#  "gradle clean test && \
#  ./xml-to-csv.sh ./build/test-results/test/TEST-com.gildedrose.GildedRoseTest.xml && \
#  ./xml-to-csv-coverage.sh ./build/reports/jacoco/test/jacocoTestReport.xml && \
#  echo '\n==================== printing directory inside container ====================' && \
#  ls -a"]


CMD ["/bin/sh", "-c", \
  "gradle clean test && \
  echo '\n==================== printing directory inside container ====================' && \
  ls -a"]