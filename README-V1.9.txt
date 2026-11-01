ENJIWS MUSIC V1.9 — SOUNDCLOUD

- Sumber baru: SoundCloud (via NewPipeExtractor, tanpa dependency tambahan). Hasilnya tampil paling atas.
- YouTube dan SoundCloud memakai satu fungsi resolve; jika gagal, pesan error memuat diagnosis (audio=, url=, hls=, dash=).
- Hanya OnlineMusic.kt yang berubah. build.gradle.kts / manifest tidak disentuh.
- Track SoundCloud berbayar (Go+) bisa hanya preview. Stream HLS-only belum didukung (butuh modul media3-exoplayer-hls).
- Source belum dikompilasi di sini; cek hasil GitHub Actions.
