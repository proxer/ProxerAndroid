#!/usr/bin/env bash
set -euo pipefail

CHANGELOG=$(git log --pretty=format:"- %s" "${COMMIT_RANGE:1:-1}" --reverse)

if [[ ${TELEGRAM_CHAT_ID:+1} ]] && [[ ${TELEGRAM_BOT_ID:+1} ]]; then
  curl --silent --show-error --request POST \
    https://api.telegram.org/bot"$TELEGRAM_BOT_ID"/sendDocument \
    --form chat_id="$TELEGRAM_CHAT_ID" \
    --form caption="$CHANGELOG" \
    --form document=@"$(find ./build/outputs/apk/logRelease/ -type f -name "*.apk")"
else
  echo "TELEGRAM_CHAT_ID or TELEGRAM_BOT_ID not set."
  exit 1
fi

unset CHANGELOG
