import zipfile
import json
import os
import hashlib

start_code = """package mcp.client;

import java.util.Arrays;
import net.minecraft.client.main.Main;

public class Start
{
    public static void main(String[] args)
    {
        String assets = System.getenv().containsKey("assetDirectory") ? System.getenv("assetDirectory") : "assets";
        Main.main(concat(new String[]{"--version", "mcp", "--accessToken", "0", "--assetsDir", assets, "--assetIndex", "16", "--userProperties", "{}"}, args));
    }

    public static <T> T[] concat(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
"""

zips = [
    os.path.expanduser("~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_config/26.2/mcp_config-26.2.zip"),
    os.path.expanduser("~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_config/26.2-20260616.103818/mcp_config-26.2-20260616.103818.zip"),
    os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/26.2/mcp.zip"),
    os.path.expanduser("~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_config/26.3/mcp_config-26.3.zip"),
    os.path.expanduser("~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_config/26.3-20260918.230105/mcp_config-26.3-20260918.230105.zip"),
    os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/26.3/mcp.zip")
]

# Add local build output zip if present
root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
build_zip = os.path.join(root_dir, "build", "downloadMcpConfig", "output.zip")
zips.append(build_zip)

# Also dynamically find any mcp_config*.zip or mcp.zip under ~/.gradle/caches/forge_gradle
cache_dir = os.path.expanduser("~/.gradle/caches/forge_gradle")
if os.path.exists(cache_dir):
    for root, dirs, files in os.walk(cache_dir):
        for f in files:
            if f.endswith(".zip") and ("mcp_config" in f or f == "mcp.zip"):
                zips.append(os.path.join(root, f))

zips = list(set(zips))

for zp in zips:
    if not os.path.exists(zp):
        continue
    try:
        with zipfile.ZipFile(zp, "r") as zin:
            entries = {info.filename: zin.read(info.filename) for info in zin.infolist()}
        
        needs_patch = False
        if "config.json" in entries:
            cfg = json.loads(entries["config.json"].decode("utf-8"))
            if "data" in cfg and "inject" not in cfg["data"]:
                cfg["data"]["inject"] = "inject/"
                entries["config.json"] = json.dumps(cfg, indent=4).encode("utf-8")
                needs_patch = True
        
        if "inject/mcp/client/Start.java" not in entries:
            entries["inject/mcp/client/Start.java"] = start_code.encode("utf-8")
            needs_patch = True
            
        if not needs_patch:
            continue

        temp_zp = zp + ".tmp"
        with zipfile.ZipFile(temp_zp, "w", compression=zipfile.ZIP_DEFLATED) as zout:
            for fname, data in entries.items():
                zout.writestr(fname, data)
                
        os.replace(temp_zp, zp)

        # Update MD5 and SHA1 checksums
        with open(zp, "rb") as f:
            content = f.read()
            md5_hash = hashlib.md5(content).hexdigest()
            sha1_hash = hashlib.sha1(content).hexdigest()

        md5_path = zp + ".md5"
        with open(md5_path, "w", encoding="utf-8") as f:
            f.write(md5_hash)

        sha1_path = zp + ".sha1"
        if os.path.exists(sha1_path):
            with open(sha1_path, "w", encoding="utf-8") as f:
                f.write(sha1_hash)

        print(f"Patched {zp} successfully")
    except Exception as e:
        print(f"Error patching {zp}: {e}")

# Patch version.json and client/server mappings for 26.x if needed
for ver in ["26.2", "26.3"]:
    for base_repo in ["minecraft_repo", "mcp_repo"]:
        base_dir = os.path.expanduser(f"~/.gradle/caches/forge_gradle/{base_repo}/versions/{ver}")
        if not os.path.exists(base_dir):
            continue
        dummy = b"# (c) 2026 Mojang\r\n"
        h = hashlib.sha1(dummy).hexdigest()
        sz = len(dummy)
        for map_name in ["client_mappings.txt", "server_mappings.txt"]:
            map_path = os.path.join(base_dir, map_name)
            if not os.path.exists(map_path):
                with open(map_path, "wb") as f:
                    f.write(dummy)
                with open(map_path + ".sha1", "w") as f:
                    f.write(h)
        vpath = os.path.join(base_dir, "version.json")
        if os.path.exists(vpath):
            try:
                v = json.load(open(vpath, "r", encoding="utf-8"))
                d = v.setdefault("downloads", {})
                updated = False
                if "client_mappings" not in d:
                    d["client_mappings"] = {"sha1": h, "size": sz, "url": f"https://piston-data.mojang.com/v1/objects/{h}/client_mappings.txt"}
                    updated = True
                if "server_mappings" not in d:
                    d["server_mappings"] = {"sha1": h, "size": sz, "url": f"https://piston-data.mojang.com/v1/objects/{h}/server_mappings.txt"}
                    updated = True
                if updated:
                    with open(vpath, "w", encoding="utf-8") as f:
                        json.dump(v, f, indent=2)
                    print(f"Patched {vpath} with mappings downloads")
            except Exception as e:
                print(f"Error patching {vpath}: {e}")
