// main.js

const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('path');
const { spawn } = require('child_process');

let unityProcess = null;

function launchUnity() {
  console.log('🚀 Unity süreci başlatılıyor...');

  // --- DÜZELTME BURADA ---
  // Yoldaki gereksiz olan '..' ifadesi kaldırıldı.
  const unityExePath = path.join(app.getAppPath(), 'unity-build', 'MetricVisulation.exe');
  
  const unityAppDir = path.dirname(unityExePath);

  try {
    unityProcess = spawn(unityExePath, [], { cwd: unityAppDir });

    unityProcess.stdout.on('data', (data) => {
      console.log(`[Unity Log]: ${data}`);
    });

    unityProcess.stderr.on('data', (data) => {
      console.error(`[Unity HATA]: ${data}`);
    });

    unityProcess.on('close', (code) => {
      console.log(`Unity süreci sonlandı. Çıkış kodu: ${code}`);
      if (code !== 0) {
        dialog.showErrorBox("Unity Hatası", `Unity uygulaması beklenmedik bir şekilde kapandı (Çıkış Kodu: ${code}). Lütfen Player.log dosyasını kontrol edin.`);
      }
      unityProcess = null;
    });

  } catch (err) {
    console.error("HATA: Unity süreci başlatılamadı!", err);
    dialog.showErrorBox("Başlatma Hatası", "Unity.exe dosyası başlatılamadı. Lütfen dosya yolunu kontrol edin: " + unityExePath);
  }
}

// ... (Geri kalan kodun tamamı aynı, değişiklik yok) ...

function createWindow() {
  console.log('⚡ Electron: createWindow çağrıldı');
  const win = new BrowserWindow({
    width: 1024,
    height: 768,
    webPreferences: {
      preload: path.join(__dirname, 'public/preload.js'),
      contextIsolation: true,
      nodeIntegration: false
    }
  });

  win.loadURL('http://localhost:3000');
}

app.whenReady().then(() => {
  launchUnity();
  createWindow();
});

app.on('will-quit', () => {
  if (unityProcess) {
    console.log("🛑 Unity süreci sonlandırılıyor...");
    unityProcess.kill();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

ipcMain.handle('dialog:openProject', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog({
    properties: ['openDirectory']
  });
  if (canceled) return null;
  return filePaths[0];
});

ipcMain.handle('dialog:openJar', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog({
    properties: ['openFile'],
    filters: [
      { name: 'Jar Files', extensions: ['jar'] }
    ]
  });
  if (canceled) return null;
  return filePaths[0];
});

ipcMain.handle('dialog:openClass', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog({
    properties: ['openFile'],
    filters: [
      { name: 'Class Files', extensions: ['class'] }
    ]
  });
  if (canceled) return null;
  return filePaths[0];
});