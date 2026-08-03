#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

mode="full"
base_ref=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --quick)
      mode="quick"
      shift
      ;;
    --base-ref)
      base_ref="${2:?--base-ref requires a value}"
      shift 2
      ;;
    *)
      echo "Usage: scripts/agent_validate.sh [--quick] [--base-ref REF]" >&2
      exit 2
      ;;
  esac
done

verify_args=()
if [[ -n "$base_ref" ]]; then
  verify_args+=(--base-ref "$base_ref")
fi

python3 scripts/verify_agent_contract.py "${verify_args[@]}"
python3 scripts/refresh_agent_index.py --check

if [[ -x scripts/verify-agent-docs.sh ]]; then
  scripts/verify-agent-docs.sh
fi

if [[ "$mode" == "quick" ]]; then
  ./gradlew test
else
  ./gradlew clean check
fi

echo "Variable Radius Circle Workspace validation completed ($mode mode)."
