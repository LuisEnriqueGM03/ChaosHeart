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
    os.path.expanduser("~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_config/26.3/mcp_config-26.3.zip"),
    os.path.expanduser("~/.gradle/caches/forge_gradle/maven_downloader/de/oceanlabs/mcp/mcp_config/26.3-20260918.230105/mcp_config-26.3-20260918.230105.zip"),
    os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/26.3/mcp.zip")
]

# Add local build output zip if present
root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
build_zip = os.path.join(root_dir, "build", "downloadMcpConfig", "output.zip")
zips.append(build_zip)

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
