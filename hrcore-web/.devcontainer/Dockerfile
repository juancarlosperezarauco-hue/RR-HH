FROM mcr.microsoft.com/devcontainers/javascript-node:24-bookworm

WORKDIR /hrcore-web

# Basic tools for frontend development and debugging.
RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    git \
    curl \
    ca-certificates \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/*

# Angular CLI global
#RUN npm install -g @angular/cli

# Keep the container alive for VS Code remote development.
CMD ["sleep", "infinity"]
