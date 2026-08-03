#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

mode="full"
stage_git="false"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --quick)
      mode="quick"
      shift
      ;;
    --skip-tests)
      mode="skip"
      shift
      ;;
    --stage-git)
      stage_git="true"
      shift
      ;;
    *)
      echo "Usage: ./setup-agent.sh [--quick] [--skip-tests] [--stage-git]" >&2
      exit 2
      ;;
  esac
done

if [[ ! -f build.gradle.kts || ! -d src/main/java/com/example/circleworkspace ]]; then
  echo "Run this script from the Variable Radius Circle Workspace repository root." >&2
  exit 1
fi

command -v python3 >/dev/null 2>&1 || {
  echo "Python 3 is required." >&2
  exit 1
}

python3 scripts/refresh_agent_index.py
python3 scripts/verify_agent_contract.py
python3 scripts/refresh_agent_index.py --check

if [[ "$mode" == "quick" ]]; then
  scripts/agent_validate.sh --quick
elif [[ "$mode" == "full" ]]; then
  scripts/agent_validate.sh
fi

if [[ "$stage_git" == "true" ]]; then
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || {
    echo "Cannot stage files because this is not a Git work tree." >&2
    exit 1
  }
  mapfile -t agent_files < <(grep -Ev '^[[:space:]]*(#|$)' scripts/agent_file_manifest.txt)
  git add -- "${agent_files[@]}"
  echo "AI-DLC files staged. No commit or push was performed."
fi

echo "AI-DLC setup complete."
