# Junior Variasi Control Android — V1

Android wrapper untuk membuka Web Control ESP32 lokal tanpa Chrome.

## Alur V1
1. Splash logo Junior Variasi ±1.3 detik.
2. Pertama kali: masukkan IP modul (default contoh `10.206.200.160`).
3. IP disimpan di HP.
4. Web Control ESP32 dibuka fullscreen di dalam WebView.
5. Tekan lama pada halaman Web Control untuk mengganti IP modul.

## ESP32
Tidak ada perubahan firmware ESP32. APK hanya membuka halaman HTTP yang sudah disediakan ESP32.

## Android permissions
- INTERNET
- ACCESS_NETWORK_STATE

HTTP lokal diizinkan dengan `usesCleartextTraffic=true` karena ESP32 melayani Web Control melalui `http://`.

## Build
GitHub Actions otomatis build debug APK pada push ke `main` atau manual `workflow_dispatch`.
Artifact: `junior-variasi-apk`.
