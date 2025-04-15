#!/bin/bash
# Exit on any error
set -e
# Input arguments
FOLDER_TO_ZIP=src/build/reports
ZIP_NAME=test-results.zip
TOPIC_NAME=dynamic-analysis-result
# Zip the folder
echo "Zipping folder $FOLDER_TO_ZIP to $ZIP_NAME..."
zip -r "$ZIP_NAME" "$FOLDER_TO_ZIP"
echo "$ZIP_NAME" > encoded_zip.txt
# Optional: base64 encode if binary payload (safest)
# BASE64_ZIP=$(base64 "$ZIP_NAME")
# Publish the base64 encoded zip to Pub/Sub
echo "Publishing zipped file to Pub/Sub topic $TOPIC_NAME..."
gcloud pubsub topics publish "$TOPIC_NAME" \
  --message="$(< encoded_zip.txt)"

echo ":white_check_mark: Done. Zipped and published!"
