#!/bin/bash
set -e
# Check if Docker is installed
if ! command -v docker &> /dev/null
then
    echo "Docker is not installed. Please install Docker and try again."
    exit 1
fi

# Image name
IMAGE_NAME="gstreamer-container"

# Check if the Docker image exists, build it if it doesn't
echo "Building the Docker image..."
docker build -t $IMAGE_NAME .

# Run the container with host networking
echo "Starting the container..."
docker run \
    -it \
    --rm \
    --env NO_LOOP=$1 \
    --network="host" \
    --name gstreamer-instance \
$IMAGE_NAME
