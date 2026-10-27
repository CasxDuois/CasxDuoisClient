const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn, exec } = require('child_process');

const PORT = 39821;
const APPDATA = process.env.APPDATA || path.join(process.env.USERPROFILE, 'AppData', 'Roaming');
const MC_DIR = path.join(APPDATA, '.minecraft');
const CFG_DIR = path.join(APPDATA, '.casxduois_launcher');
const ACCOUNTS_FILE = path.join(CFG_DIR, 'accounts.txt');

if (!fs.existsSync(CFG_DIR)) {
  fs.mkdirSync(CFG_DIR, { recursive: true });
}

function detectJava8() {
  const appData = process.env.APPDATA || '';
  const mc = path.join(appData, '.minecraft');

  // 1. Portable JRE tích hợp
  const portable = path.join(mc, 'runtime', 'jre-x64', 'bin', 'javaw.exe');
  if (fs.existsSync(portable)) return portable;

  // 2. TLauncher Java 8
  const tlPaths = [
    path.join(appData, '.tlauncher', 'legacy', 'Minecraft', 'jre', 'jre-legacy', 'windows-x64', 'jre-legacy', 'bin', 'javaw.exe'),
    path.join(appData, '.tlauncher', 'cache', 'jvms', 'jre-legacy', 'bin', 'javaw.exe')
  ];
  for (const tp of tlPaths) {
    if (fs.existsSync(tp)) return tp;
  }

  // 3. Minecraft Official Launcher Java 8
  const mojangPaths = [
    path.join(mc, 'runtime', 'jre-legacy', 'windows-x64', 'jre-legacy', 'bin', 'javaw.exe'),
    path.join(mc, 'runtime', 'java-runtime-alpha', 'windows-x64', 'java-runtime-alpha', 'bin', 'javaw.exe'),
    path.join(mc, 'runtime', '1.8.0', 'bin', 'javaw.exe'),
    'C:\\Program Files (x86)\\Minecraft Launcher\\runtime\\jre-legacy\\windows-x64\\jre-legacy\\bin\\javaw.exe',
    'C:\\Program Files\\Minecraft Launcher\\runtime\\jre-legacy\\windows-x64\\jre-legacy\\bin\\javaw.exe'
  ];
  for (const mp of mojangPaths) {
    if (fs.existsSync(mp)) return mp;
  }

  // 4. Eclipse Adoptium
  const adoptiumBases = [
    'C:\\Program Files\\Eclipse Adoptium',
    'C:\\Program Files (x86)\\Eclipse Adoptium'
  ];

  for (const aBase of adoptiumBases) {
    if (fs.existsSync(aBase)) {
      try {
        const dirs = fs.readdirSync(aBase);
        const jdk8 = dirs.find(d => d.includes('8') && (d.startsWith('jdk') || d.startsWith('jre')));
        if (jdk8) {
          const p = path.join(aBase, jdk8, 'bin', 'javaw.exe');
          if (fs.existsSync(p)) return p;
        }
        for (const d of dirs) {
          const p = path.join(aBase, d, 'bin', 'javaw.exe');
          if (fs.existsSync(p)) return p;
        }
      } catch (e) {}
    }
  }

  const candidates = [
    'C:\\Program Files\\Eclipse Adoptium\\jdk-8.0.502.7-hotspot\\bin\\javaw.exe',
    'C:\\Program Files\\BellSoft\\LibericaJDK-8-Full\\bin\\javaw.exe',
    'C:\\Program Files\\Zulu\\zulu-8\\bin\\javaw.exe',
    'C:\\Program Files\\Java\\jre1.8.0_442\\bin\\javaw.exe',
    'C:\\Program Files\\Java\\jdk1.8.0_442\\bin\\javaw.exe',
    'javaw.exe'
  ];

  for (const c of candidates) {
    if (fs.existsSync(c)) return c;
  }
  return 'javaw.exe';
}

function buildForgeClasspath() {
  const vDir = path.join(MC_DIR, 'versions', '1.12.2');
  const jar = path.join(vDir, '1.12.2.jar');
  const json1122 = path.join(vDir, '1.12.2.json');
  const forgeVDir = path.join(MC_DIR, 'versions', '1.12.2-forge-14.23.5.2860');
  const forgeJson = path.join(forgeVDir, '1.12.2-forge-14.23.5.2860.json');
  const libBase = path.join(MC_DIR, 'libraries');

  const cp = [];
  const added = new Set();

  function addLib(fullPath) {
    if (fullPath && fs.existsSync(fullPath) && !added.has(fullPath) && !fullPath.includes('natives')) {
      cp.push(fullPath);
      added.add(fullPath);
    }
  }

  // 1. Forge libraries from 1.12.2-forge-14.23.5.2860.json
  if (fs.existsSync(forgeJson)) {
    try {
      const fJson = JSON.parse(fs.readFileSync(forgeJson, 'utf8'));
      if (fJson.libraries) {
        for (const lib of fJson.libraries) {
          if (lib.downloads && lib.downloads.artifact && lib.downloads.artifact.path) {
            const rel = lib.downloads.artifact.path.replace(/\//g, path.sep);
            addLib(path.join(libBase, rel));
          }
        }
      }
    } catch (e) {
      console.error('Error parsing Forge json', e);
    }
  }

  // 2. Vanilla 1.12.2 libraries
  if (fs.existsSync(json1122)) {
    try {
      const vJson = JSON.parse(fs.readFileSync(json1122, 'utf8'));
      if (vJson.libraries) {
        for (const lib of vJson.libraries) {
          if (lib.downloads && lib.downloads.artifact && lib.downloads.artifact.path) {
            const rel = lib.downloads.artifact.path.replace(/\//g, path.sep);
            addLib(path.join(libBase, rel));
          }
        }
      }
    } catch (e) {
      console.error('Error parsing 1.12.2.json', e);
    }
  }

  // 3. Fallback direct check for Forge jar if not in list
  const forgeDirectJar = path.join(libBase, 'net', 'minecraftforge', 'forge', '1.12.2-14.23.5.2860', 'forge-1.12.2-14.23.5.2860.jar');
  addLib(forgeDirectJar);

  // 4. Vanilla 1.12.2.jar
  addLib(jar);

  return cp.join(';');
}

function buildOptiFineClasspath() {
  const vDir = path.join(MC_DIR, 'versions', '1.12.2');
  const jar = path.join(vDir, '1.12.2.jar');
  const json1122 = path.join(vDir, '1.12.2.json');
  const optiJson = path.join(MC_DIR, 'versions', '1.12.2-OptiFine_HD_U_G5', 'OptiFine 1.12.2.json');
  const libBase = path.join(MC_DIR, 'libraries');

  const cp = [];
  const added = new Set();

  function addLib(fullPath) {
    if (fullPath && fs.existsSync(fullPath) && !added.has(fullPath) && !fullPath.includes('natives')) {
      cp.push(fullPath);
      added.add(fullPath);
    }
  }

  addLib(path.join(libBase, 'optifine', 'OptiFine', '1.12.2_HD_U_G5', 'OptiFine-1.12.2_HD_U_G5.jar'));
  addLib(path.join(libBase, 'optifine', 'launchwrapper', '2.2', 'launchwrapper-2.2.jar'));
  addLib(path.join(libBase, 'net', 'minecraft', 'launchwrapper', '1.12', 'launchwrapper-1.12.jar'));

  if (fs.existsSync(optiJson)) {
    try {
      const oJson = JSON.parse(fs.readFileSync(optiJson, 'utf8'));
      if (oJson.libraries) {
        for (const lib of oJson.libraries) {
          if (lib.downloads && lib.downloads.artifact && lib.downloads.artifact.path) {
            const rel = lib.downloads.artifact.path.replace(/\//g, path.sep);
            addLib(path.join(libBase, rel));
          }
        }
      }
    } catch (e) {}
  }

  if (fs.existsSync(json1122)) {
    try {
      const vJson = JSON.parse(fs.readFileSync(json1122, 'utf8'));
      if (vJson.libraries) {
        for (const lib of vJson.libraries) {
          if (lib.downloads && lib.downloads.artifact && lib.downloads.artifact.path) {
            const rel = lib.downloads.artifact.path.replace(/\//g, path.sep);
            addLib(path.join(libBase, rel));
          }
        }
      }
    } catch (e) {}
  }

  addLib(jar);
  return cp.join(';');
}

function launchMinecraftForge(username, ramMB) {
  const javaExe = detectJava8();
  const natives = path.join(MC_DIR, 'versions', '1.12.2', '1.12.2-natives');
  const assetsDir = path.join(MC_DIR, 'assets');
  const cpStr = buildForgeClasspath();

  try {
    const cfgDir = path.join(MC_DIR, 'config');
    if (!fs.existsSync(cfgDir)) fs.mkdirSync(cfgDir, { recursive: true });
    const splashFile = path.join(cfgDir, 'splash.properties');
    if (!fs.existsSync(splashFile)) {
      fs.writeFileSync(splashFile, 'enabled=false\n', 'utf8');
    }
  } catch (e) {}

  try {
    const modsDir = path.join(MC_DIR, 'mods');
    if (!fs.existsSync(modsDir)) fs.mkdirSync(modsDir, { recursive: true });

    // Xóa triệt để các mod Mixin cũ trước khi nạp bản mới
    if (fs.existsSync(modsDir)) {
      const files = fs.readdirSync(modsDir);
      for (const f of files) {
        const fl = f.toLowerCase();
        if (fl.includes('ascend') || (fl.includes('casxduois') && fl.endsWith('.jar')) || (fl.includes('mixin') && !fl.includes('mixinbooter') && fl.endsWith('.jar'))) {
          try { fs.unlinkSync(path.join(modsDir, f)); } catch (e) {}
        }
      }
    }
    const mixinOut = path.join(MC_DIR, '.mixin.out');
    if (fs.existsSync(mixinOut)) {
      try { fs.rmdirSync(mixinOut, { recursive: true }); } catch (e) {}
    }

    const candidates = [
      path.join(__dirname, '..', 'build', 'libs', 'CasxDuoisMCClient-1.0.0-LITE.jar'),
      path.join(__dirname, '..', 'build', 'libs', 'CasxDuoisMCClient-1.0.0-reobf.jar')
    ];
    for (const c of candidates) {
      if (fs.existsSync(c)) {
        fs.copyFileSync(c, path.join(modsDir, 'CasxDuoisMCClient-1.0.0.jar'));
        break;
      }
    }
  } catch (e) {
    console.error('Error syncing client jar to mods', e);
  }

  const xms = Math.Min ? Math.Min(512, ramMB) : Math.min(512, ramMB);
  const args = [
    `-Xms${xms}M`,
    `-Xmx${ramMB}M`,
    '-XX:+UseG1GC',
    '-XX:+ParallelRefProcEnabled',
    '-XX:MaxGCPauseMillis=200',
    '-XX:+UnlockExperimentalVMOptions',
    '-XX:+DisableExplicitGC',
    '-XX:+AlwaysPreTouch',
    '-Dsun.java2d.opengl=true',
    '-Dfml.splash=false',
    `-Djava.library.path=${natives}`,
    '-Dminecraft.launcher.brand=CasxDuoisMC',
    '-cp',
    cpStr,
    'net.minecraft.launchwrapper.Launch',
    '--username',
    username,
    '--version',
    '1.12.2-forge-14.23.5.2860',
    '--gameDir',
    MC_DIR,
    '--assetsDir',
    assetsDir,
    '--assetIndex',
    '1.12',
    '--uuid',
    '0',
    '--accessToken',
    '0',
    '--userType',
    'legacy',
    '--tweakClass',
    'net.minecraftforge.fml.common.launcher.FMLTweaker',
    '--versionType',
    'Forge'
  ];

  const child = spawn(javaExe, args, {
    cwd: MC_DIR,
    detached: true,
    stdio: 'ignore'
  });

  child.unref();
  return child.pid;
}

function launchMinecraftOptiFine(username, ramMB) {
  const javaExe = detectJava8();
  const natives = path.join(MC_DIR, 'versions', '1.12.2', '1.12.2-natives');
  const assetsDir = path.join(MC_DIR, 'assets');
  const cpStr = buildOptiFineClasspath();

  const xms = Math.min(512, ramMB);
  const args = [
    `-Xms${xms}M`,
    `-Xmx${ramMB}M`,
    '-XX:+UseG1GC',
    '-Dsun.java2d.opengl=true',
    `-Djava.library.path=${natives}`,
    '-Dminecraft.launcher.brand=CasxDuoisMC',
    '-cp',
    cpStr,
    'net.minecraft.launchwrapper.Launch',
    '--username',
    username,
    '--version',
    '1.12.2-OptiFine',
    '--gameDir',
    MC_DIR,
    '--assetsDir',
    assetsDir,
    '--assetIndex',
    '1.12',
    '--uuid',
    '0',
    '--accessToken',
    '0',
    '--userType',
    'legacy',
    '--tweakClass',
    'optifine.OptiFineTweaker'
  ];

  const child = spawn(javaExe, args, {
    cwd: MC_DIR,
    detached: true,
    stdio: 'ignore'
  });

  child.unref();
  return child.pid;
}

function launchMinecraft(username, ramMB, mode = 0) {
  if (mode === 1) {
    return launchMinecraftOptiFine(username, ramMB);
  } else {
    return launchMinecraftForge(username, ramMB);
  }
}

const server = http.createServer((req, res) => {
  const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
  const pathname = parsedUrl.pathname;

  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    return res.end();
  }

  // Static files
  if (pathname === '/' || pathname === '/index.html') {
    const html = fs.readFileSync(path.join(__dirname, 'index.html'));
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    return res.end(html);
  }
  if (pathname === '/style.css') {
    const css = fs.readFileSync(path.join(__dirname, 'style.css'));
    res.writeHead(200, { 'Content-Type': 'text/css; charset=utf-8' });
    return res.end(css);
  }
  if (pathname === '/app.js') {
    const js = fs.readFileSync(path.join(__dirname, 'app.js'));
    res.writeHead(200, { 'Content-Type': 'application/javascript; charset=utf-8' });
    return res.end(js);
  }
  if (pathname === '/logo.png') {
    const img = fs.readFileSync(path.join(__dirname, 'logo.png'));
    res.writeHead(200, { 'Content-Type': 'image/png' });
    return res.end(img);
  }

  // REST API Endpoints
  if (pathname === '/api/accounts' && req.method === 'GET') {
    let accounts = ['CasxDuoisPlayer'];
    if (fs.existsSync(ACCOUNTS_FILE)) {
      const lines = fs.readFileSync(ACCOUNTS_FILE, 'utf8').split('\n').map(l => l.trim()).filter(Boolean);
      if (lines.length > 0) accounts = lines;
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ accounts }));
  }

  if (pathname === '/api/accounts' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const data = JSON.parse(body);
        if (data.accounts && Array.isArray(data.accounts)) {
          fs.writeFileSync(ACCOUNTS_FILE, data.accounts.join('\n'), 'utf8');
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: true }));
      } catch (e) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  if (pathname === '/api/launch' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const data = JSON.parse(body);
        const username = data.username || 'CasxDuoisPlayer';
        const ram = data.ram || 1024;
        const mode = data.mode !== undefined ? parseInt(data.mode, 10) : 0;
        const pid = launchMinecraft(username, ram, mode);

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true, pid }));

        if (data.autoClose) {
          setTimeout(() => {
            process.exit(0);
          }, 1200);
        }
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  if (pathname === '/api/launch-all' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const data = JSON.parse(body);
        const accounts = data.accounts || ['CasxDuoisPlayer'];
        const ram = data.ram || 1024;
        const mode = data.mode !== undefined ? parseInt(data.mode, 10) : 0;

        accounts.forEach((acc, i) => {
          setTimeout(() => {
            launchMinecraft(acc, ram, mode);
          }, i * 2000);
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ success: true }));

        if (data.autoClose) {
          setTimeout(() => {
            process.exit(0);
          }, accounts.length * 2000 + 1500);
        }
      } catch (e) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: e.message }));
      }
    });
    return;
  }

  if (pathname === '/api/open-folder') {
    exec(`explorer.exe "${MC_DIR}"`);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ success: true }));
  }

  if (pathname === '/api/window/close') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ success: true }));
    setTimeout(() => process.exit(0), 200);
    return;
  }

  res.writeHead(404);
  res.end('Not Found');
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`CasxDuoisMC Launcher Backend listening on http://127.0.0.1:${PORT}`);
});
