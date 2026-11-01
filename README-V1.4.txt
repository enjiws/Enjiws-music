ENJIWS MUSIC V1.4 — ONLINE STREAMING & LYRICS

Isi update:
- Online screen baru dari halaman Library.
- Pemutar URL audio langsung menggunakan Media3 yang sama dengan pemutar lokal.
- Pencarian katalog Jamendo (read API).
- Pencarian lirik LRCLIB dan parsing timestamp LRC untuk highlight mengikuti posisi lagu.
- Permission INTERNET.
- VersionCode 5 / VersionName 1.4.

PENTING:
1. Jamendo memerlukan client_id. ID 709fa152 adalah ID testing read API dari dokumentasi Jamendo, bukan kredensial produksi. Daftarkan aplikasi sendiri di https://devportal.jamendo.com/ dan ganti ID pada kolom client_id di halaman Online.
2. URL streaming harus berupa URL audio langsung yang dapat diakses publik. Halaman web/URL YouTube biasa bukan direct audio URL.
3. Streaming tergantung format/codec dan server sumber. Media3 mendukung format progresif umum dan streaming yang kompatibel.
4. LRCLIB tidak memerlukan API key. Lirik hanya muncul jika tersedia dan metadata cocok. Lirik LRC tersinkronisasi akan di-highlight berdasarkan posisi playback.
5. Update ini mempertahankan MusicPlaybackService.kt dari V1.3.

Instalasi:
Ekstrak seluruh isi ZIP ke root folder project ~/EnjiwsMusicNew dengan overwrite.
Kemudian:
git add .
git commit -m "Add online streaming and synced lyrics"
git push origin main

Setelah GitHub Actions berhasil, unduh artifact APK dan install sebagai update. Jangan uninstall versi lama.
