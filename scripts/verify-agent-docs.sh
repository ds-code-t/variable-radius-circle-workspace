#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

STRUCTURE_ONLY=false
BASE_REF="${AGENT_DOCS_BASE_REF:-}"
for arg in "$@"; do
  case "$arg" in
    --structure-only) STRUCTURE_ONLY=true ;;
    --base=*) BASE_REF="${arg#--base=}" ;;
    *) echo "Unknown argument: $arg" >&2; exit 2 ;;
  esac
done

required=(
  AGENTS.md CLAUDE.md GEMINI.md
  .github/copilot-instructions.md
  .cursor/rules/project.mdc
  .ai/PROJECT.md .ai/REQUIREMENTS.md .ai/DOMAIN_RULES.md
  .ai/ARCHITECTURE.md .ai/UI_BEHAVIOR.md .ai/TESTING.md
  .ai/CURRENT_STATE.md .ai/CHANGELOG.md .ai/UPDATE_PROTOCOL.md
  .ai/decisions/README.md
)

for file in "${required[@]}"; do
  [[ -f "$file" ]] || { echo "Missing required agent-context file: $file" >&2; exit 1; }
done

for doc in .ai/PROJECT.md .ai/REQUIREMENTS.md .ai/DOMAIN_RULES.md .ai/ARCHITECTURE.md .ai/UI_BEHAVIOR.md .ai/CURRENT_STATE.md .ai/TESTING.md .ai/UPDATE_PROTOCOL.md; do
  grep -Fq "$doc" AGENTS.md || { echo "AGENTS.md does not reference $doc" >&2; exit 1; }
done

for prefix in GEO- WRK- CON- ROT- CLK- PER- UI-; do
  grep -Fq "$prefix" .ai/REQUIREMENTS.md || { echo "Missing requirement family $prefix" >&2; exit 1; }
done

if $STRUCTURE_ONLY; then
  echo "Agent-context structure is valid."
  exit 0
fi

if ! command -v git >/dev/null 2>&1 || ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Git work tree unavailable; structure checks passed, change-set checks skipped."
  exit 0
fi

if [[ -z "$BASE_REF" ]] && git rev-parse --verify HEAD^ >/dev/null 2>&1; then
  BASE_REF=HEAD^
fi

if [[ -z "$BASE_REF" ]]; then
  echo "Agent-context structure is valid. No base revision available for change-set checks."
  exit 0
fi

mapfile -t changed < <(git diff --name-only "$BASE_REF...HEAD")
code_changed=false
for file in "${changed[@]}"; do
  case "$file" in
    src/main/*|build.gradle.kts|settings.gradle.kts|gradle.properties|gradle/*) code_changed=true ;;
  esac
done

if $code_changed; then
  printf '%s\n' "${changed[@]}" | grep -Fxq .ai/CURRENT_STATE.md || { echo "Code changed without .ai/CURRENT_STATE.md" >&2; exit 1; }
  printf '%s\n' "${changed[@]}" | grep -Fxq .ai/CHANGELOG.md || { echo "Code changed without .ai/CHANGELOG.md" >&2; exit 1; }
  printf '%s\n' "${changed[@]}" | grep -q '^\.ai/' || { echo "Code changed without a canonical .ai update" >&2; exit 1; }
fi

echo "Agent-context verification passed."
