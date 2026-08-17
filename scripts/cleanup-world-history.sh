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

# Branches that exist only in a local clone (typically the branch carrying the
# removal commit itself) are not part of the mirror and would keep pointing at
# pre-rewrite objects. List them in EXTRA_BRANCHES to pull them in first, e.g.
#   EXTRA_BRANCHES="chore/remove-world-data" scripts/cleanup-world-history.sh
LOCAL_REPO="${LOCAL_REPO:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"
EXTRA_BRANCHES="${EXTRA_BRANCHES:-}"

# Set FILTER_REPO_BIN to a standalone git-filter-repo script to avoid a
# system-wide install (it is a single self-contained Python file).
FILTER_REPO_BIN="${FILTER_REPO_BIN:-}"

# SOURCE=remote clones the mirror over the network. SOURCE=local assembles it from
# LOCAL_REPO's remote-tracking refs instead, which needs no bulk transfer - useful
# when a full clone of ~80 MiB keeps timing out. Local mode is only as current as
# the last fetch, so it runs `git fetch --tags --prune` first (refs only, cheap)
# and fails if that does not succeed.
SOURCE="${SOURCE:-remote}"

die() { printf '\nerror: %s\n' "$*" >&2; exit 1; }
step() { printf '\n==> %s\n' "$*"; }

# ---------------------------------------------------------------------------
# 0. Preconditions
# ---------------------------------------------------------------------------
step "Checking prerequisites"

if [ -n "$FILTER_REPO_BIN" ]; then
  [ -x "$FILTER_REPO_BIN" ] || die "FILTER_REPO_BIN is not executable: $FILTER_REPO_BIN"
  FILTER_REPO=("$FILTER_REPO_BIN")
elif git filter-repo --version >/dev/null 2>&1; then
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

Or download the standalone script (no install, single Python file) and point
FILTER_REPO_BIN at it:
  curl -fsSLo /tmp/git-filter-repo \\
    https://raw.githubusercontent.com/newren/git-filter-repo/v2.47.0/git-filter-repo
  chmod +x /tmp/git-filter-repo
  FILTER_REPO_BIN=/tmp/git-filter-repo scripts/cleanup-world-history.sh

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

case "$SOURCE" in
remote)
  step "Cloning $REMOTE_URL (mirror, all refs)"
  git clone --mirror "$REMOTE_URL" "$TARGET"
  ;;
local)
  step "Refreshing $LOCAL_REPO's view of the remote (refs only)"
  git -C "$LOCAL_REPO" fetch origin --tags --prune \
    || die "fetch failed - the remote-tracking refs would be stale, refusing to rewrite"

  step "Assembling the mirror from $LOCAL_REPO (no bulk transfer)"
  git init --bare --quiet "$TARGET"
  git -C "$TARGET" remote add origin "$REMOTE_URL"

  # Map every remote-tracking branch to a real branch. refs/remotes/origin/HEAD is
  # a symbolic alias, not a branch, and must not become refs/heads/HEAD.
  REFSPECS=()
  while read -r ref; do
    REFSPECS+=("$ref:refs/heads/${ref#refs/remotes/origin/}")
  done < <(git -C "$LOCAL_REPO" for-each-ref --format='%(refname)' refs/remotes/origin \
             | grep -v '^refs/remotes/origin/HEAD$')
  [ "${#REFSPECS[@]}" -gt 0 ] || die "no remote-tracking branches found in $LOCAL_REPO"

  git -C "$TARGET" fetch --no-tags --quiet "$LOCAL_REPO" "${REFSPECS[@]}"
  git -C "$TARGET" fetch --quiet "$LOCAL_REPO" 'refs/tags/*:refs/tags/*'
  printf '    branches: %s    tags: %s\n' \
    "$(git -C "$TARGET" for-each-ref --format='%(refname)' refs/heads | wc -l)" \
    "$(git -C "$TARGET" for-each-ref --format='%(refname)' refs/tags | wc -l)"
  ;;
*)
  die "SOURCE must be 'remote' or 'local', got: $SOURCE"
  ;;
esac

# Background maintenance repacks the mirror while we work on it, which races with
# both the backup copy and filter-repo's own object handling. Turn it off.
git -C "$TARGET" config gc.auto 0
git -C "$TARGET" config maintenance.auto false

if [ -n "$EXTRA_BRANCHES" ]; then
  step "Adding local-only branches from $LOCAL_REPO: $EXTRA_BRANCHES"
  for b in $EXTRA_BRANCHES; do
    git -C "$LOCAL_REPO" rev-parse --verify --quiet "refs/heads/$b" >/dev/null \
      || die "local branch does not exist: $b"
    git -C "$TARGET" fetch --no-tags "$LOCAL_REPO" "refs/heads/$b:refs/heads/$b"
    printf '    %s -> %s\n' "$b" "$(git -C "$TARGET" rev-parse --short "refs/heads/$b")"
  done
fi

step "Cloning an untouched backup mirror to $BACKUP"
# A real clone rather than cp -a: it cannot be tripped up by transient lock files,
# and --no-hardlinks keeps the backup's objects independent of the repack below.
git clone --mirror --no-hardlinks "$TARGET" "$BACKUP"
git -C "$BACKUP" config gc.auto 0
git -C "$BACKUP" config maintenance.auto false

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
