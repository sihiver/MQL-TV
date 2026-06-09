# Fix Resolution Selection - Test Report

## Summary

✅ **Parsing logic fixed** - Kode perbaikan berhasil extract 4 resolusi dari manifest live.
❌ **Server not updated** - Backend server tv.mqlspot.my.id belum ter-deploy dengan perbaikan.

## Test Results

### Server: tv.mqlspot.my.id
- Email: sihiver@yahoo.com
- Channel tested: GTV (ID: 9)

### Before Fix (Current API Response)
```
GET /api/channels/9/stream/qualities
Response:
{
  "data": [
    { "id": "auto", "label": "Otomatis", "height": null, "bandwidth": null }
  ]
}
```
**Issue**: Hanya 1 resolusi (AUTO) ditampilkan padahal manifest support 4 resolusi.

### After Fix (Expected API Response - setelah deployment)
```
GET /api/channels/9/stream/qualities
Response:
{
  "data": [
    { "id": "auto", "label": "Otomatis", "height": null, "bandwidth": null },
    { "id": "h720", "label": "720p", "height": 720, "bandwidth": 2499968 },
    { "id": "h480", "label": "480p", "height": 480, "bandwidth": 1000000 },
    { "id": "h360", "label": "360p", "height": 360, "bandwidth": 499968 },
    { "id": "h240", "label": "240p", "height": 240, "bandwidth": 299968 }
  ]
}
```

## Manifest Analysis

### Live Manifest URL
`https://d2tjypxxy769fn.cloudfront.net/out/v1/b8b9b1d5f80f45649b4a3619291551ab/in...`

### Format: DASH (application/dash+xml) ✓

### Representations Found
```
AdaptationSet[0] (video/mp4, 4 representations):
  ✓ Representation[0]: 240p  (426x240)  @ 0.30 Mbps
  ✓ Representation[1]: 360p  (640x360)  @ 0.50 Mbps
  ✓ Representation[2]: 480p  (854x480)  @ 1.00 Mbps
  ✓ Representation[3]: 720p  (1280x720) @ 2.50 Mbps
```

## What Was Fixed

### File: `/backend/src/services/streamQualities.js`

#### 1. Enhanced DASH Parsing (`parseDashQualities`)
- ✅ Extract both `width` dan `height` dari Representation
- ✅ Fallback ke AdaptationSet-level dimensions
- ✅ Calculate height dari width dengan 16:9 aspect ratio
- ✅ Pass bandwidth ke label function

#### 2. Improved Label Generation (`labelFromHeight`)
- ✅ Support bandwidth-based labels ketika height tidak available
- ✅ Better fallback logic

#### 3. Better Deduplication (`dedupeQualitiesByHeight`)
- ✅ Separate logic untuk height-based vs bandwidth-based
- ✅ Keep highest bandwidth per resolution
- ✅ Proper sorting (height DESC, then bandwidth DESC)

#### 4. Debug Logging
- ✅ Log variants count
- ✅ Log detail setiap variant
- ✅ Warn jika tidak ada variant

## Deployment Instructions

### Untuk deploy ke server tv.mqlspot.my.id:

1. **Copy file perbaikan:**
   ```bash
   scp /path/to/backend/src/services/streamQualities.js \
       user@tv.mqlspot.my.id:/path/to/backend/src/services/
   ```

2. **Restart backend server:**
   ```bash
   # SSH ke server
   ssh user@tv.mqlspot.my.id
   
   # Masuk ke backend directory
   cd /path/to/backend
   
   # Restart service (bergantung pada setup Anda)
   npm run dev          # atau
   systemctl restart backend-service
   ```

3. **Verify hasil:**
   ```bash
   # Jalankan test untuk confirm
   node test-qualities-api.js
   ```

### Alternatif: Manual update via git

```bash
# SSH ke server
ssh user@tv.mqlspot.my.id
cd /path/to/repo

# Update code
git pull origin main

# Install dependencies jika diperlukan
npm install --prefix backend

# Restart service
npm run dev --prefix backend
```

## Test Scripts Created

### 1. `test-qualities-api.js`
Test endpoint `/api/channels/{id}/stream/qualities` dan verify multiple resolutions.

**Usage:**
```bash
node test-qualities-api.js
```

**Output example setelah fix:**
```
✅ TEST PASSED - API berhasil return multiple resolutions
   - 720p (height: 720, bandwidth: 2.50 Mbps) 
   - 480p (height: 480, bandwidth: 1.00 Mbps)
   - 360p (height: 360, bandwidth: 0.50 Mbps)
   - 240p (height: 240, bandwidth: 0.30 Mbps)
```

### 2. `debug-manifest.js`
Debug manifest structure untuk analyze DASH/HLS format.

**Usage:**
```bash
node debug-manifest.js
```

### 3. `test-dash-parsing-live.js`
Test parsing logic langsung dengan manifest live (tanpa API call).

**Usage:**
```bash
node test-dash-parsing-live.js
```

## Files Modified

- ✅ `/backend/src/services/streamQualities.js` - Enhanced parsing logic

## Testing Status

| Test | Status | Notes |
|------|--------|-------|
| Parsing logic (local) | ✅ PASS | Menemukan 4 resolusi |
| API endpoint (live) | ⚠️ WARNING | Server belum ter-update |
| Manifest format | ✅ PASS | Valid DASH+XML dengan 4 repr |
| Deduplication | ✅ PASS | No duplicates |
| Label generation | ✅ PASS | Correct labels (720p, 480p, etc) |

## Rollback Instructions

Jika ada issue setelah deployment:

```bash
# Revert file
git checkout HEAD -- backend/src/services/streamQualities.js

# atau restore dari backup
cp backend/src/services/streamQualities.js.backup \
   backend/src/services/streamQualities.js

# Restart service
npm run dev --prefix backend
```

---

**Created:** June 9, 2026
**Status:** ✅ Fix verified, awaiting deployment

