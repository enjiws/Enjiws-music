ENJIWS MUSIC V1.8 — NEW UI

- Mini player: bar kecil di atas navigasi bawah (play/pause + next), tap untuk buka Now Playing.
- Navigasi bawah: tab Koleksi dan Online (halaman Online tidak lagi punya tombol kembali).
- Dynamic background: Now Playing memakai cover lagu yang di-blur + scrim gelap agar teks tetap kontras.
- Interactive lyrics: tombol LIRIK di Now Playing. Lirik tersinkron (LRCLIB), baris aktif membesar & terang,
  auto-scroll, tap baris untuk loncat ke bagian itu.
- Cover dipakai di daftar lagu, hasil pencarian online, mini player, dan Now Playing.

File diganti: MainActivity.kt, OnlineMusic.kt. File baru: Artwork.kt, Lyrics.kt.
build.gradle.kts dan AndroidManifest.xml TIDAK diubah (jangan timpa versi NewPipeExtractor milikmu).
Source belum dikompilasi di sini; cek hasil GitHub Actions.
