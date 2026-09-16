#!/bin/sh
set -e

# Renders env.js from the container's actual runtime config at startup — this
# is what lets one built image be deployed against any backend URL / Google
# OAuth client without rebuilding.
export API_URL="${API_URL:-http://localhost:8081}"
export GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}"
envsubst '${API_URL} ${GOOGLE_CLIENT_ID}' < /etc/nginx/env.template.js > /usr/share/nginx/html/env.js

exec nginx -g 'daemon off;'
