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
   scripts/cleanup-world-history.sh
   ```

   It needs `git-filter-repo` (`pacman -S git-filter-repo`,
   `apt install git-filter-repo`, or `pipx install git-filter-repo`) and aborts
   with installation instructions if it is missing. Do not fall back to
   `git filter-branch`.

4. Inspect the rewritten mirror it reports: pack size should drop to roughly
   1 MiB, `git log --all --oneline -- worlds test-server` must be empty, and the
   ref count should match the original.
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
