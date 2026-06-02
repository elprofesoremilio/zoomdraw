# Windows
## Para VM-Options en windows

    --enable-native-access=javafx.graphics
    --enable-native-access=com.github.kwhat.jnativehook
    --enable-native-access=com.sun.jna

Para que funcione Dorbox en WINDOWS, en VM-Options y en opts del pom

        --enable-native-access=ALL-UNNAMED
        --add-reads=dorkbox.utilities=java.desktop 
        --add-reads=dorkbox.systemtray=java.desktop 
        --add-opens=java.desktop/java.awt=ALL-UNNAMED 
        --add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED

# Dorkbox en linux
## VM Options y pom
    --add-reads=dorkbox.utilities=java.desktop
    --add-reads=dorkbox.systemtray=java.desktop

## Instalar
    sudo apt update && sudo apt install -y libayatana-appindicator3-1