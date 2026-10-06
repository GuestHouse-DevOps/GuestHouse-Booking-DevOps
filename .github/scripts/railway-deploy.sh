#!/usr/bin/env bash
# Point a Railway service at a Docker image and start a new deployment.
# Usage: railway-deploy.sh <service-id> <image>
# Needs RAILWAY_TOKEN set to a project token for the target environment.
set -euo pipefail

: "${RAILWAY_TOKEN:?RAILWAY_TOKEN is empty, check the Railway token secret}"
service_id="${1:?service ID is empty, check the Railway service ID secret}"
image="${2:?image is empty}"

gql() {
  curl -fsS https://backboard.railway.com/graphql/v2 \
    -H "Project-Access-Token: $RAILWAY_TOKEN" \
    -H 'Content-Type: application/json' \
    -d "$1" | jq -e 'if .errors then error(.errors | tostring) else .data end'
}

environment_id=$(gql '{"query":"{ projectToken { environmentId } }"}' | jq -r .projectToken.environmentId)

gql "$(jq -n --arg s "$service_id" --arg e "$environment_id" --arg i "$image" '{
  query: "mutation($s: String!, $e: String!, $i: String!) { serviceInstanceUpdate(serviceId: $s, environmentId: $e, input: { source: { image: $i } }) }",
  variables: { s: $s, e: $e, i: $i }
}')" > /dev/null

gql "$(jq -n --arg s "$service_id" --arg e "$environment_id" '{
  query: "mutation($s: String!, $e: String!) { serviceInstanceDeployV2(serviceId: $s, environmentId: $e) }",
  variables: { s: $s, e: $e }
}')" > /dev/null

echo "Deployed $image to service $service_id"
