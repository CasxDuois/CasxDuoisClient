import os, sys, zipfile, re

base_dir = os.path.dirname(os.path.abspath(__file__))
mc = os.path.expandvars(r'%APPDATA%\.minecraft')
jre_source = r'C:\Program Files\Eclipse Adoptium\jdk-8.0.502.7-hotspot\jre'
output_zip = os.path.join(base_dir, 'client_payload.zip')

needed_libs = set()
for jf in [
    os.path.join(mc, 'versions', '1.12.2', '1.12.2.json'),
    os.path.join(mc, 'versions', '1.12.2-forge-14.23.5.2860', '1.12.2-forge-14.23.5.2860.json'),
    os.path.join(mc, 'versions', '1.12.2-OptiFine_HD_U_G5', 'OptiFine 1.12.2.json')
]:
    if os.path.exists(jf):
        with open(jf, 'r', encoding='utf-8') as f:
            content = f.read()
            for m in re.finditer(r'"path":\s*"([^"]+)"', content):
                needed_libs.add(m.group(1).replace('/', '\\'))

needed_libs.add(r'net\minecraftforge\forge\1.12.2-14.23.5.2860\forge-1.12.2-14.23.5.2860.jar')
needed_libs.add(r'optifine\OptiFine\1.12.2_HD_U_G5\OptiFine-1.12.2_HD_U_G5.jar')
needed_libs.add(r'optifine\launchwrapper\2.2\launchwrapper-2.2.jar')
needed_libs.add(r'net\minecraft\launchwrapper\1.12\launchwrapper-1.12.jar')

print("Creating client payload zip...")
with zipfile.ZipFile(output_zip, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    # 1. JRE 8 Portable
    print("  Adding portable JRE 8...")
    for root, dirs, files in os.walk(jre_source):
        for file in files:
            full = os.path.join(root, file)
            rel = os.path.relpath(full, jre_source).replace('\\', '/')
            arcname = f".minecraft/runtime/jre-x64/{rel}"
            z.write(full, arcname)

    # 2. Versions
    print("  Adding versions...")
    for vname in ['1.12.2', '1.12.2-forge-14.23.5.2860', '1.12.2-OptiFine_HD_U_G5']:
        vpath = os.path.join(mc, 'versions', vname)
        if os.path.exists(vpath):
            for root, dirs, files in os.walk(vpath):
                for file in files:
                    full = os.path.join(root, file)
                    rel = os.path.relpath(full, mc).replace('\\', '/')
                    z.write(full, f".minecraft/{rel}")

    # 3. Needed libraries
    print("  Adding libraries...")
    lib_base = os.path.join(mc, 'libraries')
    for rel in needed_libs:
        full = os.path.join(lib_base, rel)
        if os.path.exists(full):
            rel_norm = rel.replace('\\', '/')
            arcname = f".minecraft/libraries/{rel_norm}"
            z.write(full, arcname)

    # 4. Mods
    print("  Adding mods...")
    latest_jar = os.path.join(base_dir, 'build', 'libs', 'CasxDuoisMCClient-1.0.0-LITE.jar')
    if os.path.exists(latest_jar):
        z.write(latest_jar, ".minecraft/mods/CasxDuoisMCClient-1.0.0.jar")

    mods_dir = os.path.join(mc, 'mods')
    if os.path.exists(mods_dir):
        for file in os.listdir(mods_dir):
            fn = file.lower()
            if fn.endswith('.jar') and not 'casxduois' in fn and not 'ascend' in fn and not ('mixin' in fn and 'mixinbooter' not in fn):
                full = os.path.join(mods_dir, file)
                z.write(full, f".minecraft/mods/{file}")

    # 5. Assets index
    idx = os.path.join(mc, 'assets', 'indexes', '1.12.json')
    if os.path.exists(idx):
        z.write(idx, ".minecraft/assets/indexes/1.12.json")

    # 6. Config
    splash = os.path.join(mc, 'config', 'splash.properties')
    if os.path.exists(splash):
        z.write(splash, ".minecraft/config/splash.properties")

    # 7. Launcher
    launcher_exe = os.path.join(base_dir, 'CasxDuoisLauncher.exe')
    if os.path.exists(launcher_exe):
        z.write(launcher_exe, "CasxDuoisLauncher.exe")

zip_size = os.path.getsize(output_zip)
print(f"Zip created successfully! Size: {zip_size / (1024*1024):.2f} MB")
