#!/bin/bash

endpoint_pr="$COSMOSDB_ENDPOINT_PR"
password_pr="$COSMOSDB_PASSWORD_PR"

endpoint_push="$COSMOSDB_ENDPOINT_PUSH"
password_push="$COSMOSDB_PASSWORD_PUSH"

git_branch=$(git branch | grep "*" | cut -d ' ' -f2-)

if [[ "$git_branch" = *"FETCH_HEAD"* ]]; then
    endpoint=$endpoint_pr
    password=$password_pr
else
    endpoint=$endpoint_push
    password=$password_push
fi

cat << EOF
cosmosdb.key=$password
cosmosdb.uri=$endpoint

EOF

