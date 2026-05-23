#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  scripts/create-draft-pr.sh --feature "기능 이름" --body path/to/pr-body.md [--base main]

Creates a GitHub Draft PR with title "(WIP) feat: 기능 이름".
The current branch must be a committed feature branch.
USAGE
}

feature_name=""
body_file=""
base_branch="main"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --feature)
      feature_name="${2:-}"
      shift 2
      ;;
    --body)
      body_file="${2:-}"
      shift 2
      ;;
    --base)
      base_branch="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$feature_name" || -z "$body_file" ]]; then
  usage >&2
  exit 1
fi

if [[ ! -f "$body_file" ]]; then
  echo "PR body file not found: $body_file" >&2
  exit 1
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI is not installed. Install gh first." >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI is not authenticated. Run: gh auth login" >&2
  exit 1
fi

current_branch="$(git branch --show-current)"

if [[ -z "$current_branch" ]]; then
  echo "Could not determine current Git branch." >&2
  exit 1
fi

if [[ "$current_branch" == "$base_branch" ]]; then
  echo "Create a feature branch before opening a Draft PR." >&2
  exit 1
fi

if [[ ! "$current_branch" =~ ^feat/[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)?$ ]]; then
  echo "Draft feature PR branches must follow feat/{feature-name} or feat/{feature-name}/{implementation-name}." >&2
  exit 1
fi

if [[ -n "$(git status --porcelain)" ]]; then
  echo "Working tree is not clean. Commit checklist/docs before creating the PR." >&2
  exit 1
fi

git remote get-url origin >/dev/null
git push -u origin "$current_branch"

gh pr create \
  --draft \
  --base "$base_branch" \
  --head "$current_branch" \
  --title "(WIP) feat: $feature_name" \
  --body-file "$body_file"
