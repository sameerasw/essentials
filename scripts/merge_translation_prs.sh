#!/usr/bin/env bash
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

BASE=develop
ONLY="${1:-}"
git fetch -q origin
start_branch=$(git rev-parse --abbrev-ref HEAD)

prs=$(gh pr list --base "$BASE" --state open --limit 100 --json number,headRefName,createdAt \
  --jq '[.[] | select(.headRefName | startswith("translations-"))] | sort_by(.createdAt)[] | "\(.number) \(.headRefName)"')

merged=(); failed=()
while read -r num branch; do
  [ -z "$num" ] && continue
  [ -n "$ONLY" ] && [ "$ONLY" != "$num" ] && continue
  echo "=== #$num ($branch) ==="
  git fetch -q origin "$BASE" "$branch"
  git checkout -q -B "$branch" "origin/$branch" || { failed+=("#$num checkout"); continue; }
  if ! git merge -q --no-edit "origin/$BASE" 2>/dev/null; then
    conflicted=$(git diff --name-only --diff-filter=U)
    if echo "$conflicted" | grep -qv '^app/src/main/res/values-[^/]*/strings\.xml$'; then
      echo "non-strings conflict: $conflicted"; git merge --abort 2>/dev/null; failed+=("#$num conflict"); continue
    fi
    python3 scripts/resolve_strings_conflict.py $conflicted || { git merge --abort; failed+=("#$num resolve"); continue; }
    git commit -q --no-edit
    echo "resolved conflicts: $conflicted"
  fi
  stray=$(git diff --name-only "origin/$BASE" HEAD | grep -v '^app/src/main/res/values-[^/]*/strings\.xml$')
  if [ -n "$stray" ]; then
    echo "touches non-translation files: $stray"; failed+=("#$num non-translation"); continue
  fi
  if ! python3 scripts/validate_strings.py >/dev/null 2>&1; then
    python3 scripts/validate_strings.py --fix >/dev/null 2>&1
    if ! python3 scripts/validate_strings.py; then
      git checkout -q -- . ; failed+=("#$num validation"); continue
    fi
    git commit -qam "fix: translation validation issues"
    echo "auto-fixed validation issues"
  fi
  if [ "$(git rev-parse HEAD)" != "$(git rev-parse "origin/$branch")" ]; then
    git push -q origin "HEAD:$branch" || { failed+=("#$num push"); continue; }
  fi
  gh pr checks "$num" --watch --fail-fast >/dev/null 2>&1
  if gh pr merge "$num" --rebase --delete-branch || gh pr merge "$num" --merge --delete-branch; then
    merged+=("#$num"); echo "merged"
  else
    failed+=("#$num merge"); echo "merge failed"
  fi
done <<< "$prs"

git checkout -q "$start_branch"
echo; echo "Merged: ${merged[*]:-none}"; echo "Failed: ${failed[*]:-none}"
