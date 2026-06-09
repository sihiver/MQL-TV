# ✅ Deployment Checklist

## Pre-Deployment Verification

### 1. Code Quality
- [x] File modified: `/backend/src/services/streamQualities.js`
- [x] Syntax checked: `node -c`✓
- [x] Logic tested with live manifest ✓
  - Expected: 4 resolutions → Actual: 4 resolutions ✓
- [x] No breaking changes to API contract ✓

### 2. Documentation
- [x] Technical notes: `DASH_FIX_NOTES.md`
- [x] Test report: `TEST_REPORT.md`
- [x] Summary: `RINGKASAN_FIX.md`

### 3. Backup
- [ ] Backup current `/backend/src/services/streamQualities.js`
  ```bash
  cp backend/src/services/streamQualities.js \
     backend/src/services/streamQualities.js.backup
  ```

---

## Deployment Steps

### Step 1: Verify Current State
```bash
# Check server reachability
[ ] curl -s https://tv.mqlspot.my.id/health | grep -q "ok"

# Check current quality endpoint (should show only "Otomatis")
[ ] curl -s https://tv.mqlspot.my.id/api/channels/9/stream/qualities \
      -H "Authorization: Bearer $TOKEN" | jq '.data | length'
    #→ Expected: 1
```

**Success Criteria**: Server accessible, API returns 1 quality option

---

### Step 2: Copy Fixed File
```bash
# Option A: SCP (direct file copy)
[ ] scp backend/src/services/streamQualities.js \
       user@tv.mqlspot.my.id:/path/to/backend/src/services/
    
# Option B: Git Pull (if repo synced)
[ ] ssh user@tv.mqlspot.my.id
    cd /path/to/repo
    git pull origin main
    # (only if changes are already pushed to repo)
```

**Success Criteria**: File transferred without errors

---

### Step 3: Dependency Check
```bash
# Verify dependencies are installed (should be already)
[ ] npm list xml2js axios  (in backend dir)
    #→ Both should be present
```

**Success Criteria**: Dependencies already installed in package.json

---

### Step 4: Restart Backend Service
```bash
[ ] ssh user@tv.mqlspot.my.id
    cd /path/to/backend
    
    # Option A: nodejs service
    npm run dev  # or similar start script
    
    # Option B: Systemd service
    sudo systemctl restart backend-service
    
    # Option C: PM2
    pm2 restart backend-service
```

**Wait**: 5-10 seconds for service to fully start

**Success Criteria**: Service started without errors

---

### Step 5: Verify Deployment
```bash
# Check API health
[ ] curl -s https://tv.mqlspot.my.id/health | jq '.database'
    #→ Should show "connected"

# Check new quality endpoint (should show 5 options now)
[ ] curl -s https://tv.mqlspot.my.id/api/channels/9/stream/qualities \
      -H "Authorization: Bearer $TOKEN" | jq '.'
    #→ Expected response:
    #  {
    #    "data": [
    #      { "id": "auto", "label": "Otomatis", ... },
    #      { "id": "h720", "label": "720p", ... },
    #      { "id": "h480", "label": "480p", ... },
    #      { "id": "h360", "label": "360p", ... },
    #      { "id": "h240", "label": "240p", ... }
    #    ],
    #    "total": 5
    #  }

# Parse the response
[ ] Count data.length
    #→ Expected: 5 (auto + 4 resolutions)
    #→ If 1: Deployment FAILED - Rollback needed
    #→ If 5: Deployment SUCCESS! ✓
```

**Success Criteria**: API returns 5 quality options (1 AUTO + 4 resolutions)

---

### Step 6: Test on Android TV App (if available)
```bash
[ ] Open app on Android TV
    [ ] Navigate to GTV channel
    [ ] Tap quality button (⚙ icon)
    [ ] Verify quality picker shows:
        - Otomatis
        - 720p
        - 480p
        - 360p
        - 240p
    [ ] Try switching between resolutions
    [ ] Verify stream continues without issues
```

**Success Criteria**: UI shows multiple resolutions, no streaming errors

---

## Post-Deployment

### 1. Monitor Logs
```bash
[ ] ssh user@tv.mqlspot.my.id
    tail -f /path/to/backend/logs/app.log | grep streamQualities
    #→ Should see:
    #  [streamQualities] Ditemukan 4 variant(s)...
    #  - 720p (height: 720, bandwidth: 2499968)
    #  - 480p (height: 480, bandwidth: 1000000)
    #  - 360p (height: 360, bandwidth: 499968)
    #  - 240p (height: 240, bandwidth: 299968)
```

**Success Criteria**: Debug logs show 4 variants detected

### 2. Test Other Channels
```bash
[ ] Test at least 2-3 other channels to ensure no regression
    curl -s https://tv.mqlspot.my.id/api/channels/{id}/stream/qualities \
          -H "Authorization: Bearer $TOKEN" | jq '.data | length'
    #→ Each should return > 1 quality options (if manifest supports)
```

**Success Criteria**: Other channels work without issues

---

## Rollback Plan (If Issues Occur)

### Immediate Rollback
```bash
[ ] ssh user@tv.mqlspot.my.id
    cd /path/to/backend

# Option A: Restore from backup
[ ] cp backend/src/services/streamQualities.js.backup \
       backend/src/services/streamQualities.js
   
# Option B: Git revert
[ ] git checkout HEAD~1 -- backend/src/services/streamQualities.js

[ ] npm run dev  # Restart service
```

**Verify Rollback**: API should return only "Otomatis" again

---

## Completion Checklist

### Go-Live Validation
- [ ] Syntax check passed
- [ ] Logic tested with live manifest
- [ ] File successfully copied to server
- [ ] Service restarted without errors
- [ ] API endpoint returns 5 quality options
- [ ] Android TV app shows multiple resolutions
- [ ] No regression in other channels
- [ ] Debug logs show correct parsing
- [ ] Logs monitored for 24 hours without errors

### Sign-Off
- [ ] Deployment Date: _______________
- [ ] Deployed By: _______________
- [ ] Verified By: _______________
- [ ] Status: ☐ Successful ☐ Rolled Back

---

## Emergency Contact Info

If issues occur during deployment:
- Check: `/path/to/backend/logs/app.log`
- Rollback: Restore from `.backup` file
- Escalate: Contact backend DevOps team

---

## Notes Section

```
Deployment Notes:
_________________________________________________________________

_________________________________________________________________

_________________________________________________________________
```

---

**Last Updated**: June 9, 2026
**Fix Version**: 1.0
**Ready for Deployment**: ✅ YES

