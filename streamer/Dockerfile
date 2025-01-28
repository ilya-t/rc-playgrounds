# Base image Alpine
FROM alpine:3.21

# Install dependencies
RUN apk update && apk add --no-cache \
    gst-plugins-base \
    gst-plugins-good \
    gst-plugins-bad \
    gst-plugins-ugly \
    gst-libav \
    gstreamer-tools \
    bash

# Set environment variables for GStreamer
ENV GST_DEBUG=2

# Create working directory
WORKDIR /app

# Copy scripts (if needed)
COPY app /app
RUN chmod +x /app/*.sh

# Set the working directory as the entry point
ENTRYPOINT ["/bin/bash", "/app/entrypoint.sh"]
