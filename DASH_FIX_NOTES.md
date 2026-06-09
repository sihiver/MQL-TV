# Fix: Resolution Selection di Overlay (DASH Multi-Resolution Support)

## Masalah
Pilihan resolusi di overlay hanya menampilkan "Otomatis" padahal channel .mpd file supports multi resolusi.

## Root Cause
Fungsi `parseDashQualities` di backend tidak dapat mengekstrak informasi height/resolusi dari DASH manifest dengan baik karena:

1. Hanya mencari attribute `height` pada Representation, padahal beberapa manifest hanya punya `width`
2. Tidak fallback ke attribute yang ada di level AdaptationSet
3. Tidak menghitung height dari width jika hanya width yang tersedia
4. Label resolusi hanya berdasarkan height, tidak support bandwidth-only variants

## Solusi

### 1. Enhanced DASH Manifest Parsing (`parseDashQualities`)
- ✓ Extract both `width` dan `height` attributes dari Representation
- ✓ Fallback ke AdaptationSet-level dimensions jika tidak ada di Representation
- ✓ Calculate height dari width dengan asumsi 16:9 aspect ratio jika hanya width tersedia
- ✓ Pass bandwidth ke label function untuk fallback label

### 2. Improved Label Generation (`labelFromHeight`)
- ✓ Support bandwidth-based labels (HD, SD, Kualitas) untuk variants tanpa height info
- ✓ Fallback intelligently ketika height tidak tersedia

### 3. Better Deduplication Logic (`dedupeQualitiesByHeight`)
- ✓ Separate deduplication untuk height-based vs bandwidth-based variants
- ✓ Keep highest bandwidth untuk setiap unique resolution
- ✓ Sort by height DESC, then by bandwidth DESC

### 4. Debug Logging
- ✓ Log jumlah variants yang ditemukan
- ✓ Log detail setiap variant (label, height, bandwidth)
- ✓ Warn jika tidak ada variant ditemukan

## File yang Diubah
- `/backend/src/services/streamQualities.js`

## Changes Detail

### `labelFromHeight(height, bandwidth)` 
Sekarang support parameter bandwidth untuk fallback label:
- `height >= 1080`: "1080p"
- `height >= 720`: "720p"  
- `height >= 480`: "480p"
- `height >= 360`: "360p"
- `bandwidth >= 5000000`: "HD"
- `bandwidth >= 2000000`: "SD"
- Fallback: "Kualitas"

### `parseDashQualities(xml, manifestUrl)`
Enhanced untuk handle berbagai manifest structure:
```javascript
// Get dimensions from AdaptationSet if not specified per Representation
const setWidth = set.width ? parseInt(set.width, 10) : null;
const setHeight = set.height ? parseInt(set.height, 10) : null;

// Extract height, with fallbacks
let height = rep.height ? parseInt(rep.height, 10) : null;
let width = rep.width ? parseInt(rep.width, 10) : null;

// Fallback ke AdaptationSet-level dimensions
if (!height) height = setHeight;
if (!width) width = setWidth;

// Calculate height dari width (16:9 aspect ratio)
if (!height && width) {
  height = Math.round(width * 9 / 16);
}
```

### `dedupeQualitiesByHeight(qualities)`
Improved deduplication logic:
- Separate maps untuk height-based vs bandwidth-based variants
- Keep highest bandwidth untuk setiap unique resolution
- Better sorting: by height DESC, then by bandwidth DESC

## Testing

Syntax sudah di-validasi dengan Node.js:
```bash
node -c src/services/streamQualities.js
```

## Expected Behavior After Fix

### Saat user buka quality picker di player:
1. ✓ Multiple resolution options akan ditampilkan (bukan hanya "Otomatis")
2. ✓ Label resolusi akan akurat (1080p, 720p, 480p, etc)
3. ✓ Bahkan untuk DASH manifest dengan attribute berbeda akan ter-parse dengan benar
4. ✓ Bandwidth-based variants akan memiliki label yang bermakna (HD, SD)
5. ✓ Server logs akan menunjukkan variants yang ditemukan untuk debugging

### Contoh Log Output:
```
[streamQualities] Ditemukan 3 variant(s) untuk https://example.com/stream.mpd
  - 1080p (height: 1080, bandwidth: 5000000)
  - 720p (height: 720, bandwidth: 2500000)
  - 480p (height: 480, bandwidth: 1000000)
```

## Rollback
Jika ada issue, langsung revert file `backend/src/services/streamQualities.js` ke version sebelumnya.

