# Converting worlds to a newer Minecraft version (CLI)

Titan runs on Minestom, which only loads worlds in the **exact** Minecraft
version it targets (see the `minestom` version in `settings.gradle.kts`, e.g.
`1.21.11`). Worlds saved by older versions fail to load — e.g.
`Unknown block minecraft:chain` because `chain` was renamed to `iron_chain` in
1.21.x.

The data-fix rules that perform these migrations live inside Minecraft
(`net.minecraft`), so neither Mojang's DataFixerUpper nor PaperMC/DataConverter
can run standalone in Minestom. The robust way is a **one-off offline upgrade
with the official Minecraft server jar** (`--forceUpgrade`), which runs the real
DataFixerUpper over every chunk. Do this whenever the worlds are older than the
targeted Minestom version.

## Prerequisites

- A JDK matching the target Minecraft version (1.21.11 needs **Java 21+**;
  newer versions may need Java 25). `java -version` must satisfy it.
- Run from the repo root. Worlds live in `worlds/` and `test-server/worlds/`.

## 1. Download the target server jar

```bash
MC_VERSION=1.21.11   # must match the -<version> suffix of net.minestom:minestom

VJSON=$(curl -s https://launchermeta.mojang.com/mc/game/version_manifest_v2.json \
  | python3 -c "import sys,json;d=json.load(sys.stdin);print(next(v['url'] for v in d['versions'] if v['id']=='$MC_VERSION'))")
SJAR=$(curl -s "$VJSON" | python3 -c "import sys,json;print(json.load(sys.stdin)['downloads']['server']['url'])")
curl -s -o /tmp/mc-server.jar "$SJAR"
echo "server jar: $(ls -la /tmp/mc-server.jar | awk '{print $5}') bytes"
```

## 2. Upgrade one world

`--forceUpgrade` runs DataFixerUpper over the level on boot, then starts the
server; once the upgrade is done we stop it. Repeat per world.

```bash
upgrade_world() {            # usage: upgrade_world <worlds-root> <world-name>
  local ROOT="$1" W="$2" C="/tmp/conv_$2"
  rm -rf "$C"; mkdir -p "$C"
  cp /tmp/mc-server.jar "$C/server.jar"
  echo "eula=true" > "$C/eula.txt"
  cp -r "$ROOT/$W" "$C/$W"
  printf 'level-name=%s\nonline-mode=false\nmax-players=1\nspawn-protection=0\n' "$W" > "$C/server.properties"

  ( cd "$C" && java -Xmx2G -jar server.jar --nogui --forceUpgrade > convert.log 2>&1 & echo $! > pid )
  local pid=$(cat "$C/pid")
  # wait until the upgrade finished and the server is up, then stop it
  for i in $(seq 1 180); do grep -q "Done (" "$C/convert.log" && break; kill -0 "$pid" 2>/dev/null || break; sleep 1; done
  sleep 1; kill "$pid" 2>/dev/null; wait "$pid" 2>/dev/null

  # copy only the upgraded world content back (no server cruft: DIM-1/DIM1,
  # datapacks, playerdata, data/* indexes, logs, …)
  for sub in region entities poi; do rm -rf "$ROOT/$W/$sub"; cp -r "$C/$W/$sub" "$ROOT/$W/$sub"; done
  cp "$C/$W/level.dat" "$ROOT/$W/level.dat"
  [ -f "$C/$W/level.dat_old" ] && cp "$C/$W/level.dat_old" "$ROOT/$W/level.dat_old"
  echo "$W upgraded ($(grep -oE '[0-9]+% completed' "$C/convert.log" | tail -1))"
}

# all lobby worlds (+ the CloudNet test-server copies)
for W in winter halloween world world_generic; do upgrade_world worlds "$W"; done
for W in winter halloween world world_generic; do upgrade_world test-server/worlds "$W"; done
```

## 3. Drop server-generated cruft

The server run creates extra files we do not want in the repo:

```bash
git clean -fd worlds/ test-server/worlds/     # removes untracked data/*.dat, etc.
git checkout -- worlds/*/data test-server/worlds/*/data   # if data/ was tracked/empty
```

## 4. Verify

```bash
# DataVersion should be the target version's (e.g. 4671 for 1.21.11)
python3 -c "import gzip,struct;d=gzip.open('worlds/winter/level.dat','rb').read();i=d.find(b'DataVersion');print('DataVersion=',struct.unpack('>i',d[i+11:i+15])[0])"

# no pre-rename blocks should remain, e.g. plain minecraft:chain
python3 - <<'PY'
import zlib,struct,glob,re
chain=iron=0
for f in glob.glob('worlds/*/region/*.mca'):
    d=open(f,'rb').read(); o=b''
    for i in range(1024):
        off=struct.unpack('>I',b'\x00'+d[i*4:i*4+3])[0]
        if off==0: continue
        s=off*4096; l=struct.unpack('>I',d[s:s+4])[0]; c=d[s+4]; r=d[s+5:s+4+l]
        try: o += zlib.decompress(r) if c==2 else r
        except Exception: pass
    chain += len(re.findall(rb'minecraft:chain(?![_a-z])', o)); iron += o.count(b'minecraft:iron_chain')
print('plain chain=',chain,'iron_chain=',iron)   # expect plain chain=0
PY
```

Finally launch Titan and confirm the world loads without `Unknown block` errors
(`java -jar app/build/libs/app-titan.jar`, then a status ping or client join).

## Notes

- Source versions seen here: 1.19.4 (DataVersion 3120), 1.20.1 (3337),
  1.20.4 (3700) → upgraded to 1.21.11 (4671).
- The server also writes empty `DIM-1`/`DIM1` (nether/end), `datapacks/`,
  `playerdata/` and structure index files — do **not** commit those; the steps
  above copy back only `region/`, `entities/`, `poi/` and `level.dat`.
- After bumping the targeted Minecraft/Minestom version, re-run this whole
  process with the new `MC_VERSION`.
