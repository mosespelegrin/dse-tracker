#!/bin/sh
set -e

# Renders env.js from the container's actual API_URL at startup — this is
# what lets one built image be deployed against any backend URL.
export API_URL="${API_URL:-http://localhost:8081}"
envsubst '${API_URL}' < /etc/nginx/env.template.js > /usr/share/nginx/html/env.js

exec nginx -g 'daemon off;'
