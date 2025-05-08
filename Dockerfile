FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy the jar file into the container
COPY Vyrtuous.jar /app/

# Copy the entry script
COPY entry.sh /app/

# Install bash and wget
RUN apt-get update && apt-get install -y wget curl bash

# Download and set executable permission for yq
RUN wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/local/bin/yq && \
    chmod +x /usr/local/bin/yq

# Make your entry script executable
RUN chmod +x /app/entry.sh

# Set the entrypoint
ENTRYPOINT ["/bin/bash", "/app/entry.sh"]
