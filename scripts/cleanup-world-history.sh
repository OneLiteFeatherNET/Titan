#!/usr/bin/env bash
#
# Purges the tracked Minecraft world data (`worlds/`, `test-server/`) from the
# entire git history. Removing the directories in a normal commit stops further
# growth but leaves ~80 MiB of blobs in the pack, so every clone keeps paying for
# them; only a history rewrite reclaims that.
#
# This script is deliberately NON-DESTRUCTIVE: it works on a fresh mirror clone
# in a scratch directory, keeps a second untouched mirror as a backup, and prints
# the force-push command instead of running it. Nothing happens to your working
# repository, and nothing reaches the remote until a human runs the final push.
#
# Usage:  scripts/cleanup-world-history.sh [work-directory]
#
# See docs/git-history-cleanup.md for the full procedure and its consequences.

set -euo pipefail

REMOTE_URL="${REMOTE_URL:-https://github.com/OneLiteFeatherNET/Titan.git}"
WORK_DIR="${1:-${TMPDIR:-/tmp}/titan-history-cleanup}"
PATHS=(worlds test-server)

die() { printf '\nerror: %s\n' "$*" >&2; exit 1; }
step() { printf '\n==> %s\n' "$*"; }

# ---------------------------------------------------------------------------
# 0. Preconditions
# ---------------------------------------------------------------------------
step "Checking prerequisites"

if git filter-repo --version >/dev/null 2>&1; then
  FILTER_REPO=(git filter-repo)
elif command -v git-filter-repo >/dev/null 2>&1; then
  FILTER_REPO=(git-filter-repo)
else
  die "git-filter-repo is not installed.

Install it with one of:
  pacman -S git-filter-repo          # Arch / CachyOS
  apt install git-filter-repo        # Debian / Ubuntu
  pipx install git-filter-repo       # any platform
  brew install git-filter-repo       # macOS

Do not substitute 'git filter-branch': it is orders of magnitude slower and
mangles tags and merge commits."
fi
printf '    git-filter-repo: %s\n' "$("${FILTER_REPO[@]}" --version)"

[ -e "$WORK_DIR" ] && die "work directory already exists: $WORK_DIR (remove it or pass another path)"

# ---------------------------------------------------------------------------
# 1. Mirror clone + backup
# ---------------------------------------------------------------------------
mkdir -p "$WORK_DIR"
BACKUP="$WORK_DIR/titan-backup.git"
TARGET="$WORK_DIR/titan-rewrite.git"

step "Cloning $REMOTE_URL (mirror, all refs)"
git clone --mirror "$REMOTE_URL" "$TARGET"

step "Copying an untouched backup mirror to $BACKUP"
cp -a "$TARGET" "$BACKUP"

size_of() { git -C "$1" count-objects -vH | awk '/size-pack/ {print $2, $3}'; }
BEFORE="$(size_of "$TARGET")"
REFS_BEFORE="$(git -C "$TARGET" for-each-ref --format='%(refname)' | wc -l)"
printf '    pack size before : %s\n    refs before      : %s\n' "$BEFORE" "$REFS_BEFORE"

# ---------------------------------------------------------------------------
# 2. Rewrite
# ---------------------------------------------------------------------------
step "Rewriting history: dropping ${PATHS[*]} from every commit on every ref"
FILTER_ARGS=()
for p in "${PATHS[@]}"; do FILTER_ARGS+=(--path "$p"); done

# git-filter-repo may be installed as a plain script that does not understand
# `git -C`, so run it from inside the mirror instead.
( cd "$TARGET" && "${FILTER_REPO[@]}" "${FILTER_ARGS[@]}" --invert-paths --force )

step "Repacking aggressively"
git -C "$TARGET" reflog expire --expire=now --all
git -C "$TARGET" gc --prune=now --aggressive

AFTER="$(size_of "$TARGET")"
REFS_AFTER="$(git -C "$TARGET" for-each-ref --format='%(refname)' | wc -l)"

# ---------------------------------------------------------------------------
# 3. Verify
# ---------------------------------------------------------------------------
step "Verifying that no world blob survived"
LEFTOVER=0
for p in "${PATHS[@]}"; do
  hits="$(git -C "$TARGET" log --all --oneline -- "$p" | wc -l)"
  printf '    commits still touching %-12s : %s\n' "$p" "$hits"
  LEFTOVER=$((LEFTOVER + hits))
done
[ "$LEFTOVER" -eq 0 ] || die "world paths still present in the rewritten history - do NOT push"

printf '    refs kept        : %s (was %s)\n' "$REFS_AFTER" "$REFS_BEFORE"
[ "$REFS_AFTER" -eq "$REFS_BEFORE" ] || \
  printf '    note: ref count changed - filter-repo drops refs whose every commit vanished.\n'

step "Result"
cat <<EOF
    pack size before : $BEFORE
    pack size after  : $AFTER

    rewritten mirror : $TARGET
    untouched backup : $BACKUP

Nothing has been pushed. Review the rewritten mirror, then - after announcing a
freeze to everyone with a clone or an open PR - publish it with:

    git -C "$TARGET" push --force --mirror "$REMOTE_URL"

Afterwards every clone is invalid: all commit SHAs changed. Everybody re-clones,
open pull requests must be recreated from re-based work, and GitHub's branch
protection on 'main' has to allow the force-push for the duration of the change.
Keep $BACKUP until the dust has settled.
EOF
