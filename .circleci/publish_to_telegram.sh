#!/usr/bin/env bash
set -euo pipefail

if [[ -n "${CIRCLE_COMPARE_URL:-}" ]]; then
	COMMIT_RANGE=$(echo "${CIRCLE_COMPARE_URL}" | cut -d/ -f7)
	CAPTION=$(git log --pretty=format:"- %s" "${COMMIT_RANGE}" --reverse) || true
else
	CAPTION=$(git log -1 --pretty=format:"- %s") || true
fi

if [[ -n "${CAPTION:-}" ]]; then
    CAPTION=$(echo "$CAPTION" | cut -c -1024)
else
	CAPTION="No changelog available"
fi

curl -s -S \
	-F chat_id="$TELEGRAM_CHAT_ID" \
	-F caption="$CAPTION" \
	-F document=@$(find ./build/outputs/apk/logRelease/ -type f -name "*.apk") \
	https://api.telegram.org/bot${TELEGRAM_BOT_ID}/sendDocument >/dev/null
