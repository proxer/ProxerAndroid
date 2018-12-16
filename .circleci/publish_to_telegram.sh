#!/usr/bin/env bash
set -euo pipefail

curl -s -S
-F chat_id="$TELEGRAM_CHAT_ID"
-F caption="$(git log -1 --pretty=format:'%s')"
-F document=@$(find ./build/outputs/apk/logRelease/ -type f -name "*.apk")
https://api.telegram.org/bot${TELEGRAM_BOT_ID}/sendDocument > /dev/null
