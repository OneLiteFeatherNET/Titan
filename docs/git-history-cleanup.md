# Purging world data from the git history

`worlds/` and `test-server/` were removed from the working tree and gitignored.
That stops the repository from growing further, but it does **not** shrink a
clone: the blobs stay reachable from older commits.

| | size |
|---|---|
| `.git` pack today | ~81 MiB |
| Blobs belonging to `worlds/` + `test-server/` | ~80 MiB |
| Everything else (code, gradle, docs, history) | ~0.8 MiB |

Roughly 98 % of every clone is Anvil region data. Reclaiming it requires
rewriting history, which changes every commit SHA in the repository.

## What the rewrite costs

- **All commit SHAs change.** Existing clones cannot be fast-forwarded; everyone
  re-clones. Commit links in issues, PRs, and changelogs stop resolving.
- **Open pull requests break.** They point at old SHAs. At the time of writing
  there are 11 remote branches (renovate updates, `release-please`,
  `feat/elytra-vanilla-boost`, `legacy/main-2026-05`, …). Each one has to be
  recreated from rebased work, or merged/closed before the rewrite.
- **Force-push to `main`** must be temporarily permitted in GitHub's branch
  protection.
- **Release tags are rewritten too.** Tags stay in place (filter-repo rewrites
  them), but anything pinning a Titan commit SHA — CI in other repos, deployment
  manifests — needs checking. Published Maven artifacts are unaffected; they
  reference versions, not SHAs.

Because of this, the cheapest safe window is right after all open PRs are merged
or closed, announced ahead of time to everyone with a clone.

## Procedure

1. Merge or close every open pull request. Note the remaining branches you want
   to keep — the rewrite preserves all refs, but rebasing is easier with fewer.
2. Announce a push freeze.
3. Run the prepared script. It clones a fresh mirror, keeps an untouched backup
   mirror alongside it, rewrites, verifies, and prints the push command **without
   running it**:

   ```bash
   scripts/cleanup-world-history.sh [work-directory]
   ```

   It needs `git-filter-repo` (`pacman -S git-filter-repo`,
   `apt install git-filter-repo`, or `pipx install git-filter-repo`) and aborts
   with installation instructions if it is missing. Do not fall back to
   `git filter-branch` — it is far slower and mangles tags and merge commits.

   Two environment variables matter:

   - `EXTRA_BRANCHES` — branches that exist only in your local clone. The mirror
     is cloned from the remote, so a local-only branch would keep pointing at
     pre-rewrite objects and its commits would be lost. List them here:

     ```bash
     EXTRA_BRANCHES="chore/remove-world-data" scripts/cleanup-world-history.sh
     ```

   - `FILTER_REPO_BIN` — path to a standalone `git-filter-repo` script, so no
     system-wide install is needed (it is a single self-contained Python file):

     ```bash
     curl -fsSLo /tmp/git-filter-repo \
       https://raw.githubusercontent.com/newren/git-filter-repo/v2.47.0/git-filter-repo
     chmod +x /tmp/git-filter-repo
     FILTER_REPO_BIN=/tmp/git-filter-repo scripts/cleanup-world-history.sh
     ```

   - `SOURCE` — `remote` (default) clones the mirror over the network. Pulling
     ~80 MiB from GitHub can time out (`RPC failed; curl 56 Recv failure`); in
     that case use `SOURCE=local`, which assembles the mirror from your clone's
     remote-tracking refs and needs no bulk transfer. Local mode runs
     `git fetch --tags --prune` first and aborts if that fails, so it cannot
     silently rewrite a stale snapshot.

     ```bash
     SOURCE=local scripts/cleanup-world-history.sh
     ```

4. Inspect the rewritten mirror it reports. A verified dry run of this procedure
   produced:

   | | before | after |
   |---|---|---|
   | pack size | 81.15 MiB | **1.08 MiB** |
   | refs (branches + tags) | 78 | 78 |
   | commits | 672 | 664 |

   The eight dropped commits are the ones that touched nothing but world data
   (`[titan#14] Add test worlds`, `chore(worlds): upgrade lobby worlds to
   Minecraft 1.21.11`, and similar) and became empty. Pre-existing empty commits
   such as `build: trigger the release pipeline` are left alone.

   Worth re-checking yourself before pushing:

   ```bash
   NEW=<rewritten-mirror>; OLD=<backup-mirror>
   git -C "$NEW" log --all --oneline -- worlds test-server   # must be empty
   git -C "$NEW" fsck --no-progress                          # must be clean

   # every ref's content outside the world paths must be bit-identical
   for ref in $(git -C "$OLD" for-each-ref --format='%(refname)' refs/heads refs/tags); do
     a=$(git -C "$OLD" ls-tree -r "$ref" | grep -vE $'\t(worlds|test-server)/' | sha256sum)
     b=$(git -C "$NEW" ls-tree -r "$ref" | grep -vE $'\t(worlds|test-server)/' | sha256sum)
     [ "$a" = "$b" ] || echo "MISMATCH: $ref"
   done
   ```
5. Publish:

   ```bash
   git -C <rewritten-mirror> push --force --mirror https://github.com/OneLiteFeatherNET/Titan.git
   ```

6. Ask GitHub Support to run `git gc` on the server side if the reported
   repository size does not drop — GitHub keeps unreachable objects for a while.
7. Everyone re-clones. Do not `git pull` into an old clone; it will drag the old
   objects back in on the next push.
8. Keep the backup mirror until the new history has been in use for a while.

## Restoring from the backup

The script leaves an untouched mirror next to the rewritten one. If the rewrite
turns out wrong after the push:

```bash
git -C <backup-mirror> push --force --mirror https://github.com/OneLiteFeatherNET/Titan.git
```

## Where the worlds live now

Nowhere in git. `worlds/` must be supplied out-of-band in the working directory;
see [world-conversion.md](world-conversion.md). One consequence worth knowing:
`generateAotCache` boots the lobby to record the AOT cache and therefore needs
world data. Without it the task is skipped and the `aot` publication artifact is
omitted from the release, even though deployments launch with
`-XX:AOTCache=app-titan.aot`. Point the build at a world directory with
`-Ptitan.worlds.dir=<path>` to restore it, or decide on a permanent source for
build-time world data.
