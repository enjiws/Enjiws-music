ENJIWS MUSIC V1.2 — SMART MUSIC LIBRARY

Fitur:
- Memindai lagu dari penyimpanan HP melalui MediaStore.
- Daftar lagu, artis, dan durasi.
- Pencarian judul atau artis.
- Ketuk lagu untuk memutar playlist.
- Kontrol play/pause dan next.
- Tetap menggunakan MusicPlaybackService V1.1 untuk playback latar belakang.

CARA UPDATE:
1. Ekstrak ZIP ini ke folder project ~/EnjiwsMusicNew dan izinkan overwrite.
2. Jangan hapus MusicPlaybackService.kt atau .github/workflows/build.yml.
3. Jalankan:
   cd ~/EnjiwsMusicNew
   git add .
   git commit -m "Add smart music library"
   git push origin main
4. Buka GitHub > Actions dan tunggu build berhasil.
5. Download artifact Enjiws-Music-APK, ekstrak APK, lalu install.

Saat pertama dibuka, izinkan akses musik/audio. Jika daftar kosong, tekan Pindai.
