REQUIRED_FILE="./config/wg0.conf"

# Check if the file does NOT exist as a regular file
if [ ! -f "$REQUIRED_FILE" ]; then
    echo "Add wg configuration file '$REQUIRED_FILE'!" >&2
    exit 1
fi

docker compose up --detach
