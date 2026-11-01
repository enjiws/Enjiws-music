ENJIWS MUSIC V1.7 - YOUTUBE DISCOVERY

What's new:
- Adds YouTube Data API v3 search to the unified discovery list.
- YouTube results open in the official YouTube app/site; they are not extracted as audio.
- Jamendo and Audius remain the in-app audio streaming providers when a playable stream is available.
- Apple/iTunes remains a discovery catalogue.
- Existing direct audio URL playback, LRCLIB lyrics, local library and background playback are retained.

Setup:
1. Enable YouTube Data API v3 in Google Cloud Console.
2. Create an API key and restrict it to YouTube Data API v3.
3. Do not commit the key to GitHub. Paste it into the YouTube Data API key field inside the app before searching.
4. If the key previously appeared in a public/shared chat, rotate it and use a newly restricted key.

Notes:
- YouTube Data API search consumes quota.
- The app uses official YouTube watch URLs and does not extract audio or bypass ads/DRM.
- Build has not been executed in this environment; verify with GitHub Actions.
