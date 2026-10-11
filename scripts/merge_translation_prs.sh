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
  rebase_failed=""
  if ! git rebase -q "origin/$BASE" >/dev/null 2>&1; then
    while [ -d "$(git rev-parse --git-path rebase-merge)" ] || [ -d "$(git rev-parse --git-path rebase-apply)" ]; do
      conflicted=$(git diff --name-only --diff-filter=U)
      if [ -z "$conflicted" ] || echo "$conflicted" | grep -qv '^app/src/main/res/values-[^/]*/strings\.xml$'; then
        echo "non-strings conflict: ${conflicted:-unknown}"; rebase_failed=conflict; break
      fi
      if ! python3 scripts/resolve_strings_conflict.py $conflicted; then rebase_failed=resolve; break; fi
      echo "resolved conflicts: $conflicted"
      GIT_EDITOR=true git rebase --continue >/dev/null 2>&1 || true
    done
    if [ -n "$rebase_failed" ]; then
      git rebase --abort 2>/dev/null; failed+=("#$num $rebase_failed"); continue
    fi
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
    git push -q --force-with-lease="$branch:$(git rev-parse "origin/$branch")" origin "HEAD:$branch" || { failed+=("#$num push"); continue; }
  fi
  gh pr checks "$num" --watch --fail-fast >/dev/null 2>&1
  if gh pr merge "$num" --rebase --delete-branch; then
    merged+=("#$num"); echo "merged"
  else
    failed+=("#$num merge"); echo "merge failed"
  fi
done <<< "$prs"

git checkout -q "$start_branch"
echo; echo "Merged: ${merged[*]:-none}"; echo "Failed: ${failed[*]:-none}"
