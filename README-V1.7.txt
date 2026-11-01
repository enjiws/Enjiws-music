ENJIWS MUSIC V1.7 — YOUTUBE SOURCE

Baru:
- Pencarian YouTube Music (kategori lagu) lewat NewPipeExtractor, hasil tampil paling atas.
- Klik hasil YouTube -> audio diambil lalu diputar di Media3 (background play tetap jalan).
- Sumber lain (Apple, Jamendo, Audius, URL, LRCLIB) tidak diubah.

File berubah/baru:
- app/build.gradle.kts (dependency NewPipeExtractor, OkHttp, desugaring, versi 1.7)
- AndroidManifest.xml (android:name=".EnjiwsApp")
- OnlineMusic.kt (searchYouTube, resolveYoutubeAudio)
- BARU: EnjiwsApp.kt, AppDownloader.kt

WAJIB MANUAL (file ini tidak ada di ZIP):
Buka settings.gradle.kts, di dalam dependencyResolutionManagement { repositories { ... } } tambahkan:
    maven { url = uri("https://jitpack.io") }

CATATAN:
- Cek versi terbaru NewPipeExtractor di github.com/TeamNewPipe/NewPipeExtractor/releases,
  ganti v0.24.6 di build.gradle.kts jika ada yang lebih baru. Jika YouTube berubah
  dan pencarian/putar gagal, update versi ini dulu.
- Memakai extractor tidak resmi dan melanggar ToS YouTube. Untuk pemakaian pribadi.
- Source belum dikompilasi di sini. Jalankan GitHub Actions dan cek hasilnya.
