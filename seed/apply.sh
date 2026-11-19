#!/usr/bin/env bash
# Writes the seed job, jobs, into Jenkins at JENKINS_URL from seed/config.xml,
# creating it where Jenkins lacks it, then builds it at COMMIT, which applies
# jobs/ with Job DSL. It follows the build to its end, prints its log, and exits
# non-zero unless the build succeeds. It signs in as JENKINS_USER with
# JENKINS_PASSWORD, which stays out of every command line, and keeps the
# session cookie that Jenkins's CSRF crumb is bound to.
set -euo pipefail
umask 077

: "${JENKINS_URL:?}" "${JENKINS_USER:?}" "${JENKINS_PASSWORD:?}" "${COMMIT:?}"
job='jobs'
config="$(dirname "$0")/config.xml"

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
printf 'user = "%s:%s"\n' "$JENKINS_USER" "$JENKINS_PASSWORD" > "$work/curlrc"

api() {
  curl -sS -K "$work/curlrc" -b "$work/cookies" -c "$work/cookies" "$@"
}

crumb=$(api -f "$JENKINS_URL/crumbIssuer/api/json" | jq -r '.crumbRequestField + ": " + .crumb')

status=$(api -o /dev/null -w '%{http_code}' "$JENKINS_URL/job/$job/api/json")
case "$status" in
  200) url="$JENKINS_URL/job/$job/config.xml" ;;
  404) url="$JENKINS_URL/createItem?name=$job" ;;
  *) echo "Jenkins answered $status for the seed job" >&2; exit 1 ;;
esac
api -f -H "$crumb" -H 'Content-Type: application/xml' --data-binary "@$config" "$url"
echo "Wrote $JENKINS_URL/job/$job/"

queue=$(api -f -D - -o /dev/null -H "$crumb" --data-urlencode "COMMIT=$COMMIT" \
          "$JENKINS_URL/job/$job/buildWithParameters" | tr -d '\r' | sed -n 's/^[Ll]ocation: //p')
echo "Queued at $queue"

build=''
for _ in $(seq 120); do
  item=$(api -f "${queue}api/json")
  if [ "$(jq -r '.cancelled // false' <<<"$item")" = true ]; then
    echo 'The seed build was cancelled while queued' >&2
    exit 1
  fi
  build=$(jq -r '.executable.url // empty' <<<"$item")
  [ -n "$build" ] && break
  sleep 5
done
[ -n "$build" ] || { echo 'The seed build never left the queue' >&2; exit 1; }
echo "Building $build"

result=''
for _ in $(seq 240); do
  state=$(api -f "${build}api/json?tree=building,result")
  if [ "$(jq -r .building <<<"$state")" = false ]; then
    result=$(jq -r .result <<<"$state")
    break
  fi
  sleep 5
done
api -f "${build}consoleText"
echo "Result: ${result:-still building}"
[ "$result" = SUCCESS ]
