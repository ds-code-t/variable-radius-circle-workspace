#!/usr/bin/env python3
"""Validate repository agent files and optionally check change coverage."""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]

CANONICAL_AI_FILES = (
    ".ai/PROJECT.md",
    ".ai/REQUIREMENTS.md",
    ".ai/DOMAIN_RULES.md",
    ".ai/ARCHITECTURE.md",
    ".ai/UI_BEHAVIOR.md",
    ".ai/CURRENT_STATE.md",
    ".ai/TESTING.md",
    ".ai/UPDATE_PROTOCOL.md",
    ".ai/CHANGELOG.md",
    ".ai/AGENT_FRAMEWORK.md",
)

REQUIRED_FILES = (
    "AGENTS.md",
    "CLAUDE.md",
    "GEMINI.md",
    "AI-DLC.md",
    "AI-DLC-INSTALLATION.md",
    "README.md",
    "REVIEW.md",
    "START-HERE.md",
    ".aiassistant/rules/variable-radius-circle.md",
    ".amazonq/rules/variable-radius-circle.md",
    ".clinerules/01-variable-radius-circle.md",
    ".continue/rules/01-variable-radius-circle.md",
    ".cursor/rules/project.mdc",
    ".github/copilot-instructions.md",
    ".github/agents/variable-radius-circle-maintainer.agent.md",
    ".github/instructions/java.instructions.md",
    ".github/instructions/documentation.instructions.md",
    ".github/instructions/persistence.instructions.md",
    ".github/pull_request_template.md",
    ".github/workflows/agent-context.yml",
    ".junie/guidelines.md",
    ".windsurf/rules/01-variable-radius-circle.md",
    "docs/agent/README.md",
    "docs/agent/feature-map.md",
    "docs/agent/change-checklist.md",
    "docs/agent/prompt-examples.md",
    "docs/agent/repository-index.md",
    "scripts/agent_file_manifest.txt",
    "scripts/refresh_agent_index.py",
    "scripts/verify_agent_contract.py",
    "scripts/agent_validate.sh",
    "scripts/agent_validate.ps1",
    "setup-agent.ps1",
    "setup-agent.sh",
) + CANONICAL_AI_FILES

ADAPTER_FILES = (
    "CLAUDE.md",
    "GEMINI.md",
    ".aiassistant/rules/variable-radius-circle.md",
    ".amazonq/rules/variable-radius-circle.md",
    ".clinerules/01-variable-radius-circle.md",
    ".continue/rules/01-variable-radius-circle.md",
    ".cursor/rules/project.mdc",
    ".github/copilot-instructions.md",
    ".github/agents/variable-radius-circle-maintainer.agent.md",
    ".junie/guidelines.md",
    ".windsurf/rules/01-variable-radius-circle.md",
)

SKILL_FILES = (
    ".agents/skills/variable-radius-circle-functionality-change/SKILL.md",
    ".claude/skills/variable-radius-circle-functionality-change/SKILL.md",
    ".github/skills/variable-radius-circle-functionality-change/SKILL.md",
    ".cursor/skills/variable-radius-circle-functionality-change/SKILL.md",
    ".windsurf/skills/variable-radius-circle-functionality-change/SKILL.md",
)

BEHAVIOR_PREFIXES = (
    "src/main/java/",
    "src/main/resources/",
)
BEHAVIOR_FILES = {
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "setup-project.ps1",
}
TEST_PREFIXES = ("src/test/",)
LIVING_DOC_PREFIXES = (".ai/",)
LIVING_DOC_FILES = {
    "AGENTS.md",
    "docs/agent/feature-map.md",
    "README.md",
}
CHANGELOG_FILE = ".ai/CHANGELOG.md"


def env_true(name: str) -> bool:
    return os.environ.get(name, "").strip().lower() in {"1", "true", "yes", "on"}


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def starts_with_any(path: str, prefixes: tuple[str, ...]) -> bool:
    return any(path.startswith(prefix) for prefix in prefixes)


def run_git(args: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )


def git_changed_files(base_ref: str) -> list[str] | None:
    result = run_git(["diff", "--name-only", f"{base_ref}...HEAD"])
    if result.returncode != 0:
        result = run_git(["diff", "--name-only", base_ref, "HEAD"])
    if result.returncode != 0:
        print(
            f"WARNING: Could not compare changes with {base_ref}: "
            f"{result.stderr.strip()}",
            file=sys.stderr,
        )
        return None

    changed = {
        line.strip().replace("\\", "/")
        for line in result.stdout.splitlines()
        if line.strip()
    }

    # Include staged and unstaged work so local pre-commit validation is useful,
    # while CI still sees the committed branch diff.
    for local_args in (["diff", "--name-only"], ["diff", "--cached", "--name-only"]):
        local = run_git(local_args)
        if local.returncode == 0:
            changed.update(
                line.strip().replace("\\", "/")
                for line in local.stdout.splitlines()
                if line.strip()
            )

    return sorted(changed)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-ref", help="Git base ref used for change-coverage checks.")
    parser.add_argument(
        "--strict",
        action="store_true",
        help="Treat change-coverage warnings as errors.",
    )
    args = parser.parse_args()

    errors: list[str] = []
    warnings: list[str] = []

    expected_source_dir = ROOT / "src" / "main" / "java" / "com" / "example" / "circleworkspace"
    if not (ROOT / "build.gradle.kts").is_file() or not expected_source_dir.is_dir():
        errors.append(
            "This does not look like the Variable Radius Circle Workspace repository root "
            "(expected build.gradle.kts and src/main/java/com/example/circleworkspace)."
        )

    for relative in REQUIRED_FILES:
        if not (ROOT / relative).is_file():
            errors.append(f"Missing required agent/context file: {relative}")

    manifest_path = ROOT / "scripts" / "agent_file_manifest.txt"
    if manifest_path.is_file():
        raw_manifest = [
            line.strip()
            for line in manifest_path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("#")
        ]
        if len(raw_manifest) != len(set(raw_manifest)):
            errors.append("scripts/agent_file_manifest.txt contains duplicate paths.")
        for relative in raw_manifest:
            normalized = Path(relative)
            if normalized.is_absolute() or ".." in normalized.parts:
                errors.append(f"Unsafe path in agent file manifest: {relative}")
            elif not (ROOT / normalized).is_file():
                errors.append(f"Agent file manifest references a missing file: {relative}")
        if "scripts/agent_file_manifest.txt" not in raw_manifest:
            errors.append("Agent file manifest must list itself.")

    for relative in SKILL_FILES:
        if not (ROOT / relative).is_file():
            errors.append(f"Missing Agent Skill copy: {relative}")

    for relative in ADAPTER_FILES:
        path = ROOT / relative
        if path.is_file() and "AGENTS.md" not in path.read_text(encoding="utf-8"):
            errors.append(f"Adapter does not reference AGENTS.md: {relative}")

    existing_skills = [ROOT / path for path in SKILL_FILES if (ROOT / path).is_file()]
    if existing_skills:
        hashes = {sha256(path) for path in existing_skills}
        if len(hashes) != 1:
            errors.append(
                "Agent Skill copies differ. Keep all "
                "variable-radius-circle-functionality-change/SKILL.md files identical."
            )

    agents_path = ROOT / "AGENTS.md"
    if agents_path.is_file():
        agents_text = agents_path.read_text(encoding="utf-8")
        for relative in CANONICAL_AI_FILES:
            if relative not in agents_text:
                errors.append(f"AGENTS.md boot/update contract does not reference {relative}")

    if args.base_ref:
        changed = git_changed_files(args.base_ref)
        if changed is not None:
            behavior_changed = any(
                starts_with_any(path, BEHAVIOR_PREFIXES) or path in BEHAVIOR_FILES
                for path in changed
            )
            tests_changed = any(starts_with_any(path, TEST_PREFIXES) for path in changed)
            living_docs_changed = any(
                starts_with_any(path, LIVING_DOC_PREFIXES) or path in LIVING_DOC_FILES
                for path in changed
            )
            changelog_changed = CHANGELOG_FILE in changed

            if behavior_changed and not tests_changed and not env_true("AGENT_CONTRACT_ALLOW_NO_TESTS"):
                warnings.append(
                    "Implementation/build behavior changed but no src/test files changed. "
                    "Add focused tests or document why this is a genuine non-testable change."
                )
            if behavior_changed and not living_docs_changed and not env_true("AGENT_CONTRACT_ALLOW_NO_DOCS"):
                warnings.append(
                    "Implementation/build behavior changed but no canonical living-context files changed."
                )
            if behavior_changed and not changelog_changed and not env_true("AGENT_CONTRACT_ALLOW_NO_CHANGELOG"):
                warnings.append(
                    "Implementation/build behavior changed but .ai/CHANGELOG.md was not updated."
                )

            rotation_changed = any(
                path.endswith((
                    "RotationSolver.java",
                    "RotationLinkPolicy.java",
                    "RotationResyncPolicy.java",
                ))
                for path in changed
            )
            rotation_test_changed = any(
                path.endswith((
                    "RotationSolverTest.java",
                    "RotationLinkPolicyTest.java",
                    "RotationResyncPolicyTest.java",
                ))
                for path in changed
            )
            if rotation_changed and not rotation_test_changed and not env_true("AGENT_CONTRACT_ALLOW_NO_TESTS"):
                warnings.append(
                    "Rotation policy/solver code changed without a corresponding focused rotation test change."
                )

            persistence_changed = any(
                path.endswith("WorkspaceStore.java")
                or path.endswith("/Model.java")
                or path == "src/main/resources/workspace.json"
                for path in changed
            )
            persistence_docs = {
                ".ai/REQUIREMENTS.md",
                ".ai/ARCHITECTURE.md",
                ".ai/TESTING.md",
                ".ai/CURRENT_STATE.md",
            }
            if persistence_changed and not any(path in persistence_docs for path in changed):
                warnings.append(
                    "Persistence-adjacent files changed without an update to persistence-relevant canonical context. "
                    "Confirm the change is not format/meaning related."
                )

    for warning in warnings:
        print(f"WARNING: {warning}", file=sys.stderr)

    if args.strict and warnings:
        errors.extend(f"Strict mode: {warning}" for warning in warnings)

    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print("Agent contract files are valid.")
    if args.base_ref:
        print(f"Change coverage checked against {args.base_ref}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
