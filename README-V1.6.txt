ENJIWS MUSIC V1.6 — WORLDWIDE DISCOVERY

Fitur pencarian:
- Apple/iTunes Search API: pencarian katalog lagu mainstream di storefront US, Indonesia, UK, Jepang, dan Korea.
  Hasil katalog Apple ditampilkan sebagai metadata dan membuka halaman lagu di Apple Music/iTunes.
  Apple preview audio tidak dipakai sebagai pemutar dalam aplikasi; Apple membatasi penggunaan preview untuk promosi konten toko, bukan hiburan.
- Jamendo: pencarian dan streaming lagu yang tersedia dengan Jamendo client_id.
- Audius: pencarian dan streaming lagu publik yang tersedia.

Tetap dipertahankan:
- Direct URL Player
- LRCLIB lyrics / LRC sync
- Media3 background playback
- UI hijau gelap Enjiws Music

PENTING:
- Apple Music/iTunes Search memberi penemuan katalog mainstream dan tautan keluar, bukan stream penuh di aplikasi.
- Pemutaran penuh Apple Music memerlukan integrasi Apple Music API / MusicKit yang sesuai, developer token dan otorisasi pengguna/akses layanan. Itu bukan bagian dari update ini.
- Tidak semua lagu tersedia di setiap storefront atau provider.
- Jamendo dan Audius tetap menyediakan stream sesuai ketersediaan dan ketentuan masing-masing.
- Source belum diuji dengan Android SDK/Gradle build. Pertahankan APK sebelumnya sampai GitHub Actions berhasil.

INSTALL:
1. Ekstrak ZIP ke root ~/EnjiwsMusicNew dan overwrite.
2. git add .
3. git commit -m "Add worldwide mainstream music discovery"
4. git push origin main
5. Tunggu GitHub Actions; unduh artifact Enjiws-Music-APK bila build hijau.
