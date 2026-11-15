#!/bin/bash
set -euo pipefail

secret_string="$(python3 -c 'import json, os; print(json.dumps({"username": os.environ["DB_USERNAME"], "password": os.environ["DB_PASSWORD"]}))')"
awslocal secretsmanager create-secret \
  --name "$DB_CREDENTIALS_SECRET_NAME" \
  --secret-string "$secret_string"
