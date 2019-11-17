#!/usr/bin/env bash
set -euo pipefail

CHANGELOG=$(git log --pretty=format:"- %s" "${COMMIT_RANGE:1:-1}" --reverse)

if [[ ${DISCORD_WEBHOOK_URL:+1} ]]; then
  FILE="$(find ./build/outputs/apk/logRelease/ -type f -name "*.apk")"
  FILENAME="$(basename "$FILE")"
  DOWNLOAD_URL=$(curl --silent --show-error --upload-file "$FILE" https://transfer.sh/"$FILENAME")

  curl --silent --show-error --request POST \
    "$DISCORD_WEBHOOK_URL" \
    --header 'Content-Type: application/json' \
    --data '{
      "embeds": [
        {
          "title": "'"$FILENAME"'",
          "url": "'"$DOWNLOAD_URL"'",
          "description": "'"${CHANGELOG//$'\n'/\\n}"'",
          "color": "9047566"
        }
      ]
    }'

  unset FILE
  unset DOWNLOAD_URL
else
  echo "DISCORD_WEBHOOK_URL not set."
  exit 1
fi

unset CAPTION
