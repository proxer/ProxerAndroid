#!/usr/bin/env bash
set -euo pipefail

if [[ -v KEYSTORE ]] && [[ -v SECRETS ]]; then
	echo "${KEYSTORE}" | base64 -d >keystore.jks
	echo "${SECRETS}" | base64 -d >secrets.properties
	echo "Created secrets"
else
	echo "Environment variables not set, skipping."
fi
