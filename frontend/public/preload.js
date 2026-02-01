const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  // Proje (klasör) seçme
  openProject: () => ipcRenderer.invoke('dialog:openProject'),
  // Class dosyası seçme
  openClass:   () => ipcRenderer.invoke('dialog:openClass'),

  openJar:   () => ipcRenderer.invoke('dialog:openJar')
});
