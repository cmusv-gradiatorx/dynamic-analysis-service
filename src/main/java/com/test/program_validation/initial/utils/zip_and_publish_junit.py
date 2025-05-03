import os
import zipfile
import base64
from google.cloud import pubsub_v1
# ---------- CONFIG ----------
FOLDER_TO_ZIP = "src/build/reports"
ZIP_NAME = "test-results-junit.zip"
PROJECT_ID = "gradiator-x-454207"
TOPIC_ID = "dynamic-analysis-result"
# ----------------------------
def zip_folder(folder_path, zip_path):
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zipf:
        for root, _, files in os.walk(folder_path):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, start=folder_path)
                zipf.write(full_path, arcname=rel_path)
    print(f":white_check_mark: Zipped folder {folder_path} -> {zip_path}")
def publish_base64_zip(zip_path, project_id, topic_id):
    publisher = pubsub_v1.PublisherClient()
    topic_path = publisher.topic_path(project_id, topic_id)
    with open(zip_path, "rb") as f:
        zip_data = f.read()
    # Base64 encode the zip
    base64_zip = base64.b64encode(zip_data).decode("utf-8")  # convert to UTF-8 str
    # Publish the message
    future = publisher.publish(topic_path, base64_zip.encode("utf-8"))  # send back as bytes
    print(":outbox_tray: Published message ID:", future.result())
if __name__ == "__main__":
    zip_folder(FOLDER_TO_ZIP, ZIP_NAME)
    publish_base64_zip(ZIP_NAME, PROJECT_ID, TOPIC_ID)
