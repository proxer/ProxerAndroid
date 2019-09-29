#!/usr/bin/env bash
set -euo pipefail

CAPTION=$(git log --pretty=format:"- %s" "${COMMIT_RANGE:1:-1}" --reverse)

if [[ ${TELEGRAM_CHAT_ID:+1} ]] && [[ ${TELEGRAM_BOT_ID:+1} ]]; then
    curl -s -S \
        -F chat_id="$TELEGRAM_CHAT_ID" \
        -F caption="$CAPTION" \
        -F document=@$(find ./build/outputs/apk/logRelease/ -type f -name "*.apk") \
        https://api.telegram.org/bot${TELEGRAM_BOT_ID}/sendDocument >/dev/null
else
    echo "TELEGRAM_CHAT_ID or TELEGRAM_BOT_ID not set."
    exit -1
fi

unset CAPTION
