# 1. Instala la librería del sistema
sudo apt install libayatana-appindicator3-1

# 2. Crea el enlace simbólico para que Java la encuentre con su nombre genérico
sudo ln -sf /usr/lib/x86_64-linux-gnu/libayatana-appindicator3.so.1 /usr/lib/x86_64-linux-gnu/libappindicator3.so.1