ENJIWS MUSIC V1.5 — GLOBAL MUSIC DISCOVERY

Update ini menambahkan pencarian global gabungan dari:
- Jamendo (menggunakan client_id Jamendo yang terdaftar)
- Audius (REST API publik untuk pencarian dan streaming)

Tetap dipertahankan:
- Pemutar URL audio langsung
- Lirik online LRCLIB dan sinkronisasi LRC
- Background playback melalui Media3 MusicPlaybackService
- Desain hijau gelap Enjiws Music

CATATAN:
- Default Jamendo client_id di UI diisi dengan ID yang sebelumnya diberikan pengguna.
  ID ini dapat diganti di aplikasi. Kosongkan kolom untuk mencari hanya di Audius.
- Katalog Audius/Jamendo tidak menjamin semua rilisan komersial populer tersedia.
- Track yang ditandai stream-gated oleh Audius tidak ditampilkan.
- Ketersediaan stream bergantung pada API/provider, jaringan, dan hak akses track.
- Source update ini belum dikompilasi di lingkungan Android SDK pada saat dibuat.
  Jalankan GitHub Actions dan periksa hasil build sebelum mengganti APK yang lama.

LANGKAH:
1. Ekstrak ZIP ini ke root project ~/EnjiwsMusicNew, pilih overwrite.
2. Jalankan:
   git add .
   git commit -m "Add global music discovery with Audius"
   git push origin main
3. Tunggu GitHub Actions selesai, lalu unduh artifact Enjiws-Music-APK.
