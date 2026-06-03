export const DEFAULT_EPG_URL = "https://epg.pw/xmltv/epg_ID.xml";

export function getEpgSourceUrl() {
  return process.env.EPG_URL || DEFAULT_EPG_URL;
}
